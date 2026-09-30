const fs = require('fs');
const zlib = require('zlib');

const file = process.argv[2];
const mode = process.argv[3] || 'tail';
const n = parseInt(process.argv[4] || '20', 10);

const buf = fs.readFileSync(file);
let text;
try {
  text = zlib.zstdDecompressSync(buf).toString('utf8');
} catch (e) {
  console.error('decompress failed:', e.message);
  process.exit(1);
}
const lines = text.split(/\r?\n/).filter(Boolean);
console.error(`# lines=${lines.length}`);
let objs = [];
for (const l of lines) {
  try { objs.push(JSON.parse(l)); } catch (e) { objs.push({ __raw: l }); }
}
const sel = mode === 'tail' ? objs.slice(-n) : objs;
for (const o of sel) {
  const s = JSON.stringify(o);
  console.log(s.length > 4000 ? s.slice(0, 4000) + '…<truncated>' : s);
}
