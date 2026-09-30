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

const sessionsRoot = path.join(process.env.USERPROFILE, '.dsh', 'sessions');
const found = [];
for (const ws of fs.readdirSync(sessionsRoot)) {
  const wsDir = path.join(sessionsRoot, ws);
  if (!fs.statSync(wsDir).isDirectory()) continue;
  for (const sid of fs.readdirSync(wsDir)) {
    const f = path.join(wsDir, sid, 'session.v4.jsonl.zstd');
    if (!fs.existsSync(f)) continue;
    let text;
    try { text = decompressAll(f); } catch { continue; }
    if (!text.includes('Content Exists Risk')) continue;
    const evs = text.split('\n').filter(Boolean).map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
    const first = evs.find(e => e.type === 'turn/end' && e.data.reason && e.data.reason.kind === 'error');
    const errCount = evs.filter(e => e.type === 'turn/end' && e.data.reason && e.data.reason.kind === 'error').length;
    found.push({ ws, sid, t: first ? first.time : 0, errCount, evs, msgs: evs.length });
  }
}
found.sort((a, b) => a.t - b.t);
console.log('sessions containing the risk error: ' + found.length);
for (const s of found) {
  console.log(`\n### ${s.sid}  ws=${s.ws}  firstError=${new Date(s.t).toISOString()}  errorTurns=${s.errCount}  events=${s.msgs}`);
  // show what came right before the first error
  const idx = s.evs.findIndex(e => e.type === 'turn/end' && e.data.reason && e.data.reason.kind === 'error');
  const ctx = s.evs.slice(Math.max(0, idx - 8), idx + 1);
  for (const e of ctx) {
    let brief = '';
    if (e.type === 'user/message') brief = 'USER: ' + (e.data.content || []).map(c => c.type === 'text' ? c.text : '[' + c.type + ']').join(' ').slice(0, 200);
    else if (e.type === 'tool/call') brief = 'TOOLCALL ' + e.data.name + ' ' + String(e.data.arguments).slice(0, 200);
    else if (e.type === 'tool/result') brief = 'TOOLRESULT len=' + JSON.stringify(e.data).length;
    else if (e.type === 'assistant/attempt') brief = 'ATTEMPT ' + JSON.stringify(e.data.stream || []).slice(0, 150);
    else if (e.type === 'turn/end') brief = 'TURNEND ' + JSON.stringify(e.data.reason).slice(0, 150);
    else brief = JSON.stringify(e.data).slice(0, 120);
    console.log(`   seq${e.seq} ${e.type} | ${brief.replace(/\s+/g, ' ')}`);
  }
}
