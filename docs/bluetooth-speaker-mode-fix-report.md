# 蓝牙音乐蓝牙音箱模式修复报告

## 未生效原因

上次改动主要使用 Android `BluetoothProfile.A2DP`，这是“本机作为音频源，把声音输出到蓝牙耳机/音箱”的模式。

本次需求是“本设备作为触屏蓝牙音箱，让已连接手机通过蓝牙音乐功能播放音频，并由本设备扬声器输出”。该方向需要系统支持 A2DP Sink 接收端。

## 本次修复

- 通过 profile id `11` 接入系统 A2DP Sink 代理。
- 监听 `android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED`，同步手机接入/断开状态。
- 设备列表 `connected` 状态同时识别 A2DP Source 和 A2DP Sink。
- 新增 `audioRole` 字段：
  - `sink`：本设备作为蓝牙音箱接收手机音频。
  - `source`：本设备作为音频源输出到蓝牙耳机/音箱。
- 蓝牙音箱模式连接成功后，准备本设备扬声器输出路由：
  - `MODE_NORMAL`
  - 关闭 Bluetooth SCO
  - 开启 speakerphone 路由
  - 申请 `STREAM_MUSIC` 音频焦点
- 前端识别“蓝牙音箱接收设备已连接/已断开”事件，连接后显示“蓝牙音箱模式：本设备扬声器输出”。
- 断开设备时同时尝试断开 Sink 和原 A2DP 输出连接。

## 验证结果

| 验证项 | 结果 |
| --- | --- |
| Android Debug 构建 | 通过 |
| Web 资源同步到 Android assets | 通过 |
| A2DP Sink 反射接入编译 | 通过 |
| 设备列表 Sink 状态同步静态检查 | 通过 |
| 前端蓝牙音箱连接事件识别 | 通过 |

## APK 信息

- 路径：`app/build/outputs/apk/debug/app-debug.apk`
- 大小：`7204988`
- 生成时间：`2026-07-02 20:06:01`

## 真机验证步骤

1. 在本设备打开“三一音乐”并进入蓝牙音乐模块。
2. 确认系统蓝牙已开启，并在手机端蓝牙列表中连接本设备。
3. 手机端播放音乐。
4. 预期本设备蓝牙音乐界面切换为连接态，并通过本设备扬声器输出手机音频。
5. 在本设备触屏界面点击播放/暂停、调整音量，确认手机媒体会话和本设备输出音量响应正常。

## 兼容性边界

A2DP Sink 是系统能力。若当前设备 ROM 未启用 A2DP Sink profile，普通第三方应用无法直接接收手机推送的蓝牙音频流。本次已接入系统 Sink profile；实际出声仍取决于设备系统蓝牙栈是否开放并启用该 profile。

