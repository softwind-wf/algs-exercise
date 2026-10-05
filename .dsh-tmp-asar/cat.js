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
const want = process.argv[2];
function walk(node, prefix) {
  for (const [k, v] of Object.entries(node.files || {})) {
    const pth = prefix + '/' + k;
    if (v.files) walk(v, pth);
    else if (pth === want) {
      const buf = Buffer.alloc(v.size);
      fs.readSync(fd, buf, 0, v.size, base + Number(v.offset));
      console.log('=== ' + pth);
      console.log(buf.toString('utf8'));
    }
  }
}
walk(json, '');
