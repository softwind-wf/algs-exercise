const fs = require('fs');
const zlib = require('zlib');
const file = process.argv[2];
const buf = fs.readFileSync(file);
console.error('compressed bytes =', buf.length);
// zstd magic: 28 B5 2F FD
let off = 0;
let frames = 0;
let out = '';
while (off < buf.length) {
  if (buf[off] === 0x28 && buf[off + 1] === 0xb5 && buf[off + 2] === 0x2f && buf[off + 3] === 0xfd) {
    frames++;
    // naive: try decompress from off, growing slice until success
    let lo = off + 4, hi = buf.length, ok = null;
    // binary search for largest slice that decompresses
    let best = null;
    for (let end = buf.length; end > off; end = off + Math.floor((end - off) * 0.98) - 1) {
      try {
        const d = zlib.zstdDecompressSync(buf.subarray(off, end));
        best = { d, end };
        break;
      } catch (e) { if (e.code === 'Z_DATA_ERROR' || /unknown frame|premature|unexpected end/i.test(e.message)) continue; }
      if (end <= off + 4) break;
    }
    if (!best) { console.error('frame at', off, 'failed'); break; }
    out += best.d.toString('utf8');
    off = best.end;
  } else {
    off++;
  }
}
console.error('frames =', frames, 'out chars =', out.length);
const lines = out.split(/\r?\n/).filter(Boolean);
console.error('lines =', lines.length);
fs.writeFileSync(process.argv[3], out, 'utf8');
