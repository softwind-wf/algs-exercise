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
const sid = process.argv[2] || 'session-37c61086-c798-47f0-a007-4dff74514db9';
const evs = decompressAll(path.join(root, sid, 'session.v4.jsonl.zstd')).split('\n').filter(Boolean)
  .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);

const wantSeq = parseInt(process.argv[3] || '275', 10);
const ev = evs.find(e => e.seq === wantSeq);
const txt = (ev.data.message ? ev.data.message.content : ev.data.content || []).map(c => c.text || '').join('');
console.log(`seq${wantSeq} type=${ev.type} chars=${txt.length}`);

const cats = {
  'gambling/赌博博彩': /赌场|赌博|博彩|彩票|下注|百家乐|casino|betting|sportsbook|poker/i,
  'adult/色情': /色情|成人视频|裸|porn|xxx|escort|sex\s*chat/i,
  'drugs/毒品': /毒品|大麻|冰毒|可卡因|marijuana|cocaine|meth|opioid/i,
  'weapons/枪械': /枪支|弹药|爆炸物|炸药|firearm|ammunition|explosive|silencer/i,
  'fraud/诈骗洗钱': /诈骗|洗钱|刷单|资金盘|money\s*launder|scam|ponzi/i,
  'evasion/翻墙代理': /翻墙|科学上网|梯子|机场|vpn|shadowsocks|v2ray|clash|trojan/i,
  'hacking/破解外挂': /破解|外挂|私服|脱库|注入攻击|hack|crack|keygen|cheat\s*engine/i,
  'crypto/虚拟币': /虚拟货币|加密货币|usdt|bitcoin|btc|以太坊/i,
  'politics/时政': /共产党|习近平|政治|六四|法轮|台独|港独/i,
  'violence/暴力自残': /自杀|自残|杀人|恐怖|suicide|terroris/i,
  'medical/医疗药品': /处方药|堕胎|伟哥|处方|abortion|viagra/i,
  'malware/恶意代码': /木马|勒索软件|后门|ransomware|malware|keylogger|rootkit/i,
  'personal-data/隐私数据': /身份证|手机号|银行卡号|credit\s*card|ssn|passport\s*number/i,
  'credentials/凭据': /password|passwd|api[_-]?key|secret|token|密码/i,
};

let any = false;
for (const [name, re] of Object.entries(cats)) {
  const m = txt.match(new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g'));
  if (m) { any = true; const uniq = [...new Set(m.map(x => x.toLowerCase()))]; console.log(`  HIT ${name}: ${m.length} 次, 关键词=${uniq.slice(0, 8).join(',')}`); }
}
if (!any) console.log('  无常见高风险类关键词命中');

// also report whether the fetched page is the blog body vs wrapper, and dump only the wrapper header
console.log('  head120 = ' + JSON.stringify(txt.slice(0, 120)));

// find the pwsh-37 spliced job output length
const sp = evs.find(e => e.type === 'agent/inbox/spliced' && JSON.stringify(e.data).includes('pwsh-37'));
if (sp) {
  const s = JSON.stringify(sp.data);
  console.log(`  splice(pwsh-37) jsonLen=${s.length}`);
}
