const fs = require('fs');
const path = require('path');
const p = 'C:\\Users\\wf\\AppData\\Local\\Programs\\DeepSeek Harness\\resources\\app.asar';
const fd = fs.openSync(p, 'r');
const sz = Buffer.alloc(16);
fs.readSync(fd, sz, 0, 16, 0);
const headerSize = sz.readUInt32LE(12);
const hb = Buffer.alloc(headerSize);
fs.readSync(fd, hb, 0, headerSize, 16);
const json = JSON.parse(hb.toString('utf8').replace(/\0+$/, ''));
const base = 16 + headerSize;
const outDir = 'D:\\downloads\\algs4-master\\algs4-master\\.dsh-tmp-asar';
fs.mkdirSync(outDir, { recursive: true });
function walk(node, prefix) {
  for (const [k, v] of Object.entries(node.files || {})) {
    const pth = prefix + '/' + k;
    if (v.files) { walk(v, pth); }
    else {
      const name = pth.split('/').pop();
      if (/\.(js|cjs|mjs)$/.test(name) && /dsh-(plugin-manager|host-plugin-inventory|client-ui-settings-plugin-inventory|cli)/.test(pth)) {
        const buf = Buffer.alloc(v.size);
        fs.readSync(fd, buf, 0, v.size, base + Number(v.offset));
        const flat = name + '__' + pth.replace(/[\\/]/g, '_');
        fs.writeFileSync(path.join(outDir, flat), buf);
      }
    }
  }
}
walk(json, '');
console.log('extracted', fs.readdirSync(outDir).length, 'files to', outDir);
