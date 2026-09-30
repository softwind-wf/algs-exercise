import { readFileSync } from 'node:fs';
import { zstdDecompressSync } from 'node:zlib';

const ZSTD_MAGIC = 4247762216;
function scanFrames(buffer) {
  const frames = [];
  let offset = 0;
  while (offset < buffer.length) {
    const start = offset;
    if (buffer.length - offset < 4) break;
    if (buffer.readUInt32LE(offset) !== ZSTD_MAGIC) throw new Error(`bad magic at ${offset}`);
    offset += 4;
    const descriptor = buffer.readUInt8(offset); offset += 1;
    const contentSizeFlag = descriptor >>> 6;
    const singleSegment = (descriptor & 32) !== 0;
    const checksum = (descriptor & 4) !== 0;
    const dictionaryFlag = descriptor & 3;
    const dictionaryBytes = dictionaryFlag === 3 ? 4 : dictionaryFlag;
    const contentSizeBytes = contentSizeFlag === 0 ? (singleSegment ? 1 : 0) : 1 << contentSizeFlag;
    offset += (singleSegment ? 0 : 1) + dictionaryBytes + contentSizeBytes;
    for (;;) {
      const blockHeader = buffer.readUIntLE(offset, 3); offset += 3;
      const lastBlock = (blockHeader & 1) !== 0;
      const blockType = (blockHeader >>> 1) & 3;
      const blockSize = blockHeader >>> 3;
      offset += blockType === 1 ? 1 : blockSize;
      if (lastBlock) break;
    }
    if (checksum) offset += 4;
    frames.push([start, offset]);
  }
  return frames;
}

const file = process.argv[2];
const mode = process.argv[3] ?? 'tail';
const grepPat = process.argv[4];
const buf = readFileSync(file);
const frames = scanFrames(buf);
let text = '';
for (const [s, e] of frames) text += zstdDecompressSync(buf.subarray(s, e)).toString('utf8');
const lines = text.split('\n').filter((l) => l.trim().length > 0);
const events = [];
for (const l of lines) { try { events.push(JSON.parse(l)); } catch {} }
console.log(`# frames=${frames.length} lines=${lines.length} parsed=${events.length}`);
let list = events.map((ev, i) => [i, ev]);
if (grepPat) list = list.filter(([, ev]) => JSON.stringify(ev).includes(grepPat));
const shown = mode === 'all' ? list : list.slice(-14);
for (const [i, ev] of shown) {
  const s = JSON.stringify(ev);
  console.log(`--- [${i}] ${s.length}B: ${s.slice(0, 2000)}`);
}
