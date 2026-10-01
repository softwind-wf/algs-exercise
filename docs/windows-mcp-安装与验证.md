# windows-mcp 安装与验证报告(用户级全局安装)

- 日期:2026-10-01
- 安装位置:**`C:\Users\wf\.windows-mcp-tools\`**(与项目无关,全局可用)
- 启动方式:**`C:\Users\wf\.windows-mcp-tools\windows-mcp.cmd serve`**
- 结论:安装成功,可启动、可握手、工具可实际执行;工作区内的旧安装(513 MB)已删除

## 1. 为什么最终装在用户级而不是项目目录

第一次装在工作区 `D:\downloads\algs4-master\algs4-master\.windows-mcp-verify\` 是为了绕开 DSH 文件沙箱,
代价是**换项目就不可用**。已改为用户级全局安装。

沙箱的实际边界(实测):

| 位置 | 新建 | 写入 |
| --- | --- | --- |
| 工作区根目录及其下新建对象 | 可 | 可 |
| `C:\Users\wf\.windows-mcp-tools`(工作区外) | 仅提权后可 | 仅提权后可 |
| `~\.local\bin`、`C:\Tools`、`%APPDATA%\uv`、`%LOCALAPPDATA%\uv` | 不可 | 不可 |
| `~\.dsh\profiles\<档>\cordis.patch.yml` | 不可 | 不可 |

原因:DSH 沙箱只给**工作区根**打低完整性(Low)标签,只有其下**新建**的对象才继承该标签;
工作区外的一切(不论新建还是既有)都被拒。DACL 修复对此无效(限制在 SACL/MIC,不在 DACL)。

> 教训:`New-Item -ItemType Directory -Force` **不能**当作可写性证据 —— 目录已存在时它会静默成功。
> 必须紧接着做一次 `Set-Content` 才算数。

## 2. 版本事实

| 项 | 值 |
| --- | --- |
| PyPI 发布版本 | **0.8.7** |
| `requires_python` | **`>= 3.14`**(0.7.4–0.8.2 = `>=3.13`;0.8.5 = `>=3.12`;0.8.6 起 = `>=3.14`) |
| 本机系统 Python | 3.12.1(**不满足**) |
| 实际使用解释器 | 受管 **CPython 3.14.7**(uv 安装,在 `.windows-mcp-tools\python\`) |
| server 自报版本 | **4.0.10**(与发布版本号不一致) |
| CLI | Click,`--version` 不是合法选项;子命令为 `serve` |

## 3. 安装方式与布局

```
C:\Users\wf\.windows-mcp-tools\
├── windows-mcp.cmd        <- 启动器(推荐入口,已修正为 CRLF)
├── bin\windows-mcp.exe    <- uv tool 生成的 shim
├── tools\windows-mcp\     <- 持久化 venv(windows-mcp 0.8.7 + 92 个依赖)
├── python\                <- 受管 CPython 3.14.7
└── cache\                 <- uv 缓存
总计 357 MB
```

安装命令(在普通终端里执行即可,不需要沙箱提权):

```powershell
$root = "$env:USERPROFILE\.windows-mcp-tools"
$env:UV_CACHE_DIR="$root\cache"; $env:UV_PYTHON_INSTALL_DIR="$root\python"
$env:UV_TOOL_DIR="$root\tools";  $env:UV_TOOL_BIN_DIR="$root\bin"
$env:UV_PYTHON_PREFERENCE='only-managed'
uv python install 3.14
uv tool install windows-mcp
```

## 4. 关键坑:comtypes 代码缓存导致启动崩溃

全局安装后首次启动直接崩:

```
File "comtypes\client\_code_cache.py", line 80, in _find_gen_dir
    os.makedirs(gen_dir)
PermissionError: [WinError 5] 拒绝访问: 'C:\Users\wf\AppData\Roaming\Python'
```

`comtypes` 启动时按顺序找可写的代码缓存目录:

1. `site-packages\comtypes\gen` —— 目录为空、不可写 → 弃用
2. 回落到 `%APPDATA%\Python\Python314\comtypes_cache` —— 沙箱不可写 → 崩溃

`comtypes` **没有**可用环境变量指定该目录(源码里不存在),所以采用的修法是两条:

- 在 venv 的 `site-packages` 下写一个 `.pth`,让 `comtypes.gen` 提前导入,
  使 `_find_gen_dir()` 直接走第一条路:
  `_dsh_comtypes_gen_fix.pth` → `import comtypes.gen as _dsh_comtypes_gen`
- 在提权会话里**预生成** COM 包装模块到 `site-packages\comtypes\gen`
  (`_00020430_...py`、`_944DE083_...py`、`stdole.py`、`UIAutomationClient.py`,共 794 KB)。
  预生成后普通(无提权)运行只需**读**这些缓存,不再需要写权限。

排查时的一个对照证据:项目内那份安装之所以能跑起来,是因为它的
`site-packages\comtypes\gen` 里同样已经存在这些预生成模块。

> 另一个坑:`.cmd` 启动器必须用 **CRLF** 换行。LF 换行的 `.cmd` 在 `set` 块后会被 cmd.exe 错误解析
> (实测出现 `'s-mcp' 不是内部或外部命令` 之类的乱码报错)。

## 5. 验证结果

### 5.1 启动器(清空全部 UV_* 环境变量,工作目录在工作区之外)

```
launcher exists = True  (C:\Users\wf\.windows-mcp-tools\windows-mcp.cmd)
当前进程 UV_* 环境变量(应为空) = 0
stderr -> [10/01/26 12:27:18] INFO  Starting MCP server 'windows-mcp' with transport 'stdio'
initialize -> OK windows-mcp v4.0.10
tools/list -> OK 20 个工具
alive=True
```

### 5.2 工具实际执行

```
tools/call DisplayInventory -> isError=False
  [{"index":0,"device":"\\\\.\\DISPLAY1","primary":true,
    "bounds":{"left":0,"top":0,"right":1920,"bottom":1080},
    "work_area":{"left":0,"top":0,"right":1920,"bottom":1020},
    "resolution":"1920x1080","orientation":"landscape","effective_dpi":120,"scale":1.25}]
