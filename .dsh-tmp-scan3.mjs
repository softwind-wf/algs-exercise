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
const sid = 'session-37c61086-c798-47f0-a007-4dff74514db9';
const evs = decompressAll(path.join(root, sid, 'session.v4.jsonl.zstd')).split('\n').filter(Boolean)
  .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);

function textOf(seq) {
  const e = evs.find(x => x.seq === seq);
  return (e.data.message ? e.data.message.content : e.data.content || []).map(c => c.text || '').join('');
}

const sets = {
  'Trad 赌博': /賭場|賭博|博彩|彩票|下注|百家樂|簽賭/i,
  'Trad 色情': /色情|成人影片|成人視頻|裸體|寫真|約炮|性服務/i,
  'Trad 毒品': /毒品|大麻|安非他命|古柯鹼|搖頭丸/i,
  'Trad 枪械': /槍枝|槍械|彈藥|炸藥|爆裂物/i,
  'Trad 诈骗洗钱': /詐騙|洗錢|車手|人頭帳戶|吸金/i,
  'Trad 翻墙代理': /翻牆|科學上網|梯子|代理伺服器|節點訂閱/i,
  'Trad 政治': /中共|習近平|維吾爾|新疆|西藏|香港|台灣|領導人|法輪|六四|台獨|港獨|獨立/i,
  'Trad 暴力': /自殺|自殘|殺人|恐怖攻擊|槍擊/i,
  'Trad 恶意代码': /木馬|勒索軟體|後門|惡意程式/i,
  'Trad 个资': /身分證|手機號|銀行卡號|信用卡號/i,
  'Trad 医疗': /處方藥|墮胎|威而鋼|避孕/i,
  'Trad 赌博/游戏': /賭|博弈|娛樂城|老虎機/i,
};

const targets = [[275, 'web_fetch 博客正文'], [265, 'web_search 结果'], [278, 'pwsh-37 补丁']];
for (const [seq, label] of targets) {
  let txt;
  if (seq === 278) { const e = evs.find(x => x.type === 'agent/inbox/spliced' && JSON.stringify(x.data).includes('pwsh-37')); txt = JSON.stringify(e.data); }
  else txt = textOf(seq);
  console.log(`\n-- seq${seq} ${label} chars=${txt ? txt.length : 0}`);
  if (!txt) continue;
  let hit = false;
  for (const [name, re] of Object.entries(sets)) {
    const m = txt.match(new RegExp(re.source, 'gi'));
    if (m) { hit = true; console.log(`   HIT ${name}: ${[...new Set(m)].join('|')}`); }
  }
  if (!hit) console.log('   无命中');
  // 段落结构概览：每行前 24 字，仅在无命中时用于判断页面主题
  if (!hit && process.argv[2] === 'show') {
    const lines = txt.split('\n').filter(l => l.trim().length > 2);
    console.log('   结构: 共 ' + lines.length + ' 行');
    for (const l of lines.slice(0, 40)) console.log('     | ' + l.slice(0, 24));
  }
}
