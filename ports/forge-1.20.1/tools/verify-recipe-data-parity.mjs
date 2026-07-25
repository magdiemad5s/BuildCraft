#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const laneRoot = path.resolve(scriptDir, "..");
const repoRoot = path.resolve(laneRoot, "..", "..");
const workspaceRoot = path.resolve(repoRoot, "..");

function option(name, fallback) {
  const index = process.argv.indexOf(name);
  if (index < 0) return fallback;
  if (index + 1 >= process.argv.length) throw new Error(`Missing value for ${name}`);
  return path.resolve(process.argv[index + 1]);
}

const dataRoot = option("--data-root", path.join(laneRoot, "src", "main", "resources", "data"));
const legacyAssetsRoot = option("--legacy-assets-root", path.join(repoRoot, "buildcraft_resources", "assets"));
const identityManifestPath = option(
  "--identity-manifest",
  path.join(repoRoot, "legacy-1.12.2-contract", "LEGACY_IDENTITY_MANIFEST.json")
);
const identityReportPath = option(
  "--identity-report",
  path.join(laneRoot, "LEGACY_IDENTITY_PARITY_REPORT.json")
);
const referenceJarPath = option("--reference-jar", path.join(workspaceRoot, "buildcraft-all-8.0.0.jar"));

const errors = [];
const notes = [];

function error(message) {
  errors.push(message);
}

function note(message) {
  notes.push(message);
}

function normalize(relative) {
  return relative.split(path.sep).join("/");
}

function walk(root) {
  if (!fs.existsSync(root)) {
    error(`Missing directory: ${root}`);
    return [];
  }
  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const absolute = path.join(root, entry.name);
    if (entry.isDirectory()) files.push(...walk(absolute));
    else if (entry.isFile()) files.push(absolute);
  }
  return files;
}

function readJson(file, label = file) {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch (cause) {
    error(`Invalid JSON ${label}: ${cause.message}`);
    return null;
  }
}

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex").toUpperCase();
}

