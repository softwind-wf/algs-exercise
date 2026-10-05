const fs = require('fs');
const p = 'C:\\Users\\wf\\AppData\\Local\\Programs\\DeepSeek Harness\\resources\\app.asar';
const fd = fs.openSync(p, 'r');
const sz = Buffer.alloc(16);
fs.readSync(fd, sz, 0, 16, 0);
const headerSize = sz.readUInt32LE(12);
const hb = Buffer.alloc(headerSize);
fs.readSync(fd, hb, 0, headerSize, 16);
const json = JSON.parse(hb.toString('utf8').replace(/\0+$/, ''));
const base = 16 + headerSize;
const entries = [];
(function walk(node, prefix) {
  for (const [k, v] of Object.entries(node.files || {})) {
    const pth = prefix + '/' + k;
    if (v.files) walk(v, pth);
    else entries.push([pth, v]);
  }
})(json, '');

const needles = ['locale/en.json', 'dictionariesOf', 'readPluginMeta', '/locale/'];
for (const needle of needles) {
  console.log('### needle:', JSON.stringify(needle));
  for (const [pth, v] of entries) {
    if (!/\.(js|cjs|mjs)$/.test(pth) || v.offset === undefined || v.size > 5_000_000) continue;
    const buf = Buffer.alloc(v.size);
    fs.readSync(fd, buf, 0, v.size, base + Number(v.offset));
    const text = buf.toString('utf8');
    let idx = text.indexOf(needle);
    let count = 0;
    const samples = [];
    while (idx !== -1 && count < 3) {
      count++;
      const line = text.slice(Math.max(0, idx - 120), idx + 120).replace(/\s+/g, ' ');
      samples.push(line);
      idx = text.indexOf(needle, idx + 1);
    }
    const total = text.split(needle).length - 1;
    if (total > 0) {
      console.log(`  ${pth}  x${total}`);
      for (const s of samples) console.log('      ...' + s + '...');
    }
  }
}
