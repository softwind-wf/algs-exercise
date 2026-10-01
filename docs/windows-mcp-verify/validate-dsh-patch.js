// 用 DSH profile 自带的 js-yaml 严格解析两个 cordis.patch.yml,校验 mcp-windows 条目
const fs = require('fs');
const path = require('path');
const yaml = require('C:/Users/wf/.dsh/profiles/node_modules/js-yaml');

const HOME = process.env.USERPROFILE;
let allOk = true;

for (const prof of ['desktop', 'web']) {
  const file = path.join(HOME, '.dsh', 'profiles', prof, 'cordis.patch.yml');
  console.log(`\n########## ${prof} ##########`);
  console.log(`file: ${file}`);
  let doc;
  try {
    doc = yaml.load(fs.readFileSync(file, 'utf8'));
    console.log('YAML 解析: OK');
  } catch (e) {
    allOk = false;
    console.log('YAML 解析: FAILED ->', e.message);
    continue;
  }
  console.log('顶层类型:', Array.isArray(doc) ? `array(${doc.length})` : typeof doc);
  if (!Array.isArray(doc)) { allOk = false; console.log('顶层不是数组,DSH 期望数组'); continue; }

  // 收集所有条目 id
  const ids = [];
  let found = null;
  for (const entry of doc) {
    if (entry && entry.id) ids.push(entry.id);
    if (entry && Array.isArray(entry.insert)) {
      for (const item of entry.insert) {
        if (item && item.id) ids.push(`(insert)${item.id}`);
        if (item && item.id === 'mcp-windows') found = item;
      }
    }
  }
  console.log('条目 id:', ids.join(', '));

  if (!found) { allOk = false; console.log('未找到 mcp-windows 条目!'); continue; }

  const c = found.config || {};
  const checks = [
    ['name', found.name, '@deepseek-ai/dsh-mcp-client'],
    ['serverName', c.serverName, 'windows'],
    ['transport', c.transport, 'stdio'],
    ['command', c.command, 'C:/Users/wf/.windows-mcp-tools/bin/windows-mcp.exe'],
  ];
  for (const [k, got, want] of checks) {
    const ok = got === want;
    if (!ok) allOk = false;
    console.log(`  ${ok ? 'OK  ' : 'FAIL'} ${k} = ${JSON.stringify(got)}`);
  }
  console.log('  args =', JSON.stringify(c.args));
  console.log('  env  =', JSON.stringify(c.env, null, 2));
  const cmdExists = fs.existsSync(c.command.replace(/\//g, path.sep));
  console.log(`  command 文件存在: ${cmdExists}`);
  if (!cmdExists) allOk = false;

  // serverName 唯一性(同名会报 duplicate normalized name)
  const dup = ids.filter((x) => x === '(insert)mcp-windows' || x === '(insert)mcp-playwright');
  console.log(`  insert 条目: ${dup.join(', ')}`);
}

console.log(`\n===== 总判定: ${allOk ? 'PASS' : 'FAIL'} =====`);
process.exit(allOk ? 0 : 1);
