# Lanhu Music Player

基于 `C:\Users\16696\Downloads\LanhuProject` 暗色音乐播放器设计稿实现的浏览器端音乐播放器。

## 运行方式

```powershell
python -m http.server 4173 --directory "d:\Code\Application\music1\web-player"
```

打开浏览器访问：

```text
http://localhost:4173/
```

## 功能

- 本地音乐导入：点击“导入音乐”或拖拽音频到专辑封面区域。
- 播放控制：播放、暂停、停止、上一首、下一首、随机播放、单曲循环。
- 进度控制：显示当前时间和总时长，支持拖动进度条定位。
- 音量控制：支持滑杆调节，也支持键盘上下键调整。
- 播放列表：支持搜索、切歌、删除、上移、下移、清空列表。
- 键盘快捷键：`Space` 播放/暂停，`←` / `→` 快退快进，`↑` / `↓` 调节音量。

## 资源引用

- `assets/album-cover.png`：来自 Lanhu 主播放页封面合成图。
- `assets/blue-texture.png`：来自 Lanhu 暗色音乐页背景纹理。
- `assets/active-tab.png`：来自 Lanhu 顶部 Tab 选中指示图。
- `assets/signal.png`、`assets/battery.png`：来自 Lanhu 状态栏图标。
