#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const laneRoot = path.resolve(scriptDir, "..");
const sourceRoot = path.join(laneRoot, "src", "main", "java");
const resourceRoot = path.join(laneRoot, "src", "main", "resources");
const reportPath = path.join(laneRoot, "GUI_PARITY_REPORT.json");
const textureManifestPath = path.join(laneRoot, "TEXTURE_PARITY_MANIFEST.json");

function walk(root, predicate) {
  const found = [];
  if (!fs.existsSync(root)) return found;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const current = path.join(root, entry.name);
    if (entry.isDirectory()) found.push(...walk(current, predicate));
    else if (predicate(current)) found.push(current);
  }
  return found;
}

function relative(file) {
  return path.relative(laneRoot, file).split(path.sep).join("/");
}

function addAllMatches(text, regex, callback) {
  for (const match of text.matchAll(regex)) callback(match);
}

const javaFiles = walk(sourceRoot, (file) => file.endsWith(".java"));
const declarations = new Map();
const screenRegistrations = new Map();
const duplicateDeclarations = [];
const duplicateScreens = [];
const guiResourceReferences = new Map();

for (const file of javaFiles) {
  const text = fs.readFileSync(file, "utf8");
  const classMatch = text.match(/\b(?:public\s+)?(?:final\s+)?(?:abstract\s+)?class\s+([A-Za-z0-9_]+)/);
  const className = classMatch?.[1];

  if (className) {
    addAllMatches(
      text,
      /RegistryObject<MenuType<[^;=]+>>\s+([A-Z][A-Z0-9_]*)\s*=/g,
      (match) => {
        const key = `${className}.${match[1]}`;
        if (declarations.has(key)) duplicateDeclarations.push(key);
        declarations.set(key, relative(file));
      }
    );
  }

  addAllMatches(
    text,
    /MenuScreens\.register\(\s*([A-Za-z0-9_$.]+)\.get\(\)\s*,\s*([A-Za-z0-9_$.]+)::[A-Za-z0-9_]+\s*\)/g,
    (match) => {
      const expression = match[1];
      const key = expression.includes(".") || !className ? expression : `${className}.${expression}`;
      if (screenRegistrations.has(key)) duplicateScreens.push(key);
      screenRegistrations.set(key, { screen: match[2], source: relative(file) });
    }
  );

  addAllMatches(
    text,
    /new\s+ResourceLocation\(\s*"([a-z0-9_.-]+)"\s*,\s*"([^"]+)"\s*\)/g,
    (match) => recordGuiReference(match[1], match[2], file)
  );
  addAllMatches(
    text,
    /new\s+ResourceLocation\(\s*"([a-z0-9_.-]+):([^"]+)"\s*\)/g,
    (match) => recordGuiReference(match[1], match[2], file)
  );
}

function recordGuiReference(namespace, resourcePath, sourceFile) {
  if (!namespace.startsWith("buildcraft")) return;
  if (!resourcePath.includes("gui/")) return;
  const key = `${namespace}:${resourcePath}`;
  const expectedCandidates = [`src/main/resources/assets/${namespace}/${resourcePath}`];
  if (!path.extname(resourcePath)) {
    expectedCandidates.push(`src/main/resources/assets/${namespace}/textures/${resourcePath}.png`);
  }
  guiResourceReferences.set(key, {
    id: key,
    source: relative(sourceFile),
    expectedCandidates,
  });
}

const declaredKeys = [...declarations.keys()].sort();
const screenKeys = [...screenRegistrations.keys()].sort();
const missingScreens = declaredKeys.filter((key) => !screenRegistrations.has(key));
const orphanScreens = screenKeys.filter((key) => !declarations.has(key));

const missingGuiResources = [...guiResourceReferences.values()]
  .filter((reference) => !reference.expectedCandidates.some(
    (candidate) => fs.existsSync(path.join(laneRoot, candidate))
  ))
  .sort((a, b) => a.id.localeCompare(b.id));

const guiJsonFiles = walk(
  path.join(resourceRoot, "assets"),
  (file) => file.endsWith(".json") && relative(file).includes("/gui/")
);
const invalidGuiJson = [];
for (const file of guiJsonFiles) {
  try {
    JSON.parse(fs.readFileSync(file, "utf8").replace(/^\uFEFF/, ""));
  } catch (error) {
    invalidGuiJson.push({ file: relative(file), error: String(error.message ?? error) });
  }
}

const guiPngFiles = walk(
  path.join(resourceRoot, "assets"),
  (file) => file.endsWith(".png") && relative(file).includes("/textures/gui/")
);
let requiredGuiPng = 0;
if (fs.existsSync(textureManifestPath)) {
  const manifest = JSON.parse(fs.readFileSync(textureManifestPath, "utf8").replace(/^\uFEFF/, ""));
  requiredGuiPng = Object.values(manifest.expectedPackagedMinimums?.guiPngByNamespace ?? {})
    .reduce((sum, count) => sum + Number(count), 0);
}

const errors = [];
for (const key of duplicateDeclarations) errors.push(`Duplicate menu declaration: ${key}`);
for (const key of duplicateScreens) errors.push(`Duplicate client screen registration: ${key}`);
for (const key of missingScreens) errors.push(`Menu has no client screen: ${key}`);
for (const key of orphanScreens) errors.push(`Client screen has no menu declaration: ${key}`);
for (const reference of missingGuiResources) errors.push(`Missing GUI resource: ${reference.id}`);
for (const invalid of invalidGuiJson) errors.push(`Invalid GUI JSON: ${invalid.file}: ${invalid.error}`);
if (guiPngFiles.length < requiredGuiPng) {
  errors.push(`GUI PNG count ${guiPngFiles.length} is below required legacy minimum ${requiredGuiPng}`);
}

const report = {
  schema: "buildcraft-neo/gui-parity-report/v1",
  generatedAtUtc: new Date().toISOString(),
  result: errors.length === 0 ? "PASS" : "FAIL",
  counts: {
    javaFiles: javaFiles.length,
    menuDeclarations: declarations.size,
    clientScreenRegistrations: screenRegistrations.size,
    literalBuildCraftGuiResources: guiResourceReferences.size,
    guiJson: guiJsonFiles.length,
    guiPng: guiPngFiles.length,
    requiredGuiPng,
    errors: errors.length,
  },
  menus: declaredKeys.map((key) => ({
    menu: key,
    declaration: declarations.get(key),
    screen: screenRegistrations.get(key)?.screen ?? null,
    screenRegistration: screenRegistrations.get(key)?.source ?? null,
  })),
  guiResources: [...guiResourceReferences.values()].sort((a, b) => a.id.localeCompare(b.id)),
  missingScreens,
  orphanScreens,
  missingGuiResources,
  invalidGuiJson,
  errors,
};

fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
console.log(`GUI parity: ${report.result}`);
console.log(`Menus/screens: ${declarations.size}/${screenRegistrations.size}`);
console.log(`BuildCraft GUI resources: ${guiResourceReferences.size}; GUI PNGs: ${guiPngFiles.length}`);
console.log(`GUI JSON files parsed: ${guiJsonFiles.length}; errors: ${errors.length}`);
console.log(`Report: ${reportPath}`);

if (errors.length > 0) {
  for (const error of errors) console.error(`- ${error}`);
  process.exitCode = 1;
}
