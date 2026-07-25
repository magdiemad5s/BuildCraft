#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import { TextDecoder } from "node:util";
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

const assetsRoot = option("--assets-root", path.join(laneRoot, "src", "main", "resources", "assets"));
const referenceJarPath = option("--reference-jar", path.join(workspaceRoot, "buildcraft-all-8.0.0.jar"));
const identityManifestPath = option(
  "--identity-manifest",
  path.join(repoRoot, "legacy-1.12.2-contract", "LEGACY_IDENTITY_MANIFEST.json")
);
const reportPath = option("--report", path.join(laneRoot, "LANGUAGE_PARITY_REPORT.json"));
const restoreMissing = process.argv.includes("--restore-missing");
const utf8 = new TextDecoder("utf-8", { fatal: true });

function normalize(value) {
  return value.split(path.sep).join("/");
}

function relativeToLane(file) {
  return normalize(path.relative(laneRoot, file));
}

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex").toUpperCase();
}

function walk(root) {
  if (!fs.existsSync(root)) return [];
  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
    const absolute = path.join(root, entry.name);
    if (entry.isDirectory()) files.push(...walk(absolute));
    else if (entry.isFile()) files.push(absolute);
  }
  return files;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function readZipEntries(file) {
  const bytes = fs.readFileSync(file);
  let eocd = -1;
  for (let offset = bytes.length - 22; offset >= Math.max(0, bytes.length - 22 - 0xffff); offset--) {
    if (bytes.readUInt32LE(offset) === 0x06054b50) {
      eocd = offset;
      break;
    }
  }
  if (eocd < 0) throw new Error(`ZIP end-of-central-directory not found: ${file}`);

  const entryCount = bytes.readUInt16LE(eocd + 10);
  let offset = bytes.readUInt32LE(eocd + 16);
  const entries = [];
  for (let index = 0; index < entryCount; index++) {
    if (bytes.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error(`Invalid ZIP central-directory entry ${index} at ${offset}`);
    }
    const method = bytes.readUInt16LE(offset + 10);
    const compressedSize = bytes.readUInt32LE(offset + 20);
    const uncompressedSize = bytes.readUInt32LE(offset + 24);
    const nameLength = bytes.readUInt16LE(offset + 28);
    const extraLength = bytes.readUInt16LE(offset + 30);
    const commentLength = bytes.readUInt16LE(offset + 32);
    const localOffset = bytes.readUInt32LE(offset + 42);
    const name = bytes.subarray(offset + 46, offset + 46 + nameLength).toString("utf8");

    if ([compressedSize, uncompressedSize, localOffset].includes(0xffffffff)) {
      throw new Error(`ZIP64 entry is unsupported: ${name}`);
    }
    if (bytes.readUInt32LE(localOffset) !== 0x04034b50) {
      throw new Error(`Invalid ZIP local header: ${name}`);
    }
    const localNameLength = bytes.readUInt16LE(localOffset + 26);
    const localExtraLength = bytes.readUInt16LE(localOffset + 28);
    const dataOffset = localOffset + 30 + localNameLength + localExtraLength;
    const compressed = bytes.subarray(dataOffset, dataOffset + compressedSize);
    let content;
    if (method === 0) content = Buffer.from(compressed);
    else if (method === 8) content = zlib.inflateRawSync(compressed);
    else throw new Error(`Unsupported ZIP compression method ${method}: ${name}`);
    if (content.length !== uncompressedSize) {
      throw new Error(`ZIP size mismatch for ${name}: expected ${uncompressedSize}, got ${content.length}`);
    }
    entries.push({ name, content });
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return entries;
}

function joinPropertyLines(text) {
  const logical = [];
  let current = "";
  let continuing = false;
  for (const raw of text.replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
    const part = continuing ? raw.replace(/^[ \t\f]+/, "") : raw;
    current += part;
    let trailingSlashes = 0;
    for (let index = current.length - 1; index >= 0 && current[index] === "\\"; index--) trailingSlashes++;
    if (trailingSlashes % 2 === 1) {
      current = current.slice(0, -1);
      continuing = true;
    } else {
      logical.push(current);
      current = "";
      continuing = false;
    }
  }
  if (continuing || current.length > 0) logical.push(current);
  return logical;
}

function unescapeProperty(value, label) {
  let output = "";
  for (let index = 0; index < value.length; index++) {
    const char = value[index];
    if (char !== "\\") {
      output += char;
      continue;
    }
    if (++index >= value.length) {
      output += "\\";
      break;
    }
    const escaped = value[index];
    if (escaped === "t") output += "\t";
    else if (escaped === "n") output += "\n";
    else if (escaped === "r") output += "\r";
    else if (escaped === "f") output += "\f";
    else if (escaped === "u") {
      const hex = value.slice(index + 1, index + 5);
      if (!/^[0-9a-fA-F]{4}$/.test(hex)) throw new Error(`Invalid Unicode escape in ${label}: \\u${hex}`);
      output += String.fromCharCode(Number.parseInt(hex, 16));
      index += 4;
    } else {
      output += escaped;
    }
  }
  return output;
}

function parseProperties(text, label) {
  const values = new Map();
  const duplicates = new Set();
  const lines = joinPropertyLines(text);
  for (let lineNumber = 0; lineNumber < lines.length; lineNumber++) {
    const line = lines[lineNumber];
    let start = 0;
    while (start < line.length && /[ \t\f]/.test(line[start])) start++;
    if (start >= line.length || line[start] === "#" || line[start] === "!") continue;

    let keyEnd = line.length;
    let valueStart = line.length;
    let escaped = false;
    for (let index = start; index < line.length; index++) {
      const char = line[index];
      if (!escaped && (char === "=" || char === ":" || /[ \t\f]/.test(char))) {
        keyEnd = index;
        valueStart = index;
        if (char === "=" || char === ":") valueStart++;
        else {
          while (valueStart < line.length && /[ \t\f]/.test(line[valueStart])) valueStart++;
          if (line[valueStart] === "=" || line[valueStart] === ":") valueStart++;
        }
        while (valueStart < line.length && /[ \t\f]/.test(line[valueStart])) valueStart++;
        break;
      }
      if (char === "\\" && !escaped) escaped = true;
      else escaped = false;
    }

    const key = unescapeProperty(line.slice(start, keyEnd), `${label}:${lineNumber + 1}`);
    const value = unescapeProperty(line.slice(valueStart), `${label}:${lineNumber + 1}`);
    if (values.has(key)) duplicates.add(key);
    values.set(key, value);
  }
  return { values, duplicates: [...duplicates].sort() };
}

function getLegacyLocales(errors) {
  if (!fs.existsSync(referenceJarPath)) {
    errors.push(`Reference JAR missing: ${referenceJarPath}`);
    return [];
  }
  const locales = [];
  for (const entry of readZipEntries(referenceJarPath)) {
    const match = entry.name.match(/^assets\/([^/]+)\/lang\/([^/]+)\.lang$/);
    if (!match) continue;
    const namespace = match[1];
    const locale = match[2].toLowerCase();
    const parsed = parseProperties(utf8.decode(entry.content), entry.name);
    locales.push({ namespace, locale, sourceEntry: entry.name, ...parsed });
  }
  locales.sort((a, b) => `${a.namespace}/${a.locale}`.localeCompare(`${b.namespace}/${b.locale}`));
  return locales;
}

function parseOutputLanguages(errors) {
  const files = walk(assetsRoot).filter(file => path.basename(path.dirname(file)).toLowerCase() === "lang");
  const jsonFiles = files.filter(file => path.extname(file).toLowerCase() === ".json").sort();
  const legacyLangFiles = files.filter(file => path.extname(file).toLowerCase() === ".lang").sort();
  const uppercaseLocaleFiles = [];
  let parsedCount = 0;
  for (const file of jsonFiles) {
    if (path.basename(file) !== path.basename(file).toLowerCase()) uppercaseLocaleFiles.push(relativeToLane(file));
    try {
      const value = readJson(file);
      if (value === null || Array.isArray(value) || typeof value !== "object") {
        errors.push(`Language JSON root must be an object: ${relativeToLane(file)}`);
        continue;
      }
      for (const [key, translation] of Object.entries(value)) {
        if (typeof translation !== "string") {
          errors.push(`Language value must be a string: ${relativeToLane(file)} key ${key}`);
        }
      }
      parsedCount++;
    } catch (cause) {
      errors.push(`Invalid language JSON ${relativeToLane(file)}: ${cause.message}`);
    }
  }
  for (const file of uppercaseLocaleFiles) errors.push(`Locale filename must be lowercase: ${file}`);
  for (const file of legacyLangFiles) errors.push(`Legacy .lang file remains in output resources: ${relativeToLane(file)}`);
  return {
    jsonFileCount: jsonFiles.length,
    parsedJsonFileCount: parsedCount,
    uppercaseLocaleFiles,
    legacyLangFiles: legacyLangFiles.map(relativeToLane)
  };
}

function loadTarget(file, errors) {
  if (!fs.existsSync(file)) return {};
  try {
    const value = readJson(file);
    if (value === null || Array.isArray(value) || typeof value !== "object") {
      errors.push(`Language JSON root must be an object: ${relativeToLane(file)}`);
      return null;
    }
    return value;
  } catch (cause) {
    errors.push(`Invalid language JSON ${relativeToLane(file)}: ${cause.message}`);
    return null;
  }
}

function buildLanguageIndex(errors) {
  const index = new Map();
  const files = walk(assetsRoot)
    .filter(file => path.extname(file).toLowerCase() === ".json")
    .sort();
  for (const file of files) {
    const relative = normalize(path.relative(assetsRoot, file));
    const match = relative.match(/^([^/]+)\/lang\/([^/]+)\.json$/i);
    if (!match) continue;
    let value;
    try {
      value = readJson(file);
    } catch (cause) {
      errors.push(`Invalid language JSON ${relativeToLane(file)}: ${cause.message}`);
      continue;
    }
    if (value === null || Array.isArray(value) || typeof value !== "object") continue;
    const locale = match[2].toLowerCase();
    if (!index.has(locale)) index.set(locale, new Map());
    const translations = index.get(locale);
    for (const [key, translation] of Object.entries(value)) {
      if (key === "_comment") continue;
      const previous = translations.get(key);
      translations.set(key, {
        value: translation,
        files: [...(previous?.files ?? []), relativeToLane(file)]
      });
    }
  }
  return index;
}

function restoreLocales(locales, errors) {
  const restored = new Map();
  const languageIndex = buildLanguageIndex(errors);
  for (const legacy of locales) {
    const targetFile = path.join(assetsRoot, legacy.namespace, "lang", `${legacy.locale}.json`);
    const target = loadTarget(targetFile, errors);
    if (target === null) continue;
    if (!languageIndex.has(legacy.locale)) languageIndex.set(legacy.locale, new Map());
    const effective = languageIndex.get(legacy.locale);
    const added = [];
    for (const key of [...legacy.values.keys()].sort()) {
      if (!effective.has(key)) {
        target[key] = legacy.values.get(key);
        added.push(key);
        effective.set(key, { value: legacy.values.get(key), files: [relativeToLane(targetFile)] });
      }
    }
    if (added.length > 0) {
      fs.mkdirSync(path.dirname(targetFile), { recursive: true });
      fs.writeFileSync(targetFile, `${JSON.stringify(target, null, 2)}\n`, "utf8");
      restored.set(`${legacy.namespace}/${legacy.locale}`, added);
    }
  }
  return restored;
}

function auditLocales(locales, restored, errors) {
  const results = [];
  const languageIndex = buildLanguageIndex(errors);
  for (const legacy of locales) {
    const id = `${legacy.namespace}/${legacy.locale}`;
    const targetFile = path.join(assetsRoot, legacy.namespace, "lang", `${legacy.locale}.json`);
    const target = loadTarget(targetFile, errors);
    const direct = target ?? {};
    const directKeys = Object.keys(direct).filter(key => key !== "_comment");
    const effective = languageIndex.get(legacy.locale) ?? new Map();
    const legacyKeys = [...legacy.values.keys()].sort();
    const missingKeys = legacyKeys.filter(key => !effective.has(key));
    const modernOverrideKeys = legacyKeys.filter(
      key => effective.has(key) && effective.get(key).value !== legacy.values.get(key)
    );
    const satisfiedByOtherNamespaceKeys = legacyKeys.filter(
      key => !Object.hasOwn(direct, key) && effective.has(key)
    );
    const duplicateCurrentKeys = legacyKeys.filter(key => (effective.get(key)?.files.length ?? 0) > 1);
    const extraModernKeys = directKeys.filter(key => !legacy.values.has(key)).sort();
    if (missingKeys.length > 0) errors.push(`Missing ${missingKeys.length} legacy language keys: ${id}`);
    results.push({
      namespace: legacy.namespace,
      locale: legacy.locale,
      classification: legacy.namespace === "buildcraftcompat" ? "obsolete_optional_compat" : "active_legacy",
      requiredForParity: true,
      sourceEntry: legacy.sourceEntry,
      targetFile: relativeToLane(targetFile),
      legacyKeyCount: legacyKeys.length,
      targetKeyCount: directKeys.length,
      effectiveLocaleKeyCount: effective.size,
      restoredKeys: restored.get(id) ?? [],
      missingKeys,
      modernOverrideKeys,
      satisfiedByOtherNamespaceKeys,
      duplicateCurrentKeys,
      extraModernKeys,
      duplicateLegacyKeys: legacy.duplicates
    });
  }
  return results;
}

const errors = [];
let identityManifest = null;
try {
  identityManifest = readJson(identityManifestPath);
} catch (cause) {
  errors.push(`Invalid identity manifest ${identityManifestPath}: ${cause.message}`);
}

let actualJarHash = null;
if (fs.existsSync(referenceJarPath)) {
  actualJarHash = sha256(referenceJarPath);
  const expected = identityManifest?.legacySource?.referenceJar?.sha256?.toUpperCase();
  if (expected && actualJarHash !== expected) {
    errors.push(`Reference JAR SHA-256 mismatch: expected ${expected}, got ${actualJarHash}`);
  }
}

let locales = [];
try {
  locales = getLegacyLocales(errors);
} catch (cause) {
  errors.push(`Unable to read released JAR languages: ${cause.message}`);
}
const restored = restoreMissing ? restoreLocales(locales, errors) : new Map();
const localeResults = auditLocales(locales, restored, errors);
const outputValidation = parseOutputLanguages(errors);

const summary = {
  releasedLocaleFiles: locales.length,
  releasedKeys: localeResults.reduce((sum, locale) => sum + locale.legacyKeyCount, 0),
  restoredKeys: localeResults.reduce((sum, locale) => sum + locale.restoredKeys.length, 0),
  missingKeys: localeResults.reduce((sum, locale) => sum + locale.missingKeys.length, 0),
  modernOverrideKeys: localeResults.reduce((sum, locale) => sum + locale.modernOverrideKeys.length, 0),
  extraModernKeys: localeResults.reduce((sum, locale) => sum + locale.extraModernKeys.length, 0),
  obsoleteOptionalCompatKeys: localeResults
    .filter(locale => locale.classification === "obsolete_optional_compat")
    .reduce((sum, locale) => sum + locale.legacyKeyCount, 0),
  parsedJsonFiles: outputValidation.parsedJsonFileCount,
  invalidJsonFiles: outputValidation.jsonFileCount - outputValidation.parsedJsonFileCount,
  errors: errors.length
};

const report = {
  schemaVersion: 1,
  status: errors.length === 0 ? "pass" : "fail",
  restorationApplied: restoreMissing,
  referenceJar: {
    path: relativeToLane(referenceJarPath),
    sha256: actualJarHash,
    expectedSha256: identityManifest?.legacySource?.referenceJar?.sha256 ?? null
  },
  summary,
  outputValidation,
  locales: localeResults,
  errors
};

fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
console.log(`Language parity: ${report.status.toUpperCase()}`);
console.log(`Released locales: ${summary.releasedLocaleFiles}; keys: ${summary.releasedKeys}`);
console.log(`Restored: ${summary.restoredKeys}; missing: ${summary.missingKeys}; parsed JSON: ${summary.parsedJsonFiles}`);
console.log(`Report: ${reportPath}`);
if (errors.length > 0) {
  for (const message of errors) console.error(`ERROR: ${message}`);
  process.exitCode = 1;
}
