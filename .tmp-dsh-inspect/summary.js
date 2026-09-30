const fs = require('fs');
const file = process.argv[2];
const from = parseInt(process.argv[3] || '0', 10);
const to = parseInt(process.argv[4] || '999999', 10);
const maxLen = parseInt(process.argv[5] || '700', 10);
const lines = fs.readFileSync(file, 'utf8').split('\n').filter(Boolean);
const cut = (s) => (s && s.length > maxLen ? s.slice(0, maxLen) + '…' : s || '');
lines.forEach((l, i) => {
  let o;
  try { o = JSON.parse(l); } catch { return; }
  const seq = o.seq;
  if (seq === undefined || seq < from || seq > to) return;
  const t = o.type;
  const d = o.data || {};
  if (t === 'user/message') {
    const txt = (d.content || []).map((c) => c.text || `[${c.type}]`).join(' ');
    console.log(`#${seq} USER: ${cut(txt.replace(/\s+/g, ' '))}`);
  } else if (t === 'assistant/message') {
    const parts = (d.message?.content || []).map((c) => {
      if (c.type === 'reasoning') return `REASON: ${cut(c.text.replace(/\s+/g, ' '))}`;
      if (c.type === 'text') return `TEXT: ${cut(c.text)}`;
      if (c.type === 'tool-call') return `CALL ${c.name} ${cut(c.arguments)}`;
      return `[${c.type}]`;
    });
    console.log(`#${seq} ASSISTANT(t${d.turn}s${d.step}):\n  ${parts.join('\n  ')}`);
  } else if (t === 'tool/result') {
    const txt = (d.message?.content || []).map((c) => c.text || '').join(' ');
    console.log(`#${seq} TOOLRESULT${d.error ? ' ERR=' + JSON.stringify(d.error) : ''}: ${cut(txt.replace(/\s+/g, ' '))}`);
  } else if (t === 'assistant/attempt') {
    const f = d.stream?.[0]?.chunk?.finish;
    if (f) console.log(`#${seq} ATTEMPT(t${d.turn}s${d.step}) finish=${JSON.stringify(f)}`);
  } else if (t === 'turn/end' || t === 'turn/start' || t === 'step/start' || t === 'step/end') {
    console.log(`#${seq} ${t.toUpperCase()} ${JSON.stringify(d).slice(0, 300)}`);
  } else if (t === 'llm/retry' || t.includes('error')) {
    console.log(`#${seq} ${t.toUpperCase()} ${cut(JSON.stringify(d))}`);
  }
});
