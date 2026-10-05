const fs = require('node:fs');
const path = require('node:path');
const base = 'C:\\Users\\wf\\AppData\\Local\\Programs\\DeepSeek Harness\\resources\\app.asar.unpacked\\dsh\\node_modules\\node-addon-require-builtin-win32-x64-msvc';
function walk(dir, prefix) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    console.log(prefix + e.name + (e.isDirectory() ? '/' : ''));
    if (e.isDirectory()) walk(p, prefix + '  ');
  }
}
walk(base, '');
