// Post-upgrade verification for @a9i5k4/dsh-auto-memory@3.2.8 in the desktop profile.
const { createRequire } = require('node:module');
const { fileURLToPath, pathToFileURL } = require('node:url');
const fs = require('node:fs');
const path = require('node:path');

const pkgDir = 'C:\\Users\\wf\\.dsh\\profiles\\desktop\\node_modules\\@a9i5k4\\dsh-auto-memory';
const spec = '@a9i5k4/dsh-auto-memory';
const parent = pathToFileURL('C:\\Users\\wf\\.dsh\\profiles\\desktop\\package.json').href;

const pkg = JSON.parse(fs.readFileSync(path.join(pkgDir, 'package.json'), 'utf8'));
console.log('name:', pkg.name, '| version:', pkg.version);
console.log('main:', pkg.main);
console.log('exports keys:', Object.keys(pkg.exports || {}).join(', '));
console.log('dsh.bundle:', JSON.stringify(pkg.dsh?.bundle));
console.log('dsh.client.inject:', JSON.stringify(pkg.dsh?.client?.inject));
console.log('peerDeps:', JSON.stringify(pkg.peerDependencies || {}));
console.log('has locale dir:', fs.existsSync(path.join(pkgDir, 'locale')), fs.existsSync(path.join(pkgDir, 'locale')) ? fs.readdirSync(path.join(pkgDir, 'locale')).join(', ') : '');

const addonReq = createRequire('C:\\Users\\wf\\.dsh\\profiles\\node_modules\\node-addon-require-builtin\\lib\\index.js');
const addon = addonReq('node-addon-require-builtin');
const esm = addon.requireBuiltin('internal/modules/esm/loader').getOrInitializeCascadedLoader();
function resolve(s) {
  return fileURLToPath(esm.resolveSync(parent, { specifier: s, attributes: {} }).url);
}

console.log('\n--- resource resolution (readPluginMeta path) ---');
for (const s of [`${spec}/locale/en.json`, `${spec}/locale/zh.json`, `${spec}/package.json`]) {
  try { const p = resolve(s); console.log('OK  ', s, '->', p, fs.existsSync(p) ? '(exists)' : '(MISSING)'); }
  catch (e) { console.log('ERR ', s, '->', e.code, String(e.message).split('\n')[0]); }
}
console.log('\n--- entry resolution (loader path) ---');
for (const s of [spec, `${spec}/client`, `${spec}/typert`, `${spec}/remote`]) {
  try { const p = resolve(s); console.log('OK  ', s, '->', path.basename(p), fs.existsSync(p) ? '(exists)' : '(MISSING)'); }
  catch (e) { console.log('ERR ', s, '->', e.code, String(e.message).split('\n')[0]); }
}
console.log('\n--- peer dependency presence ---');
for (const dep of Object.keys(pkg.peerDependencies || {}).concat(pkg.dsh?.client?.inject || [])) {
  try { console.log('OK  ', dep, '->', createRequire(path.join(pkgDir, 'package.json')).resolve(dep + '/package.json')); }
  catch (e) { console.log('MISS', dep, '->', e.code); }
}
