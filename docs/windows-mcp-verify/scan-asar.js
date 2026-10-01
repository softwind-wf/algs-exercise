// 在内嵌包副本里定位 dsh-mcp-client 的实现片段,判断 env 字段是否被支持
const fs = require('fs');
const file = process.argv[2];
const buf = fs.readFileSync(file);
const text = buf.toString('utf8');

function windows(re) {
  const out = [];
  let m;
  while ((m = re.exec(text)) !== null && out.length < 12) {
    const s = Math.max(0, m.index - 400);
    const e = Math.min(text.length, m.index + 900);
    out.push({ idx: m.index, snip: text.slice(s, e) });
    re.lastIndex = m.index + 1;
  }
  return out;
}

console.log('=== 文件大小 ===', buf.length);

for (const pat of ['serverName', 'mcpServers', 'spawn(']) {
  const hits = windows(new RegExp(pat.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'));
  console.log(`\n########## "${pat}" 命中 ${hits.length} 处 ##########`);
  hits.slice(0, 4).forEach((h, i) => {
    console.log(`\n----- hit ${i + 1} @${h.idx} -----`);
    console.log(h.snip.replace(/[^\x09\x0a\x0d\x20-\x7e]/g, '.'));
  });
}
