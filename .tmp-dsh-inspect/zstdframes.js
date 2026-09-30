const fs = require('fs');
const zlib = require('zlib');
const file = process.argv[2];
const outFile = process.argv[3];
const buf = fs.readFileSync(file);
const MAGIC = Buffer.from([0x28, 0xb5, 0x2f, 0xfd]);
const offsets = [];
let i = 0;
while ((i = buf.indexOf(MAGIC, i)) !== -1) { offsets.push(i); i += 4; }
const pieces = [];
const bad = [];
for (let k = 0; k < offsets.length; k++) {
  try {
    pieces.push(zlib.zstdDecompressSync(buf.subarray(offsets[k])).toString('utf8'));
  } catch (e) {
    bad.push(offsets[k] + ':' + e.message);
  }
}
console.error('magic hits =', offsets.length, 'decoded =', pieces.length, 'failed =', bad.length, bad.slice(0, 5).join(' | '));
const out = pieces.join('');
const lines = out.split(/\r?\n/).filter(Boolean);
let ok = 0;
for (const l of lines) { try { JSON.parse(l); ok++; } catch {} }
console.error('lines =', lines.length, 'json ok =', ok);
fs.writeFileSync(outFile, lines.join('\n'), 'utf8');