```

(cwd = `%TEMP%`,即工作区之外,证明与项目无关)

### 5.3 20 个工具

```
App, DisplayInventory, PowerShell, FileSystem, Snapshot, Screenshot, Click, Type, Scroll, Move,
Shortcut, Wait, WaitFor, Scrape, MultiSelect, MultiEdit, Clipboard, Process, Notification, Registry
```

## 6. MCP 客户端配置片段

Claude Desktop / Gemini CLI / Codex CLI / Claude Code 通用(stdio):

```json
{
  "mcpServers": {
    "windows-mcp": {
      "command": "C:\\Users\\wf\\.windows-mcp-tools\\bin\\windows-mcp.exe",
      "args": ["serve"]
    }
  }
}
```

或走启动器(自带 uv 环境变量,更省心):

```json
{
  "mcpServers": {
    "windows-mcp": {
      "command": "C:\\Users\\wf\\.windows-mcp-tools\\windows-mcp.cmd",
      "args": ["serve"]
    }
  }
}
```

Codex CLI(`%USERPROFILE%\.codex\config.toml`):

```toml
[mcp_servers.windows-mcp]
command = 'C:\Users\wf\.windows-mcp-tools\bin\windows-mcp.exe'
args = ["serve"]
```

Claude Code:

```powershell
claude mcp add --transport stdio windows-mcp -- C:\Users\wf\.windows-mcp-tools\bin\windows-mcp.exe serve
```

## 7. 已接入 DSH(2026-10-01 完成)

在 `~\.dsh\profiles\desktop\cordis.patch.yml` 与 `~\.dsh\profiles\web\cordis.patch.yml`
两档各追加了一条 `insert`(原文件已备份为 `cordis.patch.yml.bak-windows-mcp`):

```yaml
- insert:
    - id: mcp-windows
      name: '@deepseek-ai/dsh-mcp-client'
      config:
        serverName: windows
        transport: stdio
        command: C:/Users/wf/.windows-mcp-tools/bin/windows-mcp.exe
        args:
          - serve
        env:
          UV_CACHE_DIR: C:/Users/wf/.windows-mcp-tools/cache
          UV_PYTHON_INSTALL_DIR: C:/Users/wf/.windows-mcp-tools/python
          UV_TOOL_DIR: C:/Users/wf/.windows-mcp-tools/tools
          UV_TOOL_BIN_DIR: C:/Users/wf/.windows-mcp-tools/bin
          UV_PYTHON_PREFERENCE: only-managed
          ANONYMIZED_TELEMETRY: 'false'
```

验证证据:

- js-yaml 严格解析两个档:**PASS**;`serverName=windows`、`transport=stdio`、
  `command` 文件存在;与既有的 `mcp-playwright` 无 id / serverName 冲突。
- **热加载生效**:写入后本会话的 MCP 服务列表即出现 `windows`,
  且 20 个 `mcp__windows__*` 工具 schema 被注入当前会话。
- **实际调用成功**:`mcp__windows__DisplayInventory` 返回
  `DISPLAY1 1920x1080 / work_area 1920x1020 / effective_dpi 120 / scale 1.25`。

注:同一份 MCP 客户端配置在两个档都写了,保证无论 DSH 以哪个档启动都能用(配置幂等,
只有实际运行的那个档会加载)。改完无需重启 GUI:插件热加载已使工具可用;
若某次写入后未生效,重启 DSH 即可。

## 8. 未做 / 待办

- **未装开机自启**:`windows-mcp install` 需要写注册表与 `~/.windows-mcp/`,沙箱拒绝;
  要自启请在普通终端手动执行一次。
- `windows-mcp.exe` 依赖预生成的 comtypes 缓存;若日后升级版本导致 `comtypes\gen` 被清空,
  需重跑一次预生成(见 `pregenerate-comtypes.ps1`)。

## 9. 可复现脚本(位于 `docs\windows-mcp-verify\`)

- `setup-user-level.ps1` — 用户级安装
- `pregenerate-comtypes.ps1` — 预生成 comtypes 包装模块(修复启动崩溃)
- `fix-comtypes.ps1` — 写 `.pth` 并确保 `comtypes\gen` 存在
- `verify-launcher.ps1` — 清空全部环境变量,验证启动器可用(`initialize` + `tools/list`)
- `integrate-into-dsh.ps1` — 向两个 profile 的 `cordis.patch.yml` 幂等追加 `mcp-windows`(带备份)
- `validate-dsh-patch.js` — 用 js-yaml 严格校验两个 profile 的 patch 与条目字段
