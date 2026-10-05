const { createRequire } = require('node:module');
const { fileURLToPath, pathToFileURL } = require('node:url');
const path = require('node:path');
const fs = require('node:fs');

const addonReq = createRequire('C:\\Users\\wf\\.dsh\\profiles\\node_modules\\node-addon-require-builtin\\lib\\index.js');
const addon = addonReq('node-addon-require-builtin');
const esm = addon.requireBuiltin('internal/modules/esm/loader').getOrInitializeCascadedLoader();
console.log('loader.version =', esm.version, '| has resolveSync:', typeof esm.resolveSync);

const parentUrl = pathToFileURL('C:\\Users\\wf\\.dsh\\profiles\\desktop\\package.json').href;
console.log('parentUrl =', parentUrl);

function call(spec, base, label) {
  for (const variant of ['v2', 'v1']) {
    try {
      const out = variant === 'v2'
        ? esm.resolveSync(base, { specifier: spec, attributes: {} })
        : esm.resolveSync(spec, base, {});
      console.log(`  [${variant}] ${label} ${spec} -> OK`, JSON.stringify(out));
    } catch (e) {
      console.log(`  [${variant}] ${label} ${spec} -> ${e.code} | ${String(e.message).split('\n')[0]}`);
    }
  }
}
call('@a9i5k4/dsh-auto-memory/locale/en.json', parentUrl, 'string-parent');
call('@a9i5k4/dsh-auto-memory/package.json', parentUrl, 'string-parent');
call('dsh-plugin-whale-pet/locale/en.json', parentUrl, 'string-parent');
call('@a9i5k4/dsh-auto-memory/locale/en.json', pathToFileURL('C:\\Users\\wf\\.dsh\\profiles\\desktop\\node_modules\\@a9i5k4\\dsh-auto-memory\\lib\\index.js').href, 'pkg-parent');
console.log('locale dir exists:', fs.existsSync('C:\\Users\\wf\\.dsh\\profiles\\desktop\\node_modules\\@a9i5k4\\dsh-auto-memory\\locale'));
