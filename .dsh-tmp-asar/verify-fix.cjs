// End-to-end check of DSH's readPluginMeta resource resolution for the fixed plugin.
const { createRequire } = require('node:module');
const { fileURLToPath, pathToFileURL } = require('node:url');
const fs = require('node:fs');
const path = require('node:path');

const addonReq = createRequire('C:\\Users\\wf\\.dsh\\profiles\\node_modules\\node-addon-require-builtin\\lib\\index.js');
const addon = addonReq('node-addon-require-builtin');
const esm = addon.requireBuiltin('internal/modules/esm/loader').getOrInitializeCascadedLoader();

function resolvePluginResource(specifier, parentURL) {
  return fileURLToPath(esm.resolveSync(parentURL, { specifier, attributes: {} }).url);
}
function missingResource(error) {
  return ['ERR_PACKAGE_PATH_NOT_EXPORTED', 'ERR_MODULE_NOT_FOUND', 'MODULE_NOT_FOUND', 'ENOENT', 'ENOTDIR'].includes(error?.code);
}
function optionalResourcePath(specifier, parentURL) {
  try { return resolvePluginResource(specifier, parentURL); } catch (e) { if (missingResource(e)) return undefined; throw e; }
}
function dictionariesOf(englishPath, specifier, parentURL) {
  const out = {};
  for (const entry of fs.readdirSync(path.dirname(englishPath), { withFileTypes: true })) {
    if (!entry.name.endsWith('.json')) continue;
    const resource = `${specifier}/locale/${entry.name}`;
    const file = resolvePluginResource(resource, parentURL);
    const parsed = JSON.parse(fs.readFileSync(file, 'utf8'));
    out[entry.name.slice(0, -5).toLowerCase()] = parsed.meta;
  }
  return out;
}
function readPluginMeta(specifier, parentURL) {
  try {
    const englishPath = optionalResourcePath(`${specifier}/locale/en.json`, parentURL);
    const dictionaries = englishPath === undefined ? {} : dictionariesOf(englishPath, specifier, parentURL);
    const manifestPath = optionalResourcePath(`${specifier}/package.json`, parentURL);
    const manifest = manifestPath === undefined ? undefined : JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    return { specifier, englishPath, dictionaries, manifestTitle: manifest?.name };
  } catch (error) {
    return { specifier, error: `Plugin metadata for ${specifier}: ${String(error)}` };
  }
}

const dir = 'C:\\Users\\wf\\.dsh\\profiles\\desktop\\node_modules\\@a9i5k4\\dsh-auto-memory';
const specifier = '@a9i5k4/dsh-auto-memory';
console.log('--- plugin-manager path (parent = bundle package.json) ---');
console.log(JSON.stringify(readPluginMeta(specifier, pathToFileURL(path.join(dir, 'package.json')).href), null, 2));
console.log('--- inventory path (parent = profile package.json) ---');
console.log(JSON.stringify(readPluginMeta(specifier, pathToFileURL('C:\\Users\\wf\\.dsh\\profiles\\desktop\\package.json').href), null, 2));
console.log('--- control: broken plugin without locale would return englishPath undefined ---');
console.log('locale dir contents:', fs.readdirSync(path.join(dir, 'locale')).join(', '));
