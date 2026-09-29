# DSH 反馈:Windows 沙箱不会给"既有内容"打 Low 完整性标签,导致受限写入被拒

- 建议分类:`security-privacy-permission`(若只能选产品交互类,则 `product-interaction`)
- 组件:`@deepseek-ai/dsh-sandbox-windows-acl`(内置于桌面版 app.asar)
- 环境:DSH 桌面版 0.2.0-rc.2 / Windows 11 amd64 / 工作区在 D 盘 NTFS

## 现象

工作区在 `workspace-write` 模式下,受限命令(PowerShell/bash)写入**工作区内**的既有目录被拒:

```
PS> New-Item -ItemType Directory -Force -Path target\acl-verify
New-Item: Access to the path '...\target\acl-verify' is denied.
```

但同一目录的权限看起来完全正常:owner 是当前用户、DACL 里有 `Modify`/`FullControl`、也不是只读。完全权限模式下同样的操作成功,所以问题只在受限令牌这一侧。

## 根因(已定位到实现位置)

`@deepseek-ai/dsh-sandbox-windows-acl/lib/types-Cl_DXjhk.js:592 grantWrite()` 只对**授权根**(工作区根目录,或会话私有临时目录)调用一次 `SetNamedSecurityInfoW`,在同一次调用里写入三件东西:能力 SID 允许 ACE、对 world 的 `FILE_DELETE_CHILD` 拒绝、以及 **Low(NW)强制完整性标签**。README.md:98 / README.zh.md:100 说明了约束:受限令牌被降为 Low 完整性,"内核的强制完整性检查会拒绝对任何未标记为 Low 的对象进行写类访问"。

问题在于标签的传播方式:Windows 的强制完整性标签是**在对象创建时从直接父目录继承**的,给父目录打标签**不会**回溯到已经存在的子对象。于是:

| 对象 | 标签 |
|---|---|
| 工作区根(供给时打过) | `Mandatory Label\Low Mandatory Level:(OI)(CI)(NW)` |
| 根下新建的目录/文件(如 `out/`) | `Low ...(I)(OI)(CI)(NW)` / `Low ...(I)(NW)` |
| **授权前就存在的目录**(`src/`、`docs/`、`target/`) | **完全没有标签** |
| **在旧目录里新建的文件** | **也没有标签**(继承源是直接父目录) |

DACL 侧完全正常:以上对象都继承了能力 SID `S-1-4-*` 的允许 ACE,普通 SID 也有 Modify——所以**只有内核 MIC 这一道检查在拒**,补 DACL 无论如何都修不好。

代码注释本身假设了"这次 SetNamedSecurityInfoW 会把同一描述符急切传播到整棵树"(`lib/types-Cl_DXjhk.js:578-582`,对应 README.zh.md:189),并且靠 `hasExactGrant/hasExactDeny/hasExactLabel` 的精确匹配**跳过后续供给**。实测该假设对"授权之前就已存在的内容"不成立,而且由于跳过逻辑存在,**重新供给也不会修复**。

## 最小复现

1. 准备一个内容早于 DSH 供给而存在的工作区(例如先 clone/复制好项目,再首次在 DSH 里打开;或像本例这样,机器换了账户、工作区沿用了旧账户时代创建的文件)。
2. 在 `workspace-write` 模式下执行:`New-Item -ItemType Directory -Force -Path <工作区>\<既有目录>\x` → Access denied。
3. `icacls <既有目录>` 没有任何 `Mandatory Label` 行;而 `icacls <工作区根>` 显示 `Low Mandatory Level:(OI)(CI)(NW)`。
4. 一次性修复(管理员):`icacls "<工作区>" /setintegritylevel "(OI)(CI)L" /T` → 之后同样的受限写入全部成功。

第 4 步是否执行,用 P/Invoke 读 `LABEL_SECURITY_INFORMATION` 的 SACL 做全树穷举可以量化:修复前既有目录无标签;修复后 **7993 个对象 LOW=7993、NO_LABEL=0、MEDIUM=0**,随后在 `workspace-write` 下 `mvn -o -q compile`、`mvn -o -q test-compile`、`mvn -q test -Dtest=LeftistHeapTest` 全部 exit 0。

## 为什么现有诊断技能接不住

`assets/diagnose-windows-sandbox-acl/SKILL.md` 的脚本只处理 DACL:脚本第 190 行的注释明确写着 "Only the DACL and its inheritance protection are written: never owner, group or SACL",而标签在 SACL 里。本例中脚本能正确报出根目录缺 `WRITE_OWNER` 并修复(这是供给成功的前置条件),但修复后受限写入依旧失败——因为真正挡路的标签不在它的写范围内。

## 建议修复方向(任选其一或组合)

1. **供给时把标签应用到既有树**:`grantWrite` 除了授权根,还对既有后代对象应用 Low 标签(或确保自动继承传播确实发生),使工作区内所有对象一致。
2. **让跳过逻辑校验后代**:精确匹配跳过之前,顺带确认既有后代已带标签;不一致时重新物化,而不是永久跳过。
3. **诊断技能补一条判定**:当"根有标签、后代无标签"时明确报出该状态,并给出可执行的修复指引(需要提权的 `icacls` 一行),而不是只报 DACL 结论。
4. **至少在文档里声明**:内容早于供给存在的工作区,需要一次性递归补标签。

## 影响面

任何"工作区内容早于当前供给身份"的 Windows 用户都会踩到:换过 Windows 账户、从备份恢复过工作区、在 DSH 之外 clone/解压过项目、或把项目目录整体搬到新机器后沿用。表现是受限模式下的 `mvn`、`git`、formatter、脚本写入通通 Access denied,而文件权限看起来毫无问题,用户只能切完全权限绕开——等于把沙箱整个关掉。诊断成本很高:需要区分 DACL 与完整性标签两层,现有技能输出会把人引向 DACL。
