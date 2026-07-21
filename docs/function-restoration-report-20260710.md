# 功能恢复与根因报告

日期：2026-07-10

## 恢复范围

- 已恢复 WebView 入口页 `web-player/index.html` 和 `app/src/main/assets/player/index.html`。
- 已恢复前端核心逻辑 `web-player/app.js` 和 `app/src/main/assets/player/app.js` 中的乱码文案、损坏标签模板和损坏正则。
- 已确认以下功能模块的 DOM 和入口控件存在并可初始化：
  - 本地音乐：导入入口、播放控制、进度、音量、歌词预览/完整歌词、收藏按钮。
  - 蓝牙音乐：模块切换、离线态、搜索设备按钮、设备列表空态、播放控制入口。
  - 收音机：FM/AM、搜台、预设电台列表、收藏、静音、音量控制。
  - 收藏/播放列表抽屉：列表空态、搜索、清空、导入入口。
  - U 盘音乐：弹窗、状态、播放控制、扫描、文件夹列表、音量控制。

## 根因

功能消失的直接原因是 WebView 资源文件被错误编码读写后损坏：

- `index.html` 中出现大量 mojibake 和坏标签，例如 `?/button`、缺失引号、截断属性，导致浏览器无法得到完整 DOM。
- `app.js` 中出现乱码字符串和损坏正则表达式，曾触发 `SyntaxError: Invalid regular expression: missing /`，使脚本初始化中断。
- 根因链路判断为 PowerShell 5/ANSI 代码页对 UTF-8 无 BOM 文件执行读写替换，造成 UTF-8 内容被按 GBK/ANSI 解释后再次写回。

## 已执行测试

- 编码测试：`index.html`、`app.js`、`styles.css`、`MainActivity.java` 均通过严格 UTF-8 解码。
- 同步测试：`web-player` 与 `app/src/main/assets/player` 中的 `index.html`、`app.js`、`styles.css` 哈希一致。
- 乱码测试：常见损坏标记、坏标签、替换字符扫描通过。
- DOM 完整性测试：核心模块所需 ID 全部存在。
- 浏览器烟测：
  - 页面标题和本地音乐主界面正常显示。
  - 本地音乐、蓝牙音乐、收音机模块可正常切换。
  - 蓝牙设备列表空态、收音机电台列表、本地音乐播放/歌词控件正常渲染。
  - 新标签加载控制台无 `SyntaxError`、`TypeError`、`ReferenceError`。
- Android 构建测试：`./gradlew.bat assembleDebug --no-daemon` 通过，生成 `app-debug.apk`。

## 受限项

- 连接设备拒绝覆盖安装，`adb install -r`、`adb install -r -g`、`pm install -r -g` 均返回 `INSTALL_FAILED_ABORTED: User rejected permissions`。
- 因设备端拒绝安装，无法在当前设备上确认“最新 APK”的真机运行状态；已通过本地浏览器验证同一套 WebView 资源，并通过 Android 构建验证资源可打包。

## 预防措施

- 新增 `scripts/validate-webview-resources.ps1`，用于打包前校验：
  - UTF-8 严格解码。
  - 开发资源与 Android assets 哈希一致。
  - 常见 mojibake、坏标签、替换字符不存在。
  - 关键 DOM ID 完整。
- 后续修改 UTF-8 无 BOM 文件时避免使用 PowerShell 5 `Get-Content`/`Set-Content` 直接读写中文源码。
- 如需脚本化修复文本，必须使用严格 UTF-8 API，并在写回后运行校验脚本和浏览器控制台烟测。
- 建议将 `scripts/validate-webview-resources.ps1` 纳入构建前检查或 CI。