/** Lists a normal (non-ZIP64) ZIP/JAR central directory without external dependencies. */
function listZipEntries(file) {
  const bytes = fs.readFileSync(file);
  const minimumEocd = 22;
  const maximumComment = 0xffff;
  let eocd = -1;
  for (let offset = bytes.length - minimumEocd;
       offset >= Math.max(0, bytes.length - minimumEocd - maximumComment);
       offset--) {
    if (bytes.readUInt32LE(offset) === 0x06054b50) {
      eocd = offset;
      break;
    }
  }
  if (eocd < 0) throw new Error(`ZIP end-of-central-directory not found: ${file}`);

  const entryCount = bytes.readUInt16LE(eocd + 10);
  let offset = bytes.readUInt32LE(eocd + 16);
  const names = [];
  for (let index = 0; index < entryCount; index++) {
    if (bytes.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error(`Invalid ZIP central-directory entry ${index} at ${offset}: ${file}`);
    }
    const nameLength = bytes.readUInt16LE(offset + 28);
    const extraLength = bytes.readUInt16LE(offset + 30);
    const commentLength = bytes.readUInt16LE(offset + 32);
    names.push(bytes.subarray(offset + 46, offset + 46 + nameLength).toString("utf8"));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return names;
}

function recipeIdsFromLegacyPaths(paths) {
  const ids = new Set();
  for (const relative of paths) {
    const match = relative.match(/^([^/]+)\/recipes\/(.+)\.json$/);
    if (!match || path.posix.basename(match[2]).startsWith("_")) continue;
    ids.add(`${match[1]}:${match[2]}`);
  }
  return ids;
}

function difference(left, right) {
  return [...left].filter(value => !right.has(value)).sort();
}

function splitReportList(value) {
  if (Array.isArray(value)) return value;
  return String(value ?? "").trim().split(/\s+/).filter(Boolean);
}

function flattenByNamespace(value) {
  return Object.values(value ?? {}).flatMap(ids => Array.isArray(ids) ? ids : [ids]);
}

function isBuildCraftId(value) {
  return typeof value === "string" && /^buildcraft[a-z]*:/.test(value);
}

function validResourceLocation(value) {
  return /^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(value);
}

function sameSet(left, right) {
  return left.size === right.size && [...left].every(value => right.has(value));
}

if (!fs.existsSync(referenceJarPath)) {
  error(`Reference JAR missing: ${referenceJarPath}`);
}
if (!fs.existsSync(identityManifestPath)) {
  error(`Identity manifest missing: ${identityManifestPath}`);
}
if (!fs.existsSync(identityReportPath)) {
  error(`Identity parity report missing: ${identityReportPath}`);
}

const identityManifest = fs.existsSync(identityManifestPath)
  ? readJson(identityManifestPath, normalize(path.relative(laneRoot, identityManifestPath)))
  : null;
const identityReport = fs.existsSync(identityReportPath)
  ? readJson(identityReportPath, normalize(path.relative(laneRoot, identityReportPath)))
  : null;

let jarRecipeIds = new Set();
if (fs.existsSync(referenceJarPath)) {
  try {
    const expectedHash = identityManifest?.legacySource?.referenceJar?.sha256;
    const actualHash = sha256(referenceJarPath);
    if (expectedHash && actualHash !== expectedHash.toUpperCase()) {
      error(`Reference JAR SHA-256 mismatch: expected ${expectedHash}, got ${actualHash}`);
    }
    const entries = listZipEntries(referenceJarPath);
    jarRecipeIds = recipeIdsFromLegacyPaths(
      entries
        .filter(entry => entry.startsWith("assets/"))
        .map(entry => entry.slice("assets/".length))
    );
  } catch (cause) {
    error(`Unable to inspect reference JAR: ${cause.message}`);
  }
}

const legacySourceRecipeIds = recipeIdsFromLegacyPaths(
  walk(legacyAssetsRoot)
    .filter(file => file.endsWith(".json"))
    .map(file => normalize(path.relative(legacyAssetsRoot, file)))
);
if (jarRecipeIds.size > 0 && !sameSet(jarRecipeIds, legacySourceRecipeIds)) {
  for (const id of difference(jarRecipeIds, legacySourceRecipeIds)) {
    error(`Released JAR recipe missing from legacy source inventory: ${id}`);
  }
  for (const id of difference(legacySourceRecipeIds, jarRecipeIds)) {
    error(`Legacy source recipe missing from released JAR inventory: ${id}`);
  }
}

const jsonFiles = walk(dataRoot).filter(file => file.endsWith(".json")).sort();
const parsed = new Map();
const recipes = new Map();
const advancements = new Map();
const lootTables = new Map();
for (const file of jsonFiles) {
  const relative = normalize(path.relative(dataRoot, file));
  const value = readJson(file, relative);
  if (value === null) continue;
  parsed.set(relative, value);

  let match = relative.match(/^([^/]+)\/recipes\/(.+)\.json$/);
  if (match) recipes.set(`${match[1]}:${match[2]}`, { relative, value });
  match = relative.match(/^([^/]+)\/advancements\/(.+)\.json$/);
  if (match) advancements.set(`${match[1]}:${match[2]}`, { relative, value });
  match = relative.match(/^([^/]+)\/loot_tables\/(.+)\.json$/);
  if (match) lootTables.set(`${match[1]}:${match[2]}`, { relative, value });
}

const intentionallyUnavailableReleasedRecipes = new Set(["buildcraftcore:diamond_shard"]);
for (const id of jarRecipeIds) {
  if (intentionallyUnavailableReleasedRecipes.has(id)) {
    if (recipes.has(id)) error(`Unavailable legacy recipe must remain disabled until its item exists: ${id}`);
    continue;
  }
  if (!recipes.has(id)) error(`Missing released recipe ID: ${id}`);
}
for (const id of intentionallyUnavailableReleasedRecipes) {
  if (jarRecipeIds.has(id)) note(`Intentionally unavailable legacy recipe: ${id} (item was never registered in the normal 8.0.0 release)`);
}

const supersededPortRecipeIds = [
  "buildcraftbuilders:architect",
  "buildcraftcore:engine_redstone",
  "buildcraftcore:gears/gear_wood",
  "buildcraftcore:gears/gear_stone",
  "buildcraftcore:gears/gear_iron",
  "buildcraftcore:gears/gear_gold",
  "buildcraftcore:gears/gear_diamond",
  "buildcraftfactory:autowork_bench_1",
  "buildcraftfactory:autowork_bench_2",
  "buildcrafttransport:residue_to_waterproof",
  "buildcrafttransport:structure",
  "buildcrafttransport:waterproof"
];
for (const id of supersededPortRecipeIds) {
  if (recipes.has(id)) error(`Superseded port recipe ID still present: ${id}`);
}

const canonicalResults = new Map([
  ["buildcraftbuilders:architect_table", ["buildcraftbuilders:architect", 1, null]],
  ["buildcraftcore:gear_wood", ["buildcraftcore:gear_wood", 1, null]],
  ["buildcraftcore:gear_stone", ["buildcraftcore:gear_stone", 1, null]],
  ["buildcraftcore:gear_iron", ["buildcraftcore:gear_iron", 1, null]],
  ["buildcraftcore:gear_gold", ["buildcraftcore:gear_gold", 1, null]],
  ["buildcraftcore:gear_diamond", ["buildcraftcore:gear_diamond", 1, null]],
  ["buildcraftcore:redstone_engine", ["buildcraftcore:engine", 1, "{Damage:0}"]],
  ["buildcraftenergy:stirling_engine", ["buildcraftcore:engine", 1, "{Damage:1}"]],
  ["buildcraftenergy:combustion_engine", ["buildcraftcore:engine", 1, "{Damage:2}"]],
  ["buildcraftenergy:rf_engine", ["buildcraftcore:engine", 1, "{Damage:4}"]],
  ["buildcraftfactory:autoworkbench_item", ["buildcraftfactory:autoworkbench_item", 1, null]],
  ["buildcrafttransport:pipe_sealant", ["buildcrafttransport:waterproof", 1, null]],
  ["buildcrafttransport:pipe_structure", ["buildcrafttransport:pipe_structure", 8, null]]
]);
for (const [id, [item, count, nbt]] of canonicalResults) {
  const recipe = recipes.get(id)?.value;
  if (!recipe) continue;
  if (recipe.result?.item !== item) error(`${id} result item must be ${item}, got ${recipe.result?.item}`);
  if ((recipe.result?.count ?? 1) !== count) error(`${id} result count must be ${count}, got ${recipe.result?.count ?? 1}`);
  if ((recipe.result?.nbt ?? null) !== nbt) error(`${id} result NBT must be ${nbt ?? "absent"}, got ${recipe.result?.nbt ?? "absent"}`);
}
const autoworkbenchPattern = recipes.get("buildcraftfactory:autoworkbench_item")?.value?.pattern;
if (autoworkbenchPattern && JSON.stringify(autoworkbenchPattern) !== JSON.stringify(["scs"])) {
  error(`buildcraftfactory:autoworkbench_item must retain the released horizontal gear-table-gear pattern`);
}

const actualItems = new Set(flattenByNamespace(identityManifest?.registryContract?.itemsByNamespace));
const actualBlocks = new Set(flattenByNamespace(identityManifest?.registryContract?.blocksByNamespace));
if (identityReport && identityReport.gate?.complete !== true) {
  error(`Identity parity report gate is not complete: ${identityReportPath}`);
}
for (const id of splitReportList(identityReport?.registryParity?.item?.missing)) actualItems.delete(id);
for (const id of splitReportList(identityReport?.registryParity?.item?.extra)) actualItems.add(id);
for (const id of splitReportList(identityReport?.registryParity?.block?.missing)) actualBlocks.delete(id);
for (const id of splitReportList(identityReport?.registryParity?.block?.extra)) actualBlocks.add(id);

const fluidFamilies = [
  "oil", "oil_residue", "oil_heavy", "oil_dense", "oil_distilled",
  "fuel_dense", "fuel_mixed_heavy", "fuel_light", "fuel_mixed_light", "fuel_gaseous"
];
const heatNames = ["cool", "hot", "searing"];
for (const family of fluidFamilies) {
  for (let heat = 0; heat < heatNames.length; heat++) {
    actualItems.add(`buildcraftenergy:${family}/${heatNames[heat]}_bucket`);
    actualBlocks.add(`buildcraftenergy:${family}${heat === 0 ? "" : `_heat_${heat}`}`);
  }
}

function validateItem(id, relative, where) {
  if (!isBuildCraftId(id)) return;
  if (!validResourceLocation(id)) error(`Invalid item ResourceLocation ${id} at ${relative} ${where}`);
  else if (!actualItems.has(id)) error(`Unknown BuildCraft item ${id} at ${relative} ${where}`);
}

function validateBlock(id, relative, where) {
  if (!isBuildCraftId(id)) return;
  if (!validResourceLocation(id)) error(`Invalid block ResourceLocation ${id} at ${relative} ${where}`);
  else if (!actualBlocks.has(id)) error(`Unknown BuildCraft block ${id} at ${relative} ${where}`);
}

function scanRegistryReferences(value, relative, where = "$") {
  if (Array.isArray(value)) {
    value.forEach((entry, index) => scanRegistryReferences(entry, relative, `${where}[${index}]`));
    return;
  }
  if (value === null || typeof value !== "object") return;
  for (const [key, child] of Object.entries(value)) {
    const childWhere = `${where}.${key}`;
    if (key === "item" && typeof child === "string") validateItem(child, relative, childWhere);
    if (key === "items" && Array.isArray(child)) {
      child.forEach((id, index) => validateItem(id, relative, `${childWhere}[${index}]`));
    }
    if (key === "name" && value.type === "minecraft:item" && typeof child === "string") {
      validateItem(child, relative, childWhere);
    }
    if (key === "block" && typeof child === "string") validateBlock(child, relative, childWhere);
    scanRegistryReferences(child, relative, childWhere);
  }
}
for (const entry of [...recipes.values(), ...advancements.values(), ...lootTables.values()]) {
  scanRegistryReferences(entry.value, entry.relative);
}

for (const [id, { relative, value }] of advancements) {
  if (isBuildCraftId(value.parent) && !advancements.has(value.parent)) {
    error(`Missing BuildCraft advancement parent ${value.parent} at ${relative}`);
  }
  for (const recipe of value.rewards?.recipes ?? []) {
    if (isBuildCraftId(recipe) && !recipes.has(recipe)) {
      error(`Missing advancement recipe reward ${recipe} at ${relative}`);
    }
  }
  for (const loot of value.rewards?.loot ?? []) {
    if (isBuildCraftId(loot) && !lootTables.has(loot)) {
      error(`Missing advancement loot reward ${loot} at ${relative}`);
    }
  }

  const criteria = new Set(Object.keys(value.criteria ?? {}));
  const usedCriteria = new Set();
  if (value.requirements !== undefined) {
    if (!Array.isArray(value.requirements) || value.requirements.length === 0) {
      error(`Advancement requirements must be a non-empty array at ${relative}`);
    } else {
      for (let row = 0; row < value.requirements.length; row++) {
        const requirement = value.requirements[row];
        if (!Array.isArray(requirement) || requirement.length === 0) {
          error(`Advancement requirement row ${row} is empty at ${relative}`);
          continue;
        }
        for (const criterion of requirement) {
          usedCriteria.add(criterion);
          if (!criteria.has(criterion)) error(`Unknown requirement criterion ${criterion} at ${relative}`);
        }
      }
      for (const criterion of criteria) {
        if (!usedCriteria.has(criterion)) error(`Unused advancement criterion ${criterion} at ${relative}`);
      }
    }
  }

  for (const [criterionName, criterion] of Object.entries(value.criteria ?? {})) {
    if (criterion?.trigger !== "minecraft:recipe_unlocked") continue;
    const recipe = criterion.conditions?.recipe;
    if (isBuildCraftId(recipe) && !recipes.has(recipe)) {
      error(`Missing recipe_unlocked target ${recipe} in ${criterionName} at ${relative}`);
    }
  }

  if (!validResourceLocation(id)) error(`Invalid advancement ID derived from path: ${id}`);
}

for (const id of recipes.keys()) {
  if (!validResourceLocation(id)) error(`Invalid recipe ID derived from path: ${id}`);
}
for (const id of lootTables.keys()) {
  if (!validResourceLocation(id)) error(`Invalid loot table ID derived from path: ${id}`);
}

const engineAdvancement = advancements.get("buildcraftenergy:engine")?.value;
if (engineAdvancement) {
  const expected = { redstone_engine: 0, stirling_engine: 1, combustion_engine: 2 };
  for (const [criterion, damage] of Object.entries(expected)) {
    const predicate = engineAdvancement.criteria?.[criterion]?.conditions?.items?.[0];
    if (JSON.stringify(predicate?.items) !== JSON.stringify(["buildcraftcore:engine"]) ||
        predicate?.nbt !== `{Damage:${damage}}`) {
      error(`buildcraftenergy:engine criterion ${criterion} must match buildcraftcore:engine Damage ${damage}`);
    }
  }
}

if (errors.length > 0) {
  console.error(`Recipe/data parity FAILED with ${errors.length} error(s):`);
  for (const message of errors) console.error(`- ${message}`);
  process.exitCode = 1;
} else {
  console.log("Recipe/data parity PASSED");
  console.log(`- released JAR recipe IDs: ${jarRecipeIds.size}`);
  console.log(`- required released recipe IDs present: ${jarRecipeIds.size - intentionallyUnavailableReleasedRecipes.size}`);
  console.log(`- current recipes: ${recipes.size}`);
  console.log(`- advancements: ${advancements.size}`);
  console.log(`- loot tables: ${lootTables.size}`);
  console.log(`- parsed data JSON files: ${parsed.size}`);
  console.log(`- validated BuildCraft item IDs: ${actualItems.size}`);
  console.log(`- validated BuildCraft block IDs: ${actualBlocks.size}`);
  for (const message of notes) console.log(`- ${message}`);
}