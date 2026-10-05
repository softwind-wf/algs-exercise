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
const entries = [];
function walk(node, prefix) {
  for (const [k, v] of Object.entries(node.files || {})) {
    const pth = prefix + '/' + k;
    if (v.files) walk(v, pth);
    else entries.push([pth, v]);
  }
}
walk(json, '');
const needle = process.argv[2] || 'pluginPackages';
const hits = [];
for (const [pth, v] of entries) {
  if (!/\.(js|cjs|mjs|json)$/.test(pth)) continue;
  if (v.size === undefined || v.offset === undefined || v.size > 4_000_000) continue;
  const buf = Buffer.alloc(v.size);
  fs.readSync(fd, buf, 0, v.size, base + Number(v.offset));
  const text = buf.toString('utf8');
  if (text.includes(needle)) {
    hits.push(pth);
    const flat = pth.split('/').pop() + '@@' + pth.replace(/[\\/]/g, '_');
    fs.writeFileSync(path.join(outDir, flat), buf);
  }
}
console.log('hits for', needle, hits.length);
for (const h of hits) console.log('  ' + h);
