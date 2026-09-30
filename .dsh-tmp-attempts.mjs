import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const MAGIC = Buffer.from([0x28, 0xb5, 0x2f, 0xfd]);
function decompressAll(file) {
  const buf = fs.readFileSync(file);
  const offsets = [];
  let i = buf.indexOf(MAGIC, 0);
  while (i !== -1) { offsets.push(i); i = buf.indexOf(MAGIC, i + 4); }
  const out = [];
  for (let k = 0; k < offsets.length; k++) {
    const start = offsets[k];
    const end = k + 1 < offsets.length ? offsets[k + 1] : buf.length;
    try { out.push(zlib.zstdDecompressSync(buf.subarray(start, end))); } catch {}
  }
  return Buffer.concat(out).toString('utf8');
}

const root = path.join(process.env.USERPROFILE, '.dsh', 'sessions', '--D-downloads-algs4-master-algs4-master--');
for (const sid of process.argv.slice(2)) {
  const evs = decompressAll(path.join(root, sid, 'session.v4.jsonl.zstd')).split('\n').filter(Boolean)
    .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
  console.log('\n### ' + sid);
  for (const e of evs) {
    if (e.type === 'assistant/attempt') {
      const st = e.data.stream || [];
      const fin = st.find(s => s.chunk && s.chunk.type === 'finish');
      const k = fin && fin.chunk.reason ? fin.chunk.reason.kind : '?';
      const tm = new Date(e.time).toLocaleTimeString('zh-CN', { hour12: false });
      console.log(`  ${tm} turn${e.data.turn}/step${e.data.step} ${k === 'error' ? 'ERR' : k}${k === 'error' ? ' ' + fin.chunk.reason.failure.message.slice(0, 60) : ''}`);
    }
    if (e.type === 'user/message') {
      const t = (e.data.content || []).map(c => c.text || '').join(' ').replace(/\s+/g, ' ');
      if (!t.startsWith('Current runtime context') && !t.startsWith('<system-reminder>') && !t.startsWith('background job')) {
        console.log(`  ${new Date(e.time).toLocaleTimeString('zh-CN', { hour12: false })} USER: ${t.slice(0, 90)}`);
      }
    }
  }
}
