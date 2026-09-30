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

const sid = process.argv[2];
const from = parseInt(process.argv[3] || '0', 10);
const to = parseInt(process.argv[4] || '99999', 10);
const root = path.join(process.env.USERPROFILE, '.dsh', 'sessions', '--D-downloads-algs4-master-algs4-master--');
const evs = decompressAll(path.join(root, sid, 'session.v4.jsonl.zstd')).split('\n').filter(Boolean)
  .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean).slice(from, to);

for (const e of evs) {
  let brief;
  const d = e.data || {};
  if (e.type === 'user/message') brief = 'USER: ' + (d.content || []).map(c => c.type === 'text' ? c.text : '[' + c.type + ']').join(' ').slice(0, 260);
  else if (e.type === 'tool/call') brief = 'CALL ' + d.name + ' :: ' + String(d.arguments).slice(0, 220);
  else if (e.type === 'tool/result') {
    const txt = (d.message && d.message.content || []).map(c => c.text || '').join('');
    brief = 'RESULT len=' + txt.length + ' :: ' + txt.slice(0, 160);
  }
  else if (e.type === 'assistant/attempt') {
    const st = d.stream || [];
    const fin = st.find(s => s.chunk && s.chunk.type === 'finish');
    brief = 'ATTEMPT ' + (fin && fin.chunk.reason && fin.chunk.reason.kind === 'error' ? 'ERR ' + fin.chunk.reason.failure.message : 'ok');
  }
  else if (e.type === 'assistant/message') {
    const c = d.message.content || [];
    brief = 'ASSIST ' + c.map(x => x.type === 'tool-call' ? 'call:' + x.name : x.type).join(',') + ' :: ' + JSON.stringify(c).slice(0, 160);
  }
  else if (e.type === 'agent/inbox/spliced') brief = 'SPLICE ' + JSON.stringify(d).slice(0, 220);
  else if (e.type === 'turn/end') brief = 'TURNEND ' + JSON.stringify(d.reason).slice(0, 160);
  else brief = JSON.stringify(d).slice(0, 140);
  console.log(`seq${e.seq} ${e.type} | ${String(brief).replace(/\s+/g, ' ')}`);
}
