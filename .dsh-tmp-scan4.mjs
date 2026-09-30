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
const evs = decompressAll(path.join(root, 'session-37c61086-c798-47f0-a007-4dff74514db9', 'session.v4.jsonl.zstd'))
  .split('\n').filter(Boolean).map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
const e = evs.find(x => x.seq === 275);
const txt = e.data.message.content.map(c => c.text || '').join('');

const tests = {
  'prompt-injection 标记': /ignore (all )?(previous|prior) instructions|忽略(以上|之前|前面)|disregard (the )?(above|previous)|系统提示词|system prompt|you are an ai|<\|im_start\|>|<system>|Assistant:|###\s*Instruction/i,
  'base64 长串': /[A-Za-z0-9+/]{120,}={0,2}/,
  'hex 长串': /(?:[0-9a-f]{2}\s?){80,}/i,
  'URL/域名': /https?:\/\/[^\s"'<>]{4,80}/g,
  '模型/服务名': /ChatGPT|Claude|Gemini|Copilot|DeepSeek|Cursor|Trae|通义|文心|Grok|Midjourney/i,
  '代码块': /```/g,
};

console.log('blog text chars=' + txt.length);
for (const [name, re] of Object.entries(tests)) {
  const flags = re.flags.includes('g') ? re.flags : re.flags + 'g';
  const m = txt.match(new RegExp(re.source, flags));
  if (m) {
    const uniq = [...new Set(m.map(s => String(s).slice(0, 70)))];
    console.log(`HIT ${name}: count=${m.length} sample=${uniq.slice(0, 12).map(s => JSON.stringify(s)).join(', ')}`);
  } else console.log(`-- ${name}: none`);
}

// 长度分布/语言构成
const cjk = (txt.match(/[\u4e00-\u9fff]/g) || []).length;
const lat = (txt.match(/[A-Za-z]/g) || []).length;
console.log(`stats: cjk=${cjk} latin=${lat} lines=${txt.split('\n').length}`);
