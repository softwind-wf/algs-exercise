const { createRequire } = require('node:module');
const { pathToFileURL } = require('node:url');
const addonReq = createRequire('C:\\Users\\wf\\.dsh\\profiles\\node_modules\\node-addon-require-builtin\\lib\\index.js');
const addon = addonReq('node-addon-require-builtin');
const esm = addon.requireBuiltin('internal/modules/esm/loader').getOrInitializeCascadedLoader();
console.log('type:', Object.prototype.toString.call(esm));
console.log('own props:', Object.getOwnPropertyNames(esm).join(', '));
console.log('proto props:', Object.getOwnPropertyNames(Object.getPrototypeOf(esm)).slice(0, 40).join(', '));
console.log('version:', esm.version);
const parent = pathToFileURL('C:\\Users\\wf\\.dsh\\profiles\\desktop\\package.json').href;
try {
  const out = esm.resolveSync(parent, { specifier: '@a9i5k4/dsh-auto-memory/locale/en.json', attributes: {} });
  console.log('v2 OK:', JSON.stringify(out));
} catch (e) { console.log('v2 ERR:', e.code, String(e.message).split('\n')[0]); }
