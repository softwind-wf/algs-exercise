const { createRequire } = require('module');
const Module = require('module');
const req = createRequire('C:/Users/wf/.dsh/profiles/desktop/');
try {
  const r = req.resolve('@a9i5k4/dsh-auto-memory/locale/en.json');
  console.log('require.resolve OK ->', r);
} catch (e) { console.log('require.resolve ERR', e.code, e.message.split('\n')[0]); }
try {
  const r2 = req.resolve('@a9i5k4/dsh-auto-memory/package.json');
  console.log('pkg resolve OK ->', r2);
} catch (e) { console.log('pkg resolve ERR', e.code, e.message.split('\n')[0]); }
// Try internal ESM loader if exposed
const loader = Module.ModuleLoader;
console.log('ModuleLoader ctor?', typeof loader);
const inst = Module.ModuleLoader && Module.ModuleLoader.fromInternal ? Module.ModuleLoader.fromInternal() : undefined;
console.log('loader inst', inst && inst.version);
if (inst) {
  try {
    const out = inst.version === 'v2'
      ? inst.resolveSync('file:///C:/Users/wf/.dsh/profiles/desktop/package.json', { specifier: '@a9i5k4/dsh-auto-memory/locale/en.json', attributes: {} })
      : inst.resolveSync('@a9i5k4/dsh-auto-memory/locale/en.json', 'file:///C:/Users/wf/.dsh/profiles/desktop/package.json', {});
    console.log('loader.resolveSync OK ->', JSON.stringify(out));
  } catch (e) { console.log('loader.resolveSync ERR', e.code, e.message.split('\n')[0]); }
}
