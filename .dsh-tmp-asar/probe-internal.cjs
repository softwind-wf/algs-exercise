const path = require('node:path');
const addonPath = 'C:\\Users\\wf\\AppData\\Local\\Programs\\DeepSeek Harness\\resources\\app.asar.unpacked\\dsh\\node_modules\\node-addon-require-builtin-win32-x64-msvc\\prebuilt\\win32-x64-msvc-napi-v9.node';
let addon;
try {
  addon = require(addonPath);
  console.log('addon keys:', Object.keys(addon));
} catch (e) {
  console.log('addon load ERR:', e.code, e.message.split('\n')[0]);
  process.exit(0);
}
let esmModule;
try {
  esmModule = addon.requireBuiltin('internal/modules/esm/loader');
  console.log('requireBuiltin ok, keys:', Object.keys(esmModule));
} catch (e) {
  console.log('requireBuiltin ERR:', e.code, e.message.split('\n')[0]);
  process.exit(0);
}
const esm = esmModule.getOrInitializeCascadedLoader ? esmModule.getOrInitializeCascadedLoader() : esmModule.getOrInitializeCascadedLoader;
console.log('modern:', 'getOrCreateModuleJob' in esm, 'resolveSync:', typeof esm.resolveSync);
const parentURL = 'file:///C:/Users/wf/.dsh/profiles/desktop/package.json';
const targets = [
  '@a9i5k4/dsh-auto-memory/locale/en.json',
  '@a9i5k4/dsh-auto-memory/package.json',
  'dsh-plugin-whale-pet/locale/en.json',
  '@a9i5k4/dsh-auto-memory/lib/index.js',
  '@a9i5k4/dsh-auto-memory/nonexistent/x.json'
];
for (const target of targets) {
  try {
    const out = esm.resolveSync(parentURL, { specifier: target, attributes: {} });
    console.log('OK  ', target, '->', JSON.stringify(out));
  } catch (e) {
    console.log('ERR ', target, '->', e.code, '|', e.message.split('\n')[0]);
  }
}
