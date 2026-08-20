/**
 * 音乐 国际化(i18n)模块
 * 支持语言：zh-CN（简体中文，默认）、zh-TW（繁体中文）、en（英文）
 *
 * 设计原则：
 *   - 自动跟随系统语言：通过 navigator.language 检测系统区域设置
 *   - 默认中文简体：无法识别时回退至 zh-CN
 *   - 无需手动切换入口：语言随系统变化自动更新
 *
 * 使用方式：
 *   HTML：<span data-i18n="key">默认文本</span>
 *   JS：  i18n.t('key') 返回翻译后的字符串
 *        i18n.t('key', {n: 5}) 支持参数插值
 */
(function(global) {
  'use strict';

  // 支持的语言代码
  var SUPPORTED_LANGUAGES = ['zh-CN', 'zh-TW', 'en'];
  var DEFAULT_LANGUAGE = 'zh-CN';
  var currentLanguage = DEFAULT_LANGUAGE;

  /**
   * 系统语言 → 应用语言映射
   * navigator.language 返回值如 "zh-CN", "zh-TW", "zh-HK", "en-US", "en" 等
   */
  function detectLanguage() {
    try {
      var navLang = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
      if (navLang.indexOf('zh') === 0) {
        // 中文分支：zh-cn/zh-sg → 简体；zh-tw/zh-hk/zh-mo → 繁体
        if (navLang.indexOf('tw') !== -1 || navLang.indexOf('hk') !== -1 || navLang.indexOf('mo') !== -1) {
          return 'zh-TW';
        }
        return 'zh-CN';
      }
      if (navLang.indexOf('en') === 0) {
        return 'en';
      }
      // 其他语言一律回退英文
      return 'en';
    } catch (e) {
      return DEFAULT_LANGUAGE;
    }
  }

  // ==================== 翻译字典 ====================

  var translations = {
    // ---------- 通用 ----------
    'common.ok': { 'zh-CN': '确定', 'zh-TW': '確定', 'en': 'OK' },
    'common.cancel': { 'zh-CN': '取消', 'zh-TW': '取消', 'en': 'Cancel' },
    'common.close': { 'zh-CN': '关闭', 'zh-TW': '關閉', 'en': 'Close' },
    'common.loading': { 'zh-CN': '加载中', 'zh-TW': '載入中', 'en': 'Loading' },
    'common.error': { 'zh-CN': '错误', 'zh-TW': '錯誤', 'en': 'Error' },

    // ---------- 应用 ----------
    'app.title': { 'zh-CN': '音乐', 'zh-TW': '音樂', 'en': 'Music' },
    'app.name': { 'zh-CN': '音乐', 'zh-TW': '音樂', 'en': 'Music' },

    // ---------- Tab导航 ----------
    'tab.bluetooth': { 'zh-CN': '蓝牙音乐', 'zh-TW': '藍牙音樂', 'en': 'Bluetooth' },
    'tab.radio': { 'zh-CN': '收音机', 'zh-TW': '收音機', 'en': 'Radio' },
    'tab.usb': { 'zh-CN': 'U盘音乐', 'zh-TW': '隨身碟音樂', 'en': 'USB Music' },

    // ---------- USB模块 ----------
    'usb.albumArt': { 'zh-CN': '专辑封面', 'zh-TW': '專輯封面', 'en': 'Album Cover' },
    'usb.progress': { 'zh-CN': 'U盘播放进度', 'zh-TW': '隨身碟播放進度', 'en': 'USB Playback Progress' },
    'usb.favoriteCurrent': { 'zh-CN': '收藏当前曲目', 'zh-TW': '收藏目前曲目', 'en': 'Favorite Current Track' },
    'usb.prevTrack': { 'zh-CN': '上一首', 'zh-TW': '上一首', 'en': 'Previous' },
    'usb.nextTrack': { 'zh-CN': '下一首', 'zh-TW': '下一首', 'en': 'Next' },
    'usb.play': { 'zh-CN': '播放', 'zh-TW': '播放', 'en': 'Play' },
    'usb.pause': { 'zh-CN': '暂停', 'zh-TW': '暫停', 'en': 'Pause' },
    'usb.playOrPause': { 'zh-CN': '播放/暂停', 'zh-TW': '播放/暫停', 'en': 'Play/Pause' },
    'usb.stop': { 'zh-CN': '停止', 'zh-TW': '停止', 'en': 'Stop' },
    'usb.playbackControl': { 'zh-CN': 'U盘音乐播放控制', 'zh-TW': '隨身碟音樂播放控制', 'en': 'USB Music Controls' },
    'usb.deviceNotConnected': { 'zh-CN': 'USB设备未连接', 'zh-TW': 'USB裝置未連接', 'en': 'USB device not connected' },
    'usb.scanning': { 'zh-CN': '正在扫描USB音乐...', 'zh-TW': '正在掃描USB音樂...', 'en': 'Scanning USB music...' },
    'usb.scanComplete': { 'zh-CN': '扫描完成，发现{n}首歌曲', 'zh-TW': '掃描完成，發現{n}首歌曲', 'en': 'Scan complete, {n} tracks found' },
    'usb.noMusicFiles': { 'zh-CN': 'USB设备中没有音乐文件', 'zh-TW': 'USB裝置中沒有音樂檔案', 'en': 'No music files on USB device' },
    'usb.scanError': { 'zh-CN': 'USB扫描失败', 'zh-TW': 'USB掃描失敗', 'en': 'USB scan failed' },
    'usb.disconnected': { 'zh-CN': 'USB设备已断开', 'zh-TW': 'USB裝置已斷開', 'en': 'USB device disconnected' },
    'usb.disconnectedStop': { 'zh-CN': 'USB设备已断开，音乐播放已停止', 'zh-TW': 'USB裝置已斷開，音樂播放已停止', 'en': 'USB device disconnected, playback stopped' },
    'usb.readingMeta': { 'zh-CN': '正在读取音乐信息...', 'zh-TW': '正在讀取音樂資訊...', 'en': 'Reading music info...' },
    'usb.devicesFound': { 'zh-CN': '发现{n}个USB设备', 'zh-TW': '發現{n}個USB裝置', 'en': '{n} USB device(s) found' },
    'usb.readingDevices': { 'zh-CN': '正在读取USB设备...', 'zh-TW': '正在讀取USB裝置...', 'en': 'Reading USB devices...' },
    'usb.deviceSummary': { 'zh-CN': '识别到{n}个USB设备，共{m}首歌曲', 'zh-TW': '識別到{n}個USB裝置，共{m}首歌曲', 'en': '{n} USB devices, {m} tracks total' },
    'usb.unknownSong': { 'zh-CN': '未知歌曲', 'zh-TW': '未知歌曲', 'en': 'Unknown Song' },

    // ---------- 蓝牙模块 ----------
    'bluetooth.notConnected': { 'zh-CN': '蓝牙音频尚未连接', 'zh-TW': '藍牙音樂尚未連接', 'en': 'Bluetooth audio not connected' },
    'bluetooth.search': { 'zh-CN': '搜索蓝牙', 'zh-TW': '搜尋藍牙', 'en': 'Search Bluetooth' },
    'bluetooth.searchDevices': { 'zh-CN': '搜索设备', 'zh-TW': '搜尋裝置', 'en': 'Search Devices' },
    'bluetooth.openButton': { 'zh-CN': '打开蓝牙设置', 'zh-TW': '開啟藍牙設定', 'en': 'Open Bluetooth Settings' },
    'bluetooth.deviceConnected': { 'zh-CN': '已连接，播放手机或蓝牙设备上的音乐', 'zh-TW': '已連接，播放手機或藍牙裝置上的音樂', 'en': 'Connected, playing music from phone or Bluetooth device' },
    'bluetooth.waitingConnection': { 'zh-CN': '等待手机连接并播放音频', 'zh-TW': '等待手機連接並播放音訊', 'en': 'Waiting for device to connect and play audio' },
    'bluetooth.audio': { 'zh-CN': '蓝牙音频', 'zh-TW': '藍牙音樂', 'en': 'Bluetooth Audio' },
    'bluetooth.disconnect': { 'zh-CN': '断开连接', 'zh-TW': '斷開連接', 'en': 'Disconnect' },
    'bluetooth.availableDevices': { 'zh-CN': '可用设备', 'zh-TW': '可用裝置', 'en': 'Available Devices' },
    'bluetooth.connected': { 'zh-CN': '已连接', 'zh-TW': '已連接', 'en': 'Connected' },
    'bluetooth.disconnected': { 'zh-CN': '已断开', 'zh-TW': '已斷開', 'en': 'Disconnected' },
    'bluetooth.connecting': { 'zh-CN': '连接中', 'zh-TW': '連接中', 'en': 'Connecting' },
    'bluetooth.connectionFailed': { 'zh-CN': '连接失败', 'zh-TW': '連接失敗', 'en': 'Connection failed' },
    'bluetooth.reconnectFailed': { 'zh-CN': '自动重连失败，请手动重试', 'zh-TW': '自動重連失敗，請手動重試', 'en': 'Auto-reconnect failed, please retry manually' },
    'bluetooth.deviceAddressInvalid': { 'zh-CN': '设备地址无效', 'zh-TW': '裝置位址無效', 'en': 'Invalid device address' },
    'bluetooth.notEnabled': { 'zh-CN': '蓝牙未开启，请先在系统设置中开启蓝牙', 'zh-TW': '藍牙未開啟，請先在系統設定中開啟藍牙', 'en': 'Bluetooth not enabled. Please enable in system settings' },
    'bluetooth.noConnectPermission': { 'zh-CN': '缺少蓝牙连接权限', 'zh-TW': '缺少藍牙連接權限', 'en': 'Missing Bluetooth connection permission' },
    'bluetooth.statusCleared': { 'zh-CN': '错误状态已清除', 'zh-TW': '錯誤狀態已清除', 'en': 'Error state cleared' },
    'bluetooth.reconnectCancelled': { 'zh-CN': '已取消自动重连', 'zh-TW': '已取消自動重連', 'en': 'Auto-reconnect cancelled' },
    'bluetooth.statusDetailFailed': { 'zh-CN': '状态详情获取失败', 'zh-TW': '狀態詳細資訊取得失敗', 'en': 'Failed to get status details' },
    'bluetooth.deviceRefInvalid': { 'zh-CN': '设备引用已失效', 'zh-TW': '裝置引用已失效', 'en': 'Device reference invalid' },
    'bluetooth.rebootApp': { 'zh-CN': '请重新启动应用或重置蓝牙', 'zh-TW': '請重新啟動應用或重置藍牙', 'en': 'Please restart app or reset Bluetooth' },
    'bluetooth.audioDisconnected': { 'zh-CN': '蓝牙音频设备已断开', 'zh-TW': '藍牙音樂裝置已斷開', 'en': 'Bluetooth audio device disconnected' },
    'bluetooth.audioLinkDisconnected': { 'zh-CN': '音频链路断开', 'zh-TW': '音訊鏈路斷開', 'en': 'Audio link disconnected' },
    'bluetooth.statusCheckDisconnected': { 'zh-CN': '连接状态检查发现断开', 'zh-TW': '連接狀態檢查發現斷開', 'en': 'Connection status check found disconnect' },
    'bluetooth.retryHint': { 'zh-CN': '请靠近蓝牙设备后重试连接', 'zh-TW': '請靠近藍牙裝置後重試連接', 'en': 'Please move closer to the device and retry' },
    'bluetooth.grantPermission': { 'zh-CN': '请在系统设置中授予蓝牙连接权限，然后重新打开应用', 'zh-TW': '請在系統設定中授予藍牙連接權限，然後重新開啟應用', 'en': 'Please grant Bluetooth permission in system settings, then reopen the app' },
    'bluetooth.confirmBluetooth': { 'zh-CN': '请确认设备已开启蓝牙且在有效范围内，然后重试连接', 'zh-TW': '請確認裝置已開啟藍牙且在有效範圍內，然後重試連接', 'en': 'Please confirm Bluetooth is enabled and device is in range, then retry' },
    'bluetooth.completePairing': { 'zh-CN': '请在系统蓝牙设置中完成设备配对，然后返回应用重试', 'zh-TW': '請在系統藍牙設定中完成裝置配對，然後返回應用重試', 'en': 'Please complete pairing in system Bluetooth settings, then return to retry' },
    'bluetooth.enableFirst': { 'zh-CN': '请先开启手机蓝牙并确保设备已配对', 'zh-TW': '請先開啟手機藍牙並確保裝置已配對', 'en': 'Please enable Bluetooth and ensure device is paired first' },
    'bluetooth.reconnectHint': { 'zh-CN': '蓝牙连接已断开，系统将尝试自动重连，若多次失败请手动重新连接', 'zh-TW': '藍牙連接已斷開，系統將嘗試自動重連，若多次失敗請手動重新連接', 'en': 'Bluetooth disconnected, auto-reconnecting. Please manually reconnect if it fails multiple times' },
    'bluetooth.restartApp': { 'zh-CN': '请重启应用以重新初始化蓝牙设备引用', 'zh-TW': '請重啟應用以重新初始化藍牙裝置引用', 'en': 'Please restart app to reinitialize Bluetooth device reference' },
    'bluetooth.unknownCmd': { 'zh-CN': '未知蓝牙媒体控制指令', 'zh-TW': '未知藍牙媒體控制指令', 'en': 'Unknown Bluetooth media control command' },
    'bluetooth.sentCmd': { 'zh-CN': '已发送蓝牙媒体控制指令：{cmd}', 'zh-TW': '已發送藍牙媒體控制指令：{cmd}', 'en': 'Bluetooth media command sent: {cmd}' },
    'bluetooth.statusFailed': { 'zh-CN': '状态生成失败', 'zh-TW': '狀態生成失敗', 'en': 'Status generation failed' },
    'bluetooth.album': { 'zh-CN': '蓝牙音乐封面', 'zh-TW': '藍牙音樂封面', 'en': 'Bluetooth Album Cover' },
    'bluetooth.progress': { 'zh-CN': '蓝牙播放进度', 'zh-TW': '藍牙播放進度', 'en': 'Bluetooth Playback Progress' },
    'bluetooth.volume': { 'zh-CN': '蓝牙音量', 'zh-TW': '藍牙音量', 'en': 'Bluetooth Volume' },
    'bluetooth.prev': { 'zh-CN': '上一首', 'zh-TW': '上一首', 'en': 'Previous' },
    'bluetooth.next': { 'zh-CN': '下一首', 'zh-TW': '下一首', 'en': 'Next' },
    'bluetooth.notConnectedTitle': { 'zh-CN': 'Bluetooth device not connected', 'zh-TW': '藍牙裝置未連接', 'en': 'Bluetooth device not connected' },
    'bluetooth.notConnectedMeta': { 'zh-CN': 'Connect a device to use playback controls', 'zh-TW': '連接裝置以使用播放控制', 'en': 'Connect a device to use playback controls' },

    // ---------- 收音机模块 ----------
    'radio.console': { 'zh-CN': '收音机控制台', 'zh-TW': '收音機控制檯', 'en': 'Radio Console' },
    'radio.mode': { 'zh-CN': '收音机模式', 'zh-TW': '收音機模式', 'en': 'Radio Mode' },
    'radio.prevStation': { 'zh-CN': '上一台', 'zh-TW': '上一台', 'en': 'Previous Station' },
    'radio.nextStation': { 'zh-CN': '下一台', 'zh-TW': '下一台', 'en': 'Next Station' },
    'radio.tuneDown': { 'zh-CN': '微调频左', 'zh-TW': '微調頻左', 'en': 'Tune Down' },
    'radio.tuneUp': { 'zh-CN': '微调频右', 'zh-TW': '微調頻右', 'en': 'Tune Up' },
    'radio.scan': { 'zh-CN': '搜台', 'zh-TW': '掃台', 'en': 'Scan' },
    'radio.favoriteCurrent': { 'zh-CN': '收藏当前电台', 'zh-TW': '收藏目前電台', 'en': 'Favorite Current Station' },
    'radio.myFavorites': { 'zh-CN': '我的收藏', 'zh-TW': '我的收藏', 'en': 'My Favorites' },
    'radio.searching': { 'zh-CN': '电台搜索中', 'zh-TW': '電台搜尋中', 'en': 'Searching for stations' },
    'radio.searchDialog': { 'zh-CN': '电台搜索中', 'zh-TW': '電台搜尋中', 'en': 'Station search' },
    'radio.noStations': { 'zh-CN': '暂无可播放频道', 'zh-TW': '暫無可播放頻道', 'en': 'No stations available' },
    'radio.scanComplete': { 'zh-CN': '电台扫描完成', 'zh-TW': '電台掃描完成', 'en': 'Station scan complete' },
    'radio.searchComplete': { 'zh-CN': '电台搜索完成', 'zh-TW': '電台搜尋完成', 'en': 'Station search complete' },
    'radio.refreshingChannels': { 'zh-CN': '正在刷新频道列表...', 'zh-TW': '正在刷新頻道列表...', 'en': 'Refreshing channel list...' },
    'radio.locationRequired': { 'zh-CN': '需要位置权限以获取本地电台', 'zh-TW': '需要位置權限以取得本地電台', 'en': 'Location permission required for local stations' },
    'radio.noLocationDefault': { 'zh-CN': '未获得位置权限，使用默认频道', 'zh-TW': '未取得位置權限，使用預設頻道', 'en': 'Location unavailable, using default channels' },
    'radio.offlineFm': { 'zh-CN': '无网络连接，切换至离线FM模式', 'zh-TW': '無網路連線，切換至離線FM模式', 'en': 'No network, switching to offline FM' },
    'radio.volume': { 'zh-CN': '收音机音量', 'zh-TW': '收音機音量', 'en': 'Radio Volume' },
    'radio.fm': { 'zh-CN': 'FM', 'zh-TW': 'FM', 'en': 'FM' },
    'radio.am': { 'zh-CN': 'AM', 'zh-TW': 'AM', 'en': 'AM' },

    // ---------- 播放模式 ----------
    'mode.loopList': { 'zh-CN': '全部文件循环', 'zh-TW': '全部檔案循環', 'en': 'Loop All Files' },
    'mode.loopSingle': { 'zh-CN': '单曲循环', 'zh-TW': '單曲循環', 'en': 'Loop Single' },
    'mode.shuffle': { 'zh-CN': '全部文件随机', 'zh-TW': '全部檔案隨機', 'en': 'Shuffle All' },
    'mode.sequence': { 'zh-CN': '顺序播放', 'zh-TW': '順序播放', 'en': 'Play in Order' },

    // ---------- 抽屉面板 ----------
    'panel.favorites': { 'zh-CN': '收藏列表', 'zh-TW': '收藏列表', 'en': 'Favorites' },
    'panel.playlist': { 'zh-CN': '音乐列表', 'zh-TW': '音樂列表', 'en': 'Music List' },
    'panel.dragHint': { 'zh-CN': '拖动收起面板', 'zh-TW': '拖動收起面板', 'en': 'Drag to collapse' },
    'panel.selectAll': { 'zh-CN': '全选', 'zh-TW': '全選', 'en': 'Select All' },
    'panel.selectCount': { 'zh-CN': '全选({n})', 'zh-TW': '全選({n})', 'en': 'Select All ({n})' },
    'panel.playPause': { 'zh-CN': '播放/暂停', 'zh-TW': '播放/暫停', 'en': 'Play/Pause' },
    'panel.edit': { 'zh-CN': '编辑', 'zh-TW': '編輯', 'en': 'Edit' },
    'panel.remove': { 'zh-CN': '移除', 'zh-TW': '移除', 'en': 'Remove' },
    'panel.songCount': { 'zh-CN': '({n})', 'zh-TW': '({n})', 'en': '({n})' },
    'panel.close': { 'zh-CN': '关闭面板', 'zh-TW': '關閉面板', 'en': 'Close Panel' },
    'panel.emptyFavorites': { 'zh-CN': '暂无收藏', 'zh-TW': '暫無收藏', 'en': 'No favorites yet' },
    'panel.emptyPlaylist': { 'zh-CN': '播放列表为空', 'zh-TW': '播放列表為空', 'en': 'Playlist is empty' },
    'panel.emptyPlaylistHint': { 'zh-CN': '播放列表为空，请先连接USB设备', 'zh-TW': '播放列表為空，請先連接USB裝置', 'en': 'Playlist empty, please connect USB device' },

    // ---------- 确认对话框 ----------
    'dialog.confirmRemoveTrack': { 'zh-CN': '确定要移除"{title}"吗？', 'zh-TW': '確定要移除"{title}"嗎？', 'en': 'Remove "{title}"?' },
    'dialog.confirmRemoveThis': { 'zh-CN': '确定要移除这首歌吗？', 'zh-TW': '確定要移除這首歌嗎？', 'en': 'Remove this song?' },
    'dialog.confirmRemoveBatch': { 'zh-CN': '确定要移除{n}首歌吗？', 'zh-TW': '確定要移除{n}首歌嗎？', 'en': 'Remove {n} song(s)?' },

    // ---------- Toast提示 ----------
    'toast.favoriteAdded': { 'zh-CN': '已添加到收藏', 'zh-TW': '已加入收藏', 'en': 'Added to favorites' },
    'toast.favoriteRemoved': { 'zh-CN': '已从收藏移除', 'zh-TW': '已從收藏移除', 'en': 'Removed from favorites' },
    'toast.favoriteCancelled': { 'zh-CN': '已取消收藏', 'zh-TW': '已取消收藏', 'en': 'Favorite cancelled' },
    'toast.favoritesUpdated': { 'zh-CN': '收藏列表已更新', 'zh-TW': '收藏列表已更新', 'en': 'Favorites updated' },

    // ---------- 播放器状态 ----------
    'player.local': { 'zh-CN': '本地音乐', 'zh-TW': '本地音樂', 'en': 'Local Music' },
    'player.usb': { 'zh-CN': 'USB音乐', 'zh-TW': 'USB音樂', 'en': 'USB Music' },
    'player.defaultTitle': { 'zh-CN': '音乐', 'zh-TW': '音樂', 'en': 'Music' },
    'player.notificationTitle': { 'zh-CN': '音乐播放', 'zh-TW': '音樂播放', 'en': 'Music Playback' },
    'player.notificationDesc': { 'zh-CN': '本地音乐后台播放控制', 'zh-TW': '本地音樂背景播放控制', 'en': 'Local music background playback control' },
    'player.previous': { 'zh-CN': '上一曲', 'zh-TW': '上一曲', 'en': 'Previous' },
    'player.next': { 'zh-CN': '下一曲', 'zh-TW': '下一曲', 'en': 'Next' },
    'player.pause': { 'zh-CN': '暂停', 'zh-TW': '暫停', 'en': 'Pause' },
    'player.play': { 'zh-CN': '播放', 'zh-TW': '播放', 'en': 'Play' },

    // ---------- 无障碍标签 ----------
    'aria.musicPlayer': { 'zh-CN': '音乐播放器', 'zh-TW': '音樂播放器', 'en': 'Music Player' },
    'aria.musicSource': { 'zh-CN': '音乐来源', 'zh-TW': '音樂來源', 'en': 'Music Source' },
    'aria.usbModule': { 'zh-CN': 'U盘音乐', 'zh-TW': '隨身碟音樂', 'en': 'USB Music' },
    'aria.usbProgress': { 'zh-CN': 'U盘播放进度', 'zh-TW': '隨身碟播放進度', 'en': 'USB Playback Progress' },
    'aria.usbControls': { 'zh-CN': 'U盘音乐播放控制', 'zh-TW': '隨身碟音樂播放控制', 'en': 'USB Music Controls' },
    'aria.bluetoothModule': { 'zh-CN': '蓝牙音乐', 'zh-TW': '藍牙音樂', 'en': 'Bluetooth Music' },
    'aria.bluetoothConsole': { 'zh-CN': '蓝牙音乐控制台', 'zh-TW': '藍牙音樂控制檯', 'en': 'Bluetooth Music Console' },
    'aria.bluetoothProgress': { 'zh-CN': '蓝牙播放进度', 'zh-TW': '藍牙播放進度', 'en': 'Bluetooth Playback Progress' },
    'aria.bluetoothVolume': { 'zh-CN': '蓝牙音量', 'zh-TW': '藍牙音量', 'en': 'Bluetooth Volume' },
    'aria.radioModule': { 'zh-CN': '收音机', 'zh-TW': '收音機', 'en': 'Radio' },
    'aria.radioConsole': { 'zh-CN': '收音机控制台', 'zh-TW': '收音機控制檯', 'en': 'Radio Console' },
    'aria.radioMode': { 'zh-CN': '收音机模式', 'zh-TW': '收音機模式', 'en': 'Radio Mode' },
    'aria.radioVolume': { 'zh-CN': '收音机音量', 'zh-TW': '收音機音量', 'en': 'Radio Volume' },
    'aria.radioSearch': { 'zh-CN': '电台搜索中', 'zh-TW': '電台搜尋中', 'en': 'Searching for stations' },
    'aria.favoritePanel': { 'zh-CN': '收藏列表', 'zh-TW': '收藏列表', 'en': 'Favorites' },
    'aria.playlistPanel': { 'zh-CN': '音乐列表', 'zh-TW': '音樂列表', 'en': 'Music List' },
    'aria.dragCollapse': { 'zh-CN': '拖动收起面板', 'zh-TW': '拖動收起面板', 'en': 'Drag to collapse' },
    'aria.closePanel': { 'zh-CN': '关闭面板', 'zh-TW': '關閉面板', 'en': 'Close Panel' },
    'aria.confirmDialog': { 'zh-CN': '确认对话框', 'zh-TW': '確認對話框', 'en': 'Confirm Dialog' },
    'aria.nextSong': { 'zh-CN': '下一首', 'zh-TW': '下一首', 'en': 'Next Song' },
    'aria.prevSong': { 'zh-CN': '上一首', 'zh-TW': '上一首', 'en': 'Previous Song' },
    'aria.playOrPause': { 'zh-CN': '播放或暂停', 'zh-TW': '播放或暫停', 'en': 'Play or Pause' },
    'aria.confirmRemove': { 'zh-CN': '移除', 'zh-TW': '移除', 'en': 'Remove' },

    // ---------- 曲目操作 ----------
    'action.play': { 'zh-CN': '播放', 'zh-TW': '播放', 'en': 'Play' },
    'action.pause': { 'zh-CN': '暂停', 'zh-TW': '暫停', 'en': 'Pause' },
    'action.playing': { 'zh-CN': '播放中', 'zh-TW': '播放中', 'en': 'Playing' },
    'action.select': { 'zh-CN': '选择', 'zh-TW': '選擇', 'en': 'Select' },
    'action.selectAll': { 'zh-CN': '全选', 'zh-TW': '全選', 'en': 'Select All' },
    'action.addFavorite': { 'zh-CN': '添加到收藏', 'zh-TW': '加入收藏', 'en': 'Add to Favorites' },
    'action.cancelFavorite': { 'zh-CN': '取消收藏', 'zh-TW': '取消收藏', 'en': 'Remove Favorite' },
    'action.delete': { 'zh-CN': '删除', 'zh-TW': '刪除', 'en': 'Delete' },

    // ---------- 通用补充 ----------
    'common.default': { 'zh-CN': '默认', 'zh-TW': '預設', 'en': 'Default' },
    'common.songUnit': { 'zh-CN': '首', 'zh-TW': '首', 'en': ' songs' },

    // ---------- 确认对话框补充 ----------
    'dialog.confirmRemoveSingle': { 'zh-CN': '确定要移除这首歌吗？', 'zh-TW': '確定要移除這首歌嗎？', 'en': 'Remove this song?' },

    // ---------- 蓝牙模块补充 ----------
    'bluetooth.device': { 'zh-CN': '设备', 'zh-TW': '裝置', 'en': 'Device' },
    'bluetooth.unknownDevice': { 'zh-CN': '未知设备', 'zh-TW': '未知裝置', 'en': 'Unknown Device' },
    'bluetooth.connect': { 'zh-CN': '连接', 'zh-TW': '連接', 'en': 'Connect' },
    'bluetooth.connectFailed': { 'zh-CN': '蓝牙连接失败', 'zh-TW': '藍牙連接失敗', 'en': 'Bluetooth connection failed' },
    'bluetooth.connectDeviceFirst': { 'zh-CN': '请先连接蓝牙设备', 'zh-TW': '請先連接藍牙裝置', 'en': 'Connect a Bluetooth device first' },
    'bluetooth.connectNotReady': { 'zh-CN': '蓝牙尚未就绪', 'zh-TW': '藍牙尚未就緒', 'en': 'Bluetooth not ready' },
    'bluetooth.noDevices': { 'zh-CN': '未发现蓝牙设备', 'zh-TW': '未發現藍牙裝置', 'en': 'No Bluetooth devices found' },
    'bluetooth.notSupported': { 'zh-CN': '不支持蓝牙', 'zh-TW': '不支援藍牙', 'en': 'Bluetooth not supported' },
    'bluetooth.browserNotSupported': { 'zh-CN': '当前环境不支持蓝牙', 'zh-TW': '目前環境不支援藍牙', 'en': 'Bluetooth not supported in this environment' },
    'bluetooth.scanningDevices': { 'zh-CN': '正在搜索蓝牙设备', 'zh-TW': '正在搜尋藍牙裝置', 'en': 'Scanning for Bluetooth devices' },
    'bluetooth.pairing': { 'zh-CN': '配对中', 'zh-TW': '配對中', 'en': 'Pairing' },
    'bluetooth.paired': { 'zh-CN': '已配对', 'zh-TW': '已配對', 'en': 'Paired' },
    'bluetooth.waitingPairing': { 'zh-CN': '等待配对', 'zh-TW': '等待配對', 'en': 'Waiting to pair' },
    'bluetooth.disconnecting': { 'zh-CN': '正在断开', 'zh-TW': '正在斷開', 'en': 'Disconnecting' },
    'bluetooth.connectedAsOutput': { 'zh-CN': '已连接为音频输出', 'zh-TW': '已連接為音訊輸出', 'en': 'Connected as audio output' },
    'bluetooth.allDisconnected': { 'zh-CN': '所有蓝牙设备已断开', 'zh-TW': '所有藍牙裝置已斷開', 'en': 'All Bluetooth devices disconnected' },
    'bluetooth.turnedOn': { 'zh-CN': '蓝牙已开启', 'zh-TW': '藍牙已開啟', 'en': 'Bluetooth turned on' },
    'bluetooth.turnedOff': { 'zh-CN': '蓝牙已关闭', 'zh-TW': '藍牙已關閉', 'en': 'Bluetooth turned off' },
    'bluetooth.turnOnAndRetry': { 'zh-CN': '请开启蓝牙后重试', 'zh-TW': '請開啟藍牙後重試', 'en': 'Enable Bluetooth and retry' },
    'bluetooth.connectionException': { 'zh-CN': '蓝牙连接异常', 'zh-TW': '藍牙連接異常', 'en': 'Bluetooth connection error' },
    'bluetooth.autoReconnectHint': { 'zh-CN': '将尝试自动重连', 'zh-TW': '將嘗試自動重連', 'en': 'Will try auto-reconnect' },
    'bluetooth.checkPermissionAndRetry': { 'zh-CN': '请检查蓝牙权限后重试', 'zh-TW': '請檢查藍牙權限後重試', 'en': 'Check Bluetooth permission and retry' },
    'bluetooth.statusReadFailed': { 'zh-CN': '读取蓝牙状态失败', 'zh-TW': '讀取藍牙狀態失敗', 'en': 'Failed to read Bluetooth status' },
    'bluetooth.enableA2dpInSettings': { 'zh-CN': '请在系统设置中启用媒体音频', 'zh-TW': '請在系統設定中啟用媒體音訊', 'en': 'Enable media audio in system settings' },
    'bluetooth.enableA2dpSwitch': { 'zh-CN': '请开启媒体音频开关', 'zh-TW': '請開啟媒體音訊開關', 'en': 'Enable media audio switch' },
    'bluetooth.musicPlaying': { 'zh-CN': '音乐播放中', 'zh-TW': '音樂播放中', 'en': 'Music playing' },
    'bluetooth.musicPaused': { 'zh-CN': '音乐已暂停', 'zh-TW': '音樂已暫停', 'en': 'Music paused' },
    'bluetooth.playingBtMusic': { 'zh-CN': '正在播放蓝牙音乐', 'zh-TW': '正在播放藍牙音樂', 'en': 'Playing Bluetooth music' },
    'bluetooth.pausingBtMusic': { 'zh-CN': '正在暂停蓝牙音乐', 'zh-TW': '正在暫停藍牙音樂', 'en': 'Pausing Bluetooth music' },
    'bluetooth.playBtMusic': { 'zh-CN': '播放蓝牙音乐', 'zh-TW': '播放藍牙音樂', 'en': 'Play Bluetooth music' },
    'bluetooth.pauseBtMusic': { 'zh-CN': '暂停蓝牙音乐', 'zh-TW': '暫停藍牙音樂', 'en': 'Pause Bluetooth music' },
    'bluetooth.connectedAutoPlay': { 'zh-CN': '已连接并自动播放', 'zh-TW': '已連接並自動播放', 'en': 'Connected and auto-playing' },
    'bluetooth.playModeLabel': { 'zh-CN': '播放模式：{mode}', 'zh-TW': '播放模式：{mode}', 'en': 'Play Mode: {mode}' },
    'bluetooth.sentPrevTrack': { 'zh-CN': '已发送上一首', 'zh-TW': '已發送上一首', 'en': 'Previous track sent' },
    'bluetooth.sentNextTrack': { 'zh-CN': '已发送下一首', 'zh-TW': '已發送下一首', 'en': 'Next track sent' },
    'bluetooth.sentFastForward': { 'zh-CN': '已发送快进指令', 'zh-TW': '已發送快進指令', 'en': 'Fast forward sent' },
    'bluetooth.sentRewind': { 'zh-CN': '已发送快退指令', 'zh-TW': '已發送快退指令', 'en': 'Rewind sent' },
    'bluetooth.nativeCallFailed': { 'zh-CN': '调用原生接口失败', 'zh-TW': '呼叫原生介面失敗', 'en': 'Native call failed' },
    'bluetooth.signalLevel': { 'zh-CN': '信号{bars}', 'zh-TW': '訊號{bars}', 'en': 'Signal {bars}' },
    'bluetooth.signalUnknown': { 'zh-CN': '信号未知', 'zh-TW': '訊號未知', 'en': 'Unknown signal' },
    'bluetooth.typeAudio': { 'zh-CN': '音频设备', 'zh-TW': '音訊裝置', 'en': 'Audio Device' },
    'bluetooth.typePhone': { 'zh-CN': '手机', 'zh-TW': '手機', 'en': 'Phone' },
    'bluetooth.typeComputer': { 'zh-CN': '电脑', 'zh-TW': '電腦', 'en': 'Computer' },
    'bluetooth.typePeripheral': { 'zh-CN': '外设', 'zh-TW': '外設', 'en': 'Peripheral' },
    'bluetooth.typeWearable': { 'zh-CN': '可穿戴设备', 'zh-TW': '穿戴裝置', 'en': 'Wearable' },
    'bluetooth.typeNetwork': { 'zh-CN': '网络设备', 'zh-TW': '網路裝置', 'en': 'Network Device' },
    'bluetooth.typeImaging': { 'zh-CN': '影像设备', 'zh-TW': '影像裝置', 'en': 'Imaging Device' },
    'bluetooth.typeUnknown': { 'zh-CN': '未知设备', 'zh-TW': '未知裝置', 'en': 'Unknown Device' },

    // ---------- USB模块补充 ----------
    'usb.rescan': { 'zh-CN': '重新扫描', 'zh-TW': '重新掃描', 'en': 'Rescan' },
    'usb.rootDir': { 'zh-CN': '根目录', 'zh-TW': '根目錄', 'en': 'Root' },
    'usb.eventFailed': { 'zh-CN': 'USB事件处理失败', 'zh-TW': 'USB事件處理失敗', 'en': 'USB event failed' },
    'usb.discoveredMusic': { 'zh-CN': '已发现{n}首音乐', 'zh-TW': '已發現{n}首音樂', 'en': '{n} tracks discovered' },
    'usb.currentUnplayable': { 'zh-CN': '当前曲目无法播放', 'zh-TW': '目前曲目無法播放', 'en': 'Current track unplayable' },
    'usb.removedCleaned': { 'zh-CN': '已清理移除的曲目', 'zh-TW': '已清理移除的曲目', 'en': 'Removed tracks cleaned' },

    // ---------- 排序 ----------
    'sort.byFilename': { 'zh-CN': '按文件名', 'zh-TW': '按檔名', 'en': 'By Filename' },
    'sort.byArtist': { 'zh-CN': '按艺人', 'zh-TW': '按藝人', 'en': 'By Artist' },
    'sort.byAlbum': { 'zh-CN': '按专辑', 'zh-TW': '按專輯', 'en': 'By Album' },
    'sort.byDuration': { 'zh-CN': '按时长', 'zh-TW': '按時長', 'en': 'By Duration' },
    'sort.asc': { 'zh-CN': '升序', 'zh-TW': '升序', 'en': 'Ascending' },
    'sort.desc': { 'zh-CN': '降序', 'zh-TW': '降序', 'en': 'Descending' },

    // ---------- 收音机模块补充 ----------
    'radio.paused': { 'zh-CN': '已暂停', 'zh-TW': '已暫停', 'en': 'Paused' },
    'radio.playingStation': { 'zh-CN': '正在播放：{name}', 'zh-TW': '正在播放：{name}', 'en': 'Playing: {name}' },
    'radio.playingOfflineFm': { 'zh-CN': '正在播放离线FM：{name}', 'zh-TW': '正在播放離線FM：{name}', 'en': 'Playing offline FM: {name}' },
    'radio.signalStrong': { 'zh-CN': '信号强', 'zh-TW': '訊號強', 'en': 'Strong' },
    'radio.signalGood': { 'zh-CN': '信号良好', 'zh-TW': '訊號良好', 'en': 'Good' },
    'radio.receivable': { 'zh-CN': '可接收', 'zh-TW': '可接收', 'en': 'Fair' },
    'radio.signalWeak': { 'zh-CN': '信号弱', 'zh-TW': '訊號弱', 'en': 'Weak' },
    'radio.signalLevel': { 'zh-CN': '信号', 'zh-TW': '訊號', 'en': 'Signal' },
    'radio.signalUnit': { 'zh-CN': '信号', 'zh-TW': '訊號', 'en': ' signal' },
    'radio.savedToPreset': { 'zh-CN': '已保存到第{index}个预设', 'zh-TW': '已儲存到第{index}個預設', 'en': 'Saved to preset {index}' },
    'radio.noStationsPleaseScan': { 'zh-CN': '暂无电台，请搜台', 'zh-TW': '暫無電台，請掃台', 'en': 'No stations, please scan' },
    'radio.needLocationForStations': { 'zh-CN': '需要位置权限以获取本地电台', 'zh-TW': '需要位置權限以取得本地電台', 'en': 'Location permission needed for local stations' },
    'radio.refreshingLocalStations': { 'zh-CN': '正在刷新本地电台', 'zh-TW': '正在刷新本地電台', 'en': 'Refreshing local stations' },
    'radio.searchingChannels': { 'zh-CN': '正在搜索频道（{band}）', 'zh-TW': '正在搜尋頻道（{band}）', 'en': 'Searching channels ({band})' },
    'radio.identifiedStations': { 'zh-CN': '已识别{n}个电台：{name}，信号{signal}', 'zh-TW': '已識別{n}個電台：{name}，訊號{signal}', 'en': 'Found {n} stations: {name}, signal {signal}' },
    'radio.manualSearch': { 'zh-CN': '手动搜索：{name}', 'zh-TW': '手動搜尋：{name}', 'en': 'Manual search: {name}' },
    'radio.switchedTo': { 'zh-CN': '已切换到：{name}', 'zh-TW': '已切換到：{name}', 'en': 'Switched to: {name}' },
    'radio.switchingTo': { 'zh-CN': '正在切换到：{name}', 'zh-TW': '正在切換到：{name}', 'en': 'Switching to: {name}' },
    'radio.switchedToBand': { 'zh-CN': '已切换到{band}', 'zh-TW': '已切換到{band}', 'en': 'Switched to {band}' },
    'radio.switchedToLocalTuner': { 'zh-CN': '已切换到本地调谐器', 'zh-TW': '已切換到本地調諧器', 'en': 'Switched to local tuner' },
    'radio.localTuner': { 'zh-CN': '本地调谐器', 'zh-TW': '本地調諧器', 'en': 'Local tuner' },
    'radio.offlineFmBroadcast': { 'zh-CN': '离线FM广播', 'zh-TW': '離線FM廣播', 'en': 'Offline FM broadcast' },
    'radio.offlineFmScanComplete': { 'zh-CN': '离线FM扫描完成，共{n}个电台', 'zh-TW': '離線FM掃描完成，共{n}個電台', 'en': 'Offline FM scan complete, {n} stations' },
    'radio.offlineFmScanParseFailed': { 'zh-CN': '离线FM解析失败', 'zh-TW': '離線FM解析失敗', 'en': 'Offline FM parse failed' },
    'radio.offlineFmStartFailed': { 'zh-CN': '离线FM启动失败', 'zh-TW': '離線FM啟動失敗', 'en': 'Offline FM start failed' },
    'radio.offlineFmStatusReadFailed': { 'zh-CN': '离线FM状态读取失败', 'zh-TW': '離線FM狀態讀取失敗', 'en': 'Offline FM status read failed' },
    'radio.noFmHardware': { 'zh-CN': '无FM硬件', 'zh-TW': '無FM硬體', 'en': 'No FM hardware' },
    'radio.noFakeBroadcast': { 'zh-CN': '无模拟广播', 'zh-TW': '無模擬廣播', 'en': 'No simulated broadcast' },
    'radio.noNativeFmInterface': { 'zh-CN': '无原生FM接口', 'zh-TW': '無原生FM介面', 'en': 'No native FM interface' },
    'radio.noOfflineFmStations': { 'zh-CN': '无离线FM电台', 'zh-TW': '無離線FM電台', 'en': 'No offline FM stations' },
    'radio.checkingFmOffline': { 'zh-CN': '正在检查离线FM', 'zh-TW': '正在檢查離線FM', 'en': 'Checking offline FM' },
    'radio.checkingFmPlayback': { 'zh-CN': '正在检查FM播放', 'zh-TW': '正在檢查FM播放', 'en': 'Checking FM playback' },
    'radio.connectingOnlineStation': { 'zh-CN': '正在连接在线电台', 'zh-TW': '正在連接線上電台', 'en': 'Connecting to online station' },
    'radio.connectingTo': { 'zh-CN': '正在连接：{name}', 'zh-TW': '正在連接：{name}', 'en': 'Connecting to: {name}' },
    'radio.audioStartFailed': { 'zh-CN': '音频启动失败', 'zh-TW': '音訊啟動失敗', 'en': 'Audio start failed' },
    'radio.audioResumeFailed': { 'zh-CN': '音频恢复失败', 'zh-TW': '音訊恢復失敗', 'en': 'Audio resume failed' },
    'radio.buffering': { 'zh-CN': '缓冲中', 'zh-TW': '緩衝中', 'en': 'Buffering' },
    'radio.unstableBuffering': { 'zh-CN': '缓冲不稳定', 'zh-TW': '緩衝不穩定', 'en': 'Unstable buffering' },
    'radio.bufferTimeoutKeep': { 'zh-CN': '缓冲超时，保持连接', 'zh-TW': '緩衝逾時，保持連線', 'en': 'Buffer timeout, keeping connection' },
    'radio.bufferTimeoutStop': { 'zh-CN': '缓冲超时，已停止', 'zh-TW': '緩衝逾時，已停止', 'en': 'Buffer timeout, stopped' },
    'radio.stationTempUnavailable': { 'zh-CN': '电台暂时不可用', 'zh-TW': '電台暫時不可用', 'en': 'Station temporarily unavailable' },
    'radio.stationTempUnavailableStopped': { 'zh-CN': '电台暂时不可用，已停止', 'zh-TW': '電台暫時不可用，已停止', 'en': 'Station unavailable, stopped' },
    'radio.stationUnavailableStopped': { 'zh-CN': '电台不可用，已停止', 'zh-TW': '電台不可用，已停止', 'en': 'Station unavailable, stopped' },
    'radio.noStreamAvailable': { 'zh-CN': '无可用音频流', 'zh-TW': '無可用音訊流', 'en': 'No stream available' },
    'radio.noPlayableStationRetry': { 'zh-CN': '无可播放电台，请重试', 'zh-TW': '無可播放電台，請重試', 'en': 'No playable station, please retry' },
    'radio.noStationFoundRetry': { 'zh-CN': '未找到电台，请重试', 'zh-TW': '未找到電台，請重試', 'en': 'No station found, please retry' },
    'radio.switchFailedRetry': { 'zh-CN': '切换失败，请重试', 'zh-TW': '切換失敗，請重試', 'en': 'Switch failed, please retry' },
    'radio.resumedPlaying': { 'zh-CN': '已恢复播放：{name}', 'zh-TW': '已恢復播放：{name}', 'en': 'Resumed playing: {name}' },
    'radio.resumeFailedRetry': { 'zh-CN': '恢复失败，重试中', 'zh-TW': '恢復失敗，重試中', 'en': 'Resume failed, retrying' },
    'radio.autoResumeAfterSec': { 'zh-CN': '{sec}秒后自动恢复', 'zh-TW': '{sec}秒後自動恢復', 'en': 'Auto-resume in {sec}s' },
    'radio.connectionNotRestored': { 'zh-CN': '连接未恢复', 'zh-TW': '連線未恢復', 'en': 'Connection not restored' },
    'radio.networkInterrupted': { 'zh-CN': '网络已中断', 'zh-TW': '網路已中斷', 'en': 'Network interrupted' },
    'radio.networkInterruptMonitoring': { 'zh-CN': '网络中断，持续监测', 'zh-TW': '網路中斷，持續監測', 'en': 'Network interrupted, monitoring' },
    'radio.networkRestored': { 'zh-CN': '网络已恢复', 'zh-TW': '網路已恢復', 'en': 'Network restored' },
    'radio.networkRestoredResume': { 'zh-CN': '网络已恢复，恢复播放', 'zh-TW': '網路已恢復，恢復播放', 'en': 'Network restored, resuming' },
    'radio.noNetworkDetectFm': { 'zh-CN': '无网络，检测FM', 'zh-TW': '無網路，檢測FM', 'en': 'No network, detecting FM' },
    'radio.noOnlineWithoutNetwork': { 'zh-CN': '无网络无法在线播放', 'zh-TW': '無網路無法線上播放', 'en': 'No online playback without network' },

    // ---------- Web Audio / 歌词 ----------
    'webAudio.unavailable': { 'zh-CN': 'Web Audio不可用', 'zh-TW': 'Web Audio不可用', 'en': 'Web Audio unavailable' },
    'lyrics.currentMusic': { 'zh-CN': '当前音乐', 'zh-TW': '目前音樂', 'en': 'Current Music' },
    'lyrics.unknownArtist': { 'zh-CN': '未知艺人', 'zh-TW': '未知藝人', 'en': 'Unknown Artist' },
    'lyrics.playing': { 'zh-CN': '正在播放', 'zh-TW': '正在播放', 'en': 'Playing' },
    'lyrics.melodyUnfolds': { 'zh-CN': '旋律缓缓展开', 'zh-TW': '旋律緩緩展開', 'en': 'Melody unfolds' },
    'lyrics.followBeat': { 'zh-CN': '跟随节拍律动', 'zh-TW': '跟隨節拍律動', 'en': 'Follow the beat' },
    'lyrics.everyWord': { 'zh-CN': '每个字句都动听', 'zh-TW': '每個字句都動聽', 'en': 'Every word resonates' },
    'lyrics.catchMemories': { 'zh-CN': '抓住每一份回忆', 'zh-TW': '抓住每一份回憶', 'en': 'Catch every memory' },
    'lyrics.momentBelongs': { 'zh-CN': '这一刻属于你我', 'zh-TW': '這一刻屬於你我', 'en': 'This moment is ours' },
    'lyrics.liftHeadSee': { 'zh-CN': '抬头看见星光', 'zh-TW': '抬頭看見星光', 'en': 'Look up at the stars' },
    'lyrics.nextLine': { 'zh-CN': '下一句歌词', 'zh-TW': '下一句歌詞', 'en': 'The next lyric' },
    'lyrics.thanksForBeing': { 'zh-CN': '感谢一路有你', 'zh-TW': '感謝一路有你', 'en': 'Thanks for being here' }
  };

  // ==================== 核心API ====================

  /**
   * 获取翻译文本
   * @param {string} key - 翻译键
   * @param {Object} [params] - 插值参数 {n: 5, title: '歌曲名'}
   * @returns {string} 翻译文本，找不到则返回key本身
   */
  function t(key, params) {
    var entry = translations[key];
    if (!entry) {
      console.warn('[i18n] Missing key:', key);
      return key;
    }
    var text = entry[currentLanguage] || entry['zh-CN'] || key;
    if (params && typeof params === 'object') {
      text = text.replace(/\{(\w+)\}/g, function(match, paramKey) {
        return params[paramKey] !== undefined ? params[paramKey] : match;
      });
    }
    return text;
  }

  /**
   * 获取当前语言代码
   * @returns {string}
   */
  function getLanguage() {
    return currentLanguage;
  }

  /**
   * 应用翻译到DOM
   * 扫描所有 [data-i18n] 元素更新文本，处理 [data-i18n-attr] 属性翻译
   */
  function applyTranslations() {
    var elements = document.querySelectorAll('[data-i18n]');
    for (var i = 0; i < elements.length; i++) {
      var el = elements[i];
      var key = el.getAttribute('data-i18n');
      var translated = t(key);
      el.textContent = translated;
    }

    var attrElements = document.querySelectorAll('[data-i18n-attr]');
    for (var j = 0; j < attrElements.length; j++) {
      var attrEl = attrElements[j];
      var attrConfig = attrEl.getAttribute('data-i18n-attr');
      try {
        var config = JSON.parse(attrConfig);
        for (var attrName in config) {
          if (config.hasOwnProperty(attrName)) {
            attrEl.setAttribute(attrName, t(config[attrName]));
          }
        }
      } catch (e) { /* ignore malformed JSON */ }
    }

    document.documentElement.setAttribute('lang', currentLanguage);
    var titleEntry = translations['app.title'];
    if (titleEntry) {
      document.title = titleEntry[currentLanguage] || titleEntry['zh-CN'];
    }
  }

  /**
   * 初始化：检测系统语言并应用翻译
   */
  function init() {
    currentLanguage = detectLanguage();
    applyTranslations();
  }

  /**
   * 动态添加翻译条目
   */
  function addTranslation(key, values) {
    if (!key || !values) return;
    translations[key] = values;
  }

  /**
   * 动态批量添加翻译
   */
  function addTranslations(entries) {
    if (!entries) return;
    for (var key in entries) {
      if (entries.hasOwnProperty(key)) {
        translations[key] = entries[key];
      }
    }
  }

  // 导出到全局
  global.i18n = {
    init: init,
    t: t,
    getLanguage: getLanguage,
    applyTranslations: applyTranslations,
    addTranslation: addTranslation,
    addTranslations: addTranslations,
    SUPPORTED_LANGUAGES: SUPPORTED_LANGUAGES
  };

})(window);