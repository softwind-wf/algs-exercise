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

// collect every text block per session, up to a given seq (the failing request boundary)
function collect(sid, upto) {
  const evs = decompressAll(path.join(root, sid, 'session.v4.jsonl.zstd')).split('\n').filter(Boolean)
    .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
  const blocks = [];
  for (const e of evs) {
    if (e.seq > upto) break;
    const d = e.data || {};
    const push = (label, s) => { if (s && s.length) blocks.push({ seq: e.seq, label, s }); };
    if (e.type === 'user/message') push('user', (d.content || []).map(c => c.text || '').join('\n'));
    else if (e.type === 'tool/result') push('tool-result', (d.message ? d.message.content : d.content || []).map(c => c.text || '').join('\n'));
    else if (e.type === 'assistant/message') push('assistant', (d.message ? d.message.content : d.content || []).map(c => c.text || c.arguments || '').join('\n'));
    else if (e.type === 'system/message') push('system', (d.message ? d.message.content : []).map(c => c.text || '').join('\n'));
    else if (e.type === 'agent/inbox/spliced') push('splice', JSON.stringify(d));
  }
  return blocks;
}

const cats = {
  gambling: /赌场|赌博|博彩|彩票|下注|百家乐|casino|betting|sportsbook/i,
  adult: /色情|成人视频|裸体|porn|xxx|escort/i,
  drugs: /毒品|大麻|冰毒|可卡因|cocaine|methamphetamine|opioid/i,
  weapons: /枪支|弹药|爆炸物|炸药|firearm|ammunition|explosive/i,
  fraud: /诈骗|洗钱|刷单|资金盘|money laundering|ponzi/i,
  evasion: /翻墙|科学上网|梯子|机场订阅|shadowsocks|v2ray|clash|trojan/i,
  hacking: /破解|外挂|私服|脱库|sql注入|keygen|cheat engine/i,
  crypto: /虚拟货币|加密货币|usdt|bitcoin|btc|以太坊/i,
  politics: /共产党|政治|六四|法轮|台独|港独/i,
  violence: /自杀|自残|杀人|恐怖袭击|suicide|terroris/i,
  malware: /木马|勒索软件|后门|ransomware|malware|keylogger|rootkit/i,
  pii: /身份证|手机号|银行卡号|credit card number|passport number/i,
};

const targets = [
  ['session-37c61086-c798-47f0-a007-4dff74514db9', 278],
  ['session-8d9ef963-5998-4608-86f0-338e813886ae', 128],
  ['session-c9329563-33a1-4448-bc88-716562739a39', 144],
];

for (const [sid, upto] of targets) {
  const blocks = collect(sid, upto);
  const total = blocks.reduce((a, b) => a + b.s.length, 0);
  console.log(`\n### ${sid}  uptoSeq=${upto}  blocks=${blocks.length}  totalChars=${total}`);
  // biggest blocks
  console.log('   largest blocks: ' + blocks.slice().sort((a, b) => b.s.length - a.s.length).slice(0, 5)
    .map(b => `seq${b.seq}/${b.label}=${b.s.length}`).join(' '));
  for (const [name, re] of Object.entries(cats)) {
    const hits = [];
    for (const b of blocks) {
      const m = b.s.match(new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g'));
      if (m) hits.push(`seq${b.seq}/${b.label}x${m.length}(${[...new Set(m)].slice(0, 4).join('|')})`);
    }
    if (hits.length) console.log(`   HIT ${name}: ` + hits.join(' '));
  }
}
