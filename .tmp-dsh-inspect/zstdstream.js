const fs = require('fs');
const zlib = require('zlib');
const file = process.argv[2];
const outFile = process.argv[3];
const chunks = [];
const s = zlib.createZstdDecompress();
s.on('data', (c) => chunks.push(c));
s.on('error', (e) => { console.error('stream error:', e.message); });
s.on('end', () => {
  const out = Buffer.concat(chunks).toString('utf8');
  console.error('out chars =', out.length, 'lines =', out.split(/\r?\n/).filter(Boolean).length);
  fs.writeFileSync(outFile, out, 'utf8');
});
fs.createReadStream(file).pipe(s);
