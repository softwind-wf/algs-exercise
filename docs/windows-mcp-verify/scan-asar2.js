// 挖 DSH 的 MCP 配置来源:工作区级文件?插件配置?以及 McpClient 的字段 schema
const fs = require('fs');
const buf = fs.readFileSync(process.argv[2]);
const text = buf.toString('utf8');

function ctx(pattern, before, after, max) {
  const out = [];
  const re = new RegExp(pattern, 'g');
  let m;
  while ((m = re.exec(text)) !== null && out.length < (max || 6)) {
    out.push({ idx: m.index, snip: text.slice(Math.max(0, m.index - before), Math.min(text.length, m.index + after)) });
    re.lastIndex = m.index + 1;
  }
  return out;
}
const clean = (s) => s.replace(/[^\x09\x0a\x0d\x20-\x7e]/g, '.');

const probes = [
  ['mcp\\.json', 500, 700],
  ['dsh-mcp-client', 300, 800],
  ['serverName:\\s*Config', 200, 900],
  ['resolveMcpConfigs\\(', 900, 500],
  ['\\.dsh[/\\\\]', 300, 300],
  ['mcpServers', 300, 500],
];

for (const [pat, b, a] of probes) {
  const hits = ctx(pat, b, a);
  console.log(`\n########## /${pat}/ -> ${hits.length} 处 ##########`);
  hits.slice(0, 3).forEach((h, i) => {
    console.log(`\n----- @${h.idx} -----`);
    console.log(clean(h.snip));
  });
}
