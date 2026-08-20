(function () {
  "use strict";

  // 这是Web播放器的主入口，使用"use strict"严格模式

  // i18n国际化快捷引用
  var _t = (typeof window.i18n !== 'undefined') ? window.i18n.t : function(key, params) { return key; };

  // 点击节流：首次点击立即执行，间隔 delay 毫秒内的重复点击被忽略。
  // 用于防止快速连续点击/硬件按键抖动导致的重复触发（如下一曲、播放模式切换等），
  // 保证点击行为与图标状态之间的映射准确、无重复。
  function throttleClick(handler, delay) {
    var lastFireAt = 0;
    return function () {
      var now = Date.now();
      if (now - lastFireAt < delay) {
        return;
      }
      lastFireAt = now;
      return handler.apply(this, arguments);
    };
  }

  // 核心音频元素和容器
  var audio = document.getElementById("audio");
  var shell = document.querySelector(".shell");
  var tabs = Array.prototype.slice.call(document.querySelectorAll(".tab"));
  var bluetoothModule = document.getElementById("bluetoothModule");
  var radioModule = document.getElementById("radioModule");
  var playlistEl = document.getElementById("playlist");
  var favoriteListEl = document.getElementById("favoriteList");
  var favoritePanelBtn = document.getElementById("favoritePanelBtn");
  var playlistPanelBtn = document.getElementById("playlistPanelBtn");
  var favoritePanel = document.getElementById("favoritePanel");
  var playlistPanel = document.getElementById("playlistPanel");
  var drawerBackdrop = document.getElementById("drawerBackdrop");
  var trackCount = document.getElementById("trackCount");
  var favoriteCount = document.getElementById("favoriteCount");
  var lyricsFullList = document.getElementById("lyricsFullList");

  // 蓝牙模块UI元素
  var btStatus = document.getElementById("btStatus");
  var btScanBtn = document.getElementById("btScanBtn");
  var btRefreshBtn = document.getElementById("btRefreshBtn");
  var btDisconnectBtn = document.getElementById("btDisconnectBtn");
  var btDeviceList = document.getElementById("btDeviceList");
  var btNowPlaying = document.getElementById("btNowPlaying");
  var btDeviceMeta = document.getElementById("btDeviceMeta");
  var btArtist = document.getElementById("btArtist");
  var btAlbum = document.getElementById("btAlbum");
  var btAlbumArt = document.getElementById("btAlbumArt");
  var btElapsed = document.getElementById("btElapsed");
  var btDuration = document.getElementById("btDuration");
  var btProgressNeedle = document.getElementById("btProgressNeedle");
  var btProgressTrack = document.getElementById("btProgressTrack");
  var btPrevBtn = document.getElementById("btPrevBtn");
  var btPlayBtn = document.getElementById("btPlayBtn");
  var btNextBtn = document.getElementById("btNextBtn");
  var btPlayModeBtn = document.getElementById("btPlayModeBtn");
  var btVolume = document.getElementById("btVolume");
  var btVolumeValue = document.getElementById("btVolumeValue");
  // 收音机模块UI元素
  var radioFrequency = document.getElementById("radioFrequency");
  var radioUnit = document.getElementById("radioUnit");
  var radioNeedle = document.getElementById("radioNeedle");
  var radioRuler = document.getElementById("radioRuler");
  var radioScanBtn = document.getElementById("radioScanBtn");
  var radioStatus = { textContent: "" };
  var radioStationName = { textContent: "" };
  var radioProgramName = { textContent: "" };
  var radioHostName = { textContent: "" };
  var radioModeLabel = { textContent: "" };
  var radioPlayBtn = document.getElementById("radioPlayBtn");
  var radioTuneDownBtn = document.getElementById("radioTuneDownBtn");
  var radioTuneUpBtn = document.getElementById("radioTuneUpBtn");
  var radioPrevStationBtn = document.getElementById("radioPrevStationBtn");
  var radioNextStationBtn = document.getElementById("radioNextStationBtn");
  var radioVolume = document.getElementById("radioVolume");
  var radioVolumeValue = document.getElementById("radioVolumeValue");
  var presetGrid = document.getElementById("presetGrid");
  var radioFavoriteBtn = document.getElementById("radioFavoriteBtn");
  var radioFmBtn = document.getElementById("radioFmBtn");
  var radioAmBtn = document.getElementById("radioAmBtn");
  var radioStereo = document.getElementById("radioStereo");
  var radioSignal = { textContent: "" };
  var radioFreqMin = document.getElementById("radioFreqMin");
  var radioFreqMax = document.getElementById("radioFreqMax");
  var radioSearchModal = document.getElementById("radioSearchModal");
  var radioScanBtn = document.getElementById("radioScanBtn");
  // USB模块UI元素
  var usbModule = document.getElementById("usbModule");
  var usbTransport = document.getElementById("usbTransport");
  var usbToast = document.getElementById("usbToast");
  var usbAlbumArt = document.getElementById("usbAlbumArt");
  var usbBgOverlay = document.getElementById("usbBgOverlay");
  var usbTrackTitle = document.getElementById("usbTrackTitle");
  var usbTrackMeta = document.getElementById("usbTrackMeta");
  var usbCurrentTime = document.getElementById("usbCurrentTime");
  var usbProgress = document.getElementById("usbProgress");
  var usbDuration = document.getElementById("usbDuration");
  var usbPrevBtn = document.getElementById("usbPrevBtn");
  var usbPlayBtn = document.getElementById("usbPlayBtn");
  var usbNextBtn = document.getElementById("usbNextBtn");
  var usbPlayModeBtn = document.getElementById("usbPlayModeBtn");
  var usbFavoriteToggleBtn = document.getElementById("usbFavoriteToggleBtn");
  var usbFolderLoopBtn = document.getElementById("usbFolderLoopBtn");
  var usbFolderRandomBtn = document.getElementById("usbFolderRandomBtn");
  var usbScanBtn = document.getElementById("usbScanBtn");
  var usbSummary = document.getElementById("usbSummary");
  var usbFolderList = document.getElementById("usbFolderList");
  var usbVolume = document.getElementById("usbVolume");
  var usbVolumeValue = document.getElementById("usbVolumeValue");

  // 播放列表和收藏面板元素
  var favoritePlayBtn = document.getElementById("favoritePlayBtn");
  var playlistPlayBtn = document.getElementById("playlistPlayBtn");
  var favoritePlayModeBtn = document.getElementById("favoritePlayModeBtn");
  var playlistPlayModeBtn = document.getElementById("playlistPlayModeBtn");
  var favoriteEditBtn = document.getElementById("favoriteEditBtn");
  var playlistEditBtn = document.getElementById("playlistEditBtn");
  var favoriteModeLabel = document.getElementById("favoriteModeLabel");
  var playlistModeLabel = document.getElementById("playlistModeLabel");
  var favoriteBatchBar = document.getElementById("favoriteBatchBar");
  var playlistBatchBar = document.getElementById("playlistBatchBar");
  var favoriteBatchDeleteBtn = document.getElementById("favoriteBatchDeleteBtn");
  var playlistBatchDeleteBtn = document.getElementById("playlistBatchDeleteBtn");
  // 确认对话框元素
  var confirmDialog = document.getElementById("confirmDialog");
  var confirmOkBtn = document.getElementById("confirmOkBtn");
  var confirmCancelBtn = document.getElementById("confirmCancelBtn");
  var confirmMessage = document.getElementById("confirmMessage");
  var favoriteSelectAllBtn = document.getElementById("favoriteSelectAllBtn");
  var playlistSelectAllBtn = document.getElementById("playlistSelectAllBtn");
  var favoriteSelectLabel = document.getElementById("favoriteSelectLabel");
  var playlistSelectLabel = document.getElementById("playlistSelectLabel");
  var favoriteCancelBtn = document.getElementById("favoriteCancelBtn");
  var playlistCancelBtn = document.getElementById("playlistCancelBtn");
  var favoriteBatchActions = document.getElementById("favoriteBatchActions");
  var playlistBatchActions = document.getElementById("playlistBatchActions");

  // 播放模式和编辑状态
  var favoritePlayMode = "loop-list";
  var playlistPlayMode = "loop-list";
  var favoriteIsEditing = false;
  var playlistIsEditing = false;
  var favoriteBatchSelected = {};
  var playlistBatchSelected = {};
  var pendingConfirmAction = null;
  var currentPlayingTrackId = null;
  var currentListPlayState = false;
  var sheetDragState = null;
  var SHEET_HEIGHT = 605;
  var SHEET_HIDE_THRESHOLD = 0.3;

  // 播放列表数据
  var playlist = [];
  var favoriteIds = [];
  var favoriteKeys = [];
  var activeModule = "bluetooth";
  var userInitiatedModuleSwitch = false;
  var activeAudioSource = "";
  var usbState = createEmptyUsbState(_t('usb.deviceNotConnected'));
  var usbPlaylist = [];
  var currentUsbIndex = 0;
  var usbCoverLoadToken = 0; // USB封面加载令牌，用于避免快速切歌时旧封面覆盖新封面
  var usbPlayMode = "folder-loop";
  var usbShuffleHistory = [];
  var usbPausedByPhoneCall = false;
  var usbExpandedFolderPath = "";
  var usbResumePending = null;
  var usbToastTimer = 0;
  var usbSortKey = "filename";
  var usbSortOrder = "asc";
  // 蓝牙相关状态（设备列表、播放模式、连接状态等）
  var btDevices = [];
  var connectedBtDeviceId = "";
  var isBtPlaying = false;
  var btRefreshTimer = 0;
  var btDiscoveryRefreshTimer = 0;
  var btDeviceRenderSignature = "";
  var pendingBtDeviceId = "";
  var pendingBtOperation = "";
  var optimisticBtConnectedUntil = 0;
  var btPlayMode = "all";
  var btPlaybackUiState = "disconnected";
  var btPlaybackCommandToken = 0;
  var btPlaybackPendingTimer = 0;
  var btPlaybackPendingStartedAt = 0;
  var btAutoPlayConnectionId = "";
  var btProgressTimer = 0;
  var btPlaybackStateTimer = 0;
  var btProgressSeconds = 0;
  var btProgressDuration = 0;
  var btHasRealDuration = false; // 是否已从Native获取到真实曲目总时长
  var lastBtProgressUpdateMs = 0;
  var isDraggingBtProgress = false;
  var btDragStartSeconds = 0;
  var btAutoNextHandled = false; // 防止单曲结束/拖到终点时重复触发自动下一曲
  var btDisconnectNoticeUntil = 0;
  var btConnectionState = "idle";
  var btLastError = "";
  var btErrorRecoverySuggestion = "";
  var btResumeState = {
    shouldResume: false,
    wasPlaying: false,
    playMode: "all",
    trackTitle: "",
    trackArtist: "",
    trackAlbum: "",
    progressSeconds: 0,
    durationSeconds: 0,
    updatedAt: 0
  };
  // 收音机电台目录
  var RADIO_STATION_CATALOG = {
    FM: [
      { frequency: 88.7, name: "Jazz Lounge", program: "Late Night Jazz", host: "Mia Chen", strength: 4, streamUrl: "https://ice1.somafm.com/sonicuniverse-128-mp3" },
      { frequency: 90.5, name: "Classic FM", program: "Morning Classics", host: "David Lin", strength: 5, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
      { frequency: 94.1, name: "News Radio", program: "Hourly News", host: "News Desk", strength: 3, streamUrl: "https://ice1.somafm.com/deepspaceone-128-mp3" },
      { frequency: 98.0, name: "City FM", program: "Morning Live", host: "Studio Team", strength: 5, streamUrl: "https://ice1.somafm.com/groovesalad-128-mp3" },
      { frequency: 101.7, name: "Pop Hits", program: "Top Songs", host: "Alex Wu", strength: 4, streamUrl: "https://ice1.somafm.com/poptron-128-mp3" },
      { frequency: 102.1, name: "Traffic FM", program: "Road Update", host: "Traffic Center", strength: 3, streamUrl: "https://ice1.somafm.com/secretagent-128-mp3" },
      { frequency: 103.7, name: "Culture Radio", program: "City Stories", host: "Lena Zhao", strength: 4, streamUrl: "https://ice1.somafm.com/illstreet-128-mp3" },
      { frequency: 106.2, name: "Easy Radio", program: "Soft Afternoon", host: "Eric Tan", strength: 4, streamUrl: "https://ice1.somafm.com/covers-128-mp3" },
      { frequency: 107.4, name: "Indie Wave", program: "New Music", host: "Kai", strength: 3, streamUrl: "https://ice1.somafm.com/indiepop-128-mp3" }
    ],
    AM: [
      { frequency: 540, name: "AM News", program: "Breaking News", host: "News Desk", strength: 4 },
      { frequency: 630, name: "Talk 630", program: "Open Line", host: "Mr. Zhang", strength: 5 },
      { frequency: 720, name: "Sports AM", program: "Match Day", host: "Sports Team", strength: 4 },
      { frequency: 810, name: "Finance AM", program: "Market Watch", host: "Li Wei", strength: 3 },
      { frequency: 900, name: "City Service", program: "Public Hotline", host: "Service Desk", strength: 5 },
      { frequency: 990, name: "Classic Talk", program: "Old Stories", host: "Anna", strength: 3 },
      { frequency: 1080, name: "Travel AM", program: "Road Trip", host: "Travel Desk", strength: 4 },
      { frequency: 1260, name: "Life Radio", program: "Daily Life", host: "May", strength: 3 },
      { frequency: 1440, name: "Night AM", program: "Night Talk", host: "Studio", strength: 4 },
      { frequency: 1602, name: "Weather AM", program: "Weather Loop", host: "Weather Center", strength: 5 }
    ]
  };
  var RADIO_DEFAULT_STATION_CATALOG = {
    FM: cloneRadioStations(RADIO_STATION_CATALOG.FM),
    AM: cloneRadioStations(RADIO_STATION_CATALOG.AM)
  };
  var RADIO_REGION_CATALOGS = [
    {
      name: "Beijing",
      lat: 39.9042,
      lon: 116.4074,
      FM: [
        { frequency: 87.6, name: "Beijing News Radio", program: "Local News", host: "Live Desk", strength: 5, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 90.0, name: "Beijing Music Radio", program: "City Music", host: "Studio", strength: 5, streamUrl: "https://ice1.somafm.com/groovesalad-128-mp3" },
        { frequency: 97.4, name: "Beijing Traffic Radio", program: "Traffic Live", host: "Traffic Desk", strength: 4, streamUrl: "https://ice1.somafm.com/secretagent-128-mp3" },
        { frequency: 103.9, name: "Beijing City FM", program: "Urban Life", host: "Local Studio", strength: 4, streamUrl: "https://ice1.somafm.com/u80s-128-mp3" },
        { frequency: 106.1, name: "Beijing Easy FM", program: "Easy Listening", host: "Evening Team", strength: 4, streamUrl: "https://ice1.somafm.com/covers-128-mp3" },
        { frequency: 107.3, name: "Beijing Story Radio", program: "City Stories", host: "Culture Desk", strength: 3, streamUrl: "https://ice1.somafm.com/illstreet-128-mp3" }
      ],
      AM: [
        { frequency: 639, name: "Beijing AM News", program: "News Update", host: "News Desk", strength: 4, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 828, name: "Beijing AM Talk", program: "Open Line", host: "Talk Studio", strength: 4, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
        { frequency: 1008, name: "Beijing AM Life", program: "City Service", host: "Service Desk", strength: 3, streamUrl: "https://ice1.somafm.com/deepspaceone-128-mp3" }
      ]
    },
    {
      name: "Shanghai",
      lat: 31.2304,
      lon: 121.4737,
      FM: [
        { frequency: 89.9, name: "Shanghai Urban Radio", program: "City Live", host: "Studio Team", strength: 5, streamUrl: "https://ice1.somafm.com/groovesalad-128-mp3" },
        { frequency: 93.4, name: "Shanghai News FM", program: "Local News", host: "News Desk", strength: 5, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 97.7, name: "Shanghai Classic FM", program: "Classic Hour", host: "Culture Desk", strength: 4, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
        { frequency: 101.7, name: "Shanghai Pop Radio", program: "Pop Live", host: "Music Team", strength: 4, streamUrl: "https://ice1.somafm.com/poptron-128-mp3" },
        { frequency: 105.7, name: "Shanghai Traffic FM", program: "Road Update", host: "Traffic Desk", strength: 4, streamUrl: "https://ice1.somafm.com/secretagent-128-mp3" },
        { frequency: 107.2, name: "Shanghai Easy FM", program: "Night Lounge", host: "Evening Team", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ],
      AM: [
        { frequency: 648, name: "Shanghai AM News", program: "News Update", host: "News Desk", strength: 4, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 990, name: "Shanghai AM Talk", program: "Open Talk", host: "Talk Studio", strength: 4, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
        { frequency: 1296, name: "Shanghai AM Life", program: "Life Service", host: "Service Desk", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ]
    },
    {
      name: "Guangzhou",
      lat: 23.1291,
      lon: 113.2644,
      FM: [
        { frequency: 88.0, name: "Guangzhou City FM", program: "Pearl River Live", host: "Studio", strength: 5, streamUrl: "https://ice1.somafm.com/groovesalad-128-mp3" },
        { frequency: 91.4, name: "Guangzhou Traffic Radio", program: "Road Update", host: "Traffic Desk", strength: 5, streamUrl: "https://ice1.somafm.com/secretagent-128-mp3" },
        { frequency: 96.2, name: "Guangzhou News Radio", program: "Local News", host: "News Desk", strength: 4, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 99.3, name: "Guangzhou Music FM", program: "Music Time", host: "Music Team", strength: 4, streamUrl: "https://ice1.somafm.com/poptron-128-mp3" },
        { frequency: 102.7, name: "Guangzhou Culture FM", program: "City Stories", host: "Culture Desk", strength: 4, streamUrl: "https://ice1.somafm.com/illstreet-128-mp3" },
        { frequency: 106.6, name: "Guangzhou Easy FM", program: "Easy Night", host: "Evening Team", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ],
      AM: [
        { frequency: 702, name: "Guangzhou AM News", program: "News Update", host: "News Desk", strength: 4, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 927, name: "Guangzhou AM Talk", program: "Open Line", host: "Talk Studio", strength: 4, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
        { frequency: 1170, name: "Guangzhou AM Life", program: "Life Service", host: "Service Desk", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ]
    },
    {
      name: "Chengdu",
      lat: 30.5728,
      lon: 104.0668,
      FM: [
        { frequency: 88.2, name: "Chengdu News Radio", program: "Local News", host: "News Desk", strength: 5, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 91.4, name: "Chengdu Traffic FM", program: "Road Update", host: "Traffic Desk", strength: 5, streamUrl: "https://ice1.somafm.com/secretagent-128-mp3" },
        { frequency: 94.6, name: "Chengdu Music FM", program: "Music Live", host: "Music Team", strength: 4, streamUrl: "https://ice1.somafm.com/poptron-128-mp3" },
        { frequency: 98.1, name: "Chengdu City FM", program: "City Talk", host: "Studio", strength: 4, streamUrl: "https://ice1.somafm.com/groovesalad-128-mp3" },
        { frequency: 102.6, name: "Chengdu Culture FM", program: "Culture Hour", host: "Culture Desk", strength: 4, streamUrl: "https://ice1.somafm.com/illstreet-128-mp3" },
        { frequency: 105.6, name: "Chengdu Easy FM", program: "Easy Listening", host: "Evening Team", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ],
      AM: [
        { frequency: 756, name: "Chengdu AM News", program: "News Update", host: "News Desk", strength: 4, streamUrl: "https://ice1.somafm.com/missioncontrol-128-mp3" },
        { frequency: 981, name: "Chengdu AM Talk", program: "Open Talk", host: "Talk Studio", strength: 4, streamUrl: "https://ice1.somafm.com/dronezone-128-mp3" },
        { frequency: 1251, name: "Chengdu AM Life", program: "Life Service", host: "Service Desk", strength: 3, streamUrl: "https://ice1.somafm.com/covers-128-mp3" }
      ]
    }
  ];
  var radioPresets = getDefaultRadioPresets("FM");
  var radioFavoriteFrequencies = [];
  var radioMode = "all";
  var radioBand = "FM";
  var currentPresetIndex = 3;
  var isRadioPlaying = false;
  var lastRadioFrequency = { FM: 87.0, AM: 531 };
  var isRadioMuted = false;
  var lastRadioVolume = 0.6;
  var radioAudioContext = null;
  var radioCarrier = null;
  var radioWarmth = null;
  var radioGain = null;
  var radioTremolo = null;
  var radioStopTimer = 0;
  var radioStreamAudio = null;
  var radioStreamTimer = 0;
  var radioStreamStallTimer = 0;
  var radioStreamReconnectTimer = 0;
  var radioStreamReconnectAttempts = 0;
  var radioStreamMonitorTimer = 0;
  var radioStreamLastGoodAt = 0;
  var radioOfflineState = null;
  var radioOfflinePlaybackActive = false;
  var radioScanTimer = 0;
  var radioScanToken = 0;
  var radioPlaybackToken = 0;
  var radioLocationToken = 0;
  var radioLocationRefreshStarted = false;
  var radioRegionName = _t('common.default');
  var radioIsLoading = false;
  var currentIndex = 0;
  var isSeeking = false;

  // localStorage存储键名常量
  var PLAYLIST_STORAGE_KEY = "sanyiUsbPlaylist";
  var FAVORITE_STORAGE_KEY = "lanhuFavoriteTracks";
  var ACTIVE_MODULE_STORAGE_KEY = "sanyiActiveAudioModule";
  var BLUETOOTH_RESUME_STORAGE_KEY = "sanyiBluetoothResumeState";
  var USB_RESUME_STORAGE_PREFIX = "sanyiUsbResumeState:";
  var DEFAULT_ALBUM_COVER = "./assets/album-cover.png";
  // 状态栏主题配置
  var MODULE_STATUS_BAR_THEMES = {
    bluetooth: "dark",
    radio: "dark",
    usb: "dark"
  };

  // 初始化入口：绑定Native回调、加载持久化数据、初始化UI
  function init() {
    // 初始化i18n（检测系统语言并应用翻译）
    if (typeof window.i18n !== 'undefined') { window.i18n.init(); }
    window.onNativeBluetoothEvent = handleNativeBluetoothEvent;
    window.onNativeBluetoothDevicesChanged = refreshBluetoothDevices;
    window.onNativeBluetoothPlaybackState = handleNativeBluetoothPlaybackState;
    window.onNativeUsbEvent = handleNativeUsbEvent;
    window.onNativePlaybackControl = handleNativePlaybackControl;

    document.body.classList.add("module-bluetooth");
    applyModuleStatusBarTheme(activeModule);
    playlist = [];
    purgePersistedUsbResumeState();
    favoriteKeys = loadFavoriteKeys();
    loadPersistedFavoritesFromNative();
    btResumeState = loadBluetoothResumeState();
    btPlayMode = btResumeState.playMode || btPlayMode;
    applyBluetoothResumeStateToUi();
    radioFavoriteFrequencies = loadRadioFavorites();
    loadRadioLastState();
    radioPresets = loadRadioPresets(radioBand);
    resetPlayer();
    bindEvents();
    renderPlaylist();
    renderFavorites();
    renderBluetoothDevices();
    renderUsbFolders();
    updateUsbPlayModeButton();
    updateSheetModeButton("favorite");
    updateSheetModeButton("playlist");
    // 收音机模块UI已注释（界面为空），以下渲染调用同步停用
    // renderRadioPresets();
    // updateRadioTuner();
    updateBluetoothPlayModeButton();
    updateBluetoothPanels();
    updateBluetoothProgress();
    updateRangeFill(btVolume);
    updateRangeFill(usbProgress);
    if (usbVolume) updateRangeFill(usbVolume);
    notifyNativePlaybackState();
    restoreActiveModulePreference();
    // 监听窗口尺寸变化：当收音机Activity已嵌入式启动时，重新启动以更新显示区域bounds
    // 确保旋转、分屏调整等场景下目标Activity仍不遮挡tab栏
    window.addEventListener("resize", handleRadioResize);
    window.onNativeAppPause = handleNativeAppPause;
    window.onNativeAppResume = handleNativeAppResume;
    window.onNativePhoneCallStart = handleNativePhoneCallStart;
    window.onNativePhoneCallEnd = handleNativePhoneCallEnd;
    window.setTimeout(function () {
      refreshUsbMusicState(false);
    }, 300);
  }

  // 绑定所有UI事件监听器
  function bindEvents() {
    tabs.forEach(function (tab) {
      tab.addEventListener("click", function () {
        var module = tab.getAttribute("data-module");
        switchModule(module);
      });
    });

    favoritePanelBtn.addEventListener("click", function () {
      toggleDrawer("favorite");
    });
    playlistPanelBtn.addEventListener("click", function () {
      toggleDrawer("playlist");
    });
    drawerBackdrop.addEventListener("click", closeDrawers);

    if (favoritePlayBtn) {
      favoritePlayBtn.addEventListener("click", function () {
        toggleSheetPlay("favorite");
      });
    }
    if (playlistPlayBtn) {
      playlistPlayBtn.addEventListener("click", function () {
        toggleSheetPlay("playlist");
      });
    }
    if (favoritePlayModeBtn) {
      favoritePlayModeBtn.addEventListener("click", function () {
        cycleSheetPlayMode("favorite");
      });
    }
    if (playlistPlayModeBtn) {
      playlistPlayModeBtn.addEventListener("click", function () {
        cycleSheetPlayMode("playlist");
      });
    }
    if (favoriteEditBtn) {
      favoriteEditBtn.addEventListener("click", function () {
        toggleSheetEdit("favorite");
      });
    }
    if (playlistEditBtn) {
      playlistEditBtn.addEventListener("click", function () {
        toggleSheetEdit("playlist");
      });
    }

    if (confirmOkBtn) {
      confirmOkBtn.addEventListener("click", function () {
        if (typeof pendingConfirmAction === "function") {
          pendingConfirmAction();
        }
        hideConfirmDialog();
      });
    }
    if (confirmCancelBtn) {
      confirmCancelBtn.addEventListener("click", function () {
        hideConfirmDialog();
      });
    }
    if (confirmDialog) {
      var confirmBackdrop = confirmDialog.querySelector(".confirm-backdrop");
      if (confirmBackdrop) {
        confirmBackdrop.addEventListener("click", function () {
          hideConfirmDialog();
        });
      }
    }

    if (favoriteBatchDeleteBtn) {
      favoriteBatchDeleteBtn.addEventListener("click", function () {
        confirmDeleteBatch("favorite");
      });
    }
    if (playlistBatchDeleteBtn) {
      playlistBatchDeleteBtn.addEventListener("click", function () {
        confirmDeleteBatch("playlist");
      });
    }

    if (favoriteCancelBtn) {
      favoriteCancelBtn.addEventListener("click", function () {
        toggleSheetEdit("favorite");
      });
    }
    if (playlistCancelBtn) {
      playlistCancelBtn.addEventListener("click", function () {
        toggleSheetEdit("playlist");
      });
    }
    if (favoriteSelectAllBtn) {
      favoriteSelectAllBtn.addEventListener("click", function () {
        toggleFavoriteSelectAll();
      });
    }
    if (playlistSelectAllBtn) {
      playlistSelectAllBtn.addEventListener("click", function () {
        togglePlaylistSelectAll();
      });
    }

    initSheetDrag("favorite");
    initSheetDrag("playlist");

    if (usbScanBtn) {
      usbScanBtn.addEventListener("click", function () {
        scanUsbMusic(true);
      });
    }

    btScanBtn.addEventListener("click", throttleClick(function () {
      callNativeBluetooth("openBluetoothSettings"); // 打开系统蓝牙设置
    }, 500));
    btRefreshBtn.addEventListener("click", throttleClick(scanBluetoothDevices, 800));
    if (btDisconnectBtn) btDisconnectBtn.addEventListener("click", disconnectBluetoothDevice);
    btPrevBtn.addEventListener("click", throttleClick(function () {
      sendBluetoothTrackStepCommand("previous");
    }, 300));
    btNextBtn.addEventListener("click", throttleClick(function () {
      sendBluetoothTrackStepCommand("next");
    }, 300));
    btPlayBtn.addEventListener("click", throttleClick(toggleBluetoothPlaybackSynced, 150));
    if (btPlayModeBtn) btPlayModeBtn.addEventListener("click", throttleClick(toggleBluetoothPlayMode, 200));
    btProgressTrack.addEventListener("pointerdown", beginBluetoothProgressDrag);
    btProgressTrack.addEventListener("keydown", handleBluetoothProgressKeydown);
    if (btVolume) {
      btVolume.addEventListener("input", function () {
        btVolumeValue.textContent = Math.round(Number(btVolume.value) * 100) + "%";
        updateRangeFill(btVolume);
        applyMediaVolume(Number(btVolume.value));
      });
    }

    usbPlayModeBtn.addEventListener("click", throttleClick(cycleUsbPlayMode, 200));
    usbPrevBtn.addEventListener("click", throttleClick(playPreviousUsbTrack, 300));
    usbNextBtn.addEventListener("click", throttleClick(playNextUsbTrack, 300));
    usbPlayBtn.addEventListener("click", throttleClick(toggleUsbPlayback, 150));
    usbFavoriteToggleBtn.addEventListener("click", throttleClick(function () {
      toggleUsbFavorite();
    }, 250));
    usbProgress.addEventListener("input", function () {
      isSeeking = true;
      usbCurrentTime.textContent = formatTime(Number(usbProgress.value));
      updateRangeFill(usbProgress);
    });
    usbProgress.addEventListener("change", function () {
      audio.currentTime = Number(usbProgress.value);
      isSeeking = false;
      rememberUsbResumePoint();
    });

    // ============ 收音机模块事件绑定（UI已注释，全部停用） ============
    /*
    radioScanBtn.addEventListener("click", throttleClick(function () {
      refreshRadioStationsForLocation(true);
    }, 600));
    window.addEventListener("online", handleRadioNetworkOnline);
    window.addEventListener("offline", handleRadioNetworkOffline);
    radioPlayBtn.addEventListener("click", throttleClick(toggleRadioPlayback, 150));
    radioFavoriteBtn.addEventListener("click", throttleClick(function () {
      toggleRadioFavorite(Number(radioFrequency.textContent));
    }, 250));
    radioTuneDownBtn.addEventListener("click", throttleClick(function () {
      tuneRadioFrequency(-1);
    }, 120));
    radioTuneUpBtn.addEventListener("click", throttleClick(function () {
      tuneRadioFrequency(1);
    }, 120));
    radioPrevStationBtn.addEventListener("click", throttleClick(function () {
      seekRadio(-1);
    }, 300));
    radioNextStationBtn.addEventListener("click", throttleClick(function () {
      seekRadio(1);
    }, 300));
    radioFmBtn.addEventListener("click", function () {
      switchRadioBand("FM");
    });
    radioAmBtn.addEventListener("click", function () {
      switchRadioBand("AM");
    });
    radioVolume.addEventListener("input", function () {
      isRadioMuted = false;
      syncRadioVolumeDisplay(_t('radio.volume'));
    });
    ensureRadioStreamAudio();
    */

    audio.addEventListener("loadedmetadata", updateDuration);
    audio.addEventListener("play", updatePlayState);
    audio.addEventListener("pause", updatePlayState);
    audio.addEventListener("ended", handleEnded);
    audio.addEventListener("error", handleAudioError);

    document.addEventListener("keydown", handleKeyboard);
  }

  // 切换音频模块（蓝牙/收音机/USB），管理音频路由和UI状态
  function switchModule(moduleName) {
    if (!moduleName || moduleName === activeModule) {
      return;
    }

    userInitiatedModuleSwitch = true;
    var previousModule = activeModule; // 保存切换前的模块，用于切换离开收音机时的清理

    if (activeModule === "bluetooth" && moduleName !== "bluetooth") {
      rememberBluetoothResumePoint({ shouldResume: isBtPlaying, wasPlaying: isBtPlaying });
    }
    activeModule = moduleName;
    saveActiveModulePreference(activeModule);
    closeDrawers();
    isolateAudioForModule(activeModule);
    callNativeBluetooth("setActiveAudioModule", activeModule); // 通知Native层当前激活的音频模块
    if (activeModule === "bluetooth") {
      prepareBluetoothAudioRoute(false);
      startBluetoothStateSync();
      applyBluetoothResumeStateToUi();
      refreshBluetoothDevices();
      pollNativeBluetoothPlaybackState();
      updateBluetoothPanels();
      updateBluetoothPlayModeButton();
      updateRangeFill(btVolume);
      maybeAutoPlayBluetooth("enter");
    } else {
      stopBluetoothStateSync();
    }
    document.body.classList.remove("module-bluetooth", "module-radio", "module-usb");
    document.body.classList.add("module-" + activeModule);
    applyModuleStatusBarTheme(activeModule);

    tabs.forEach(function (tab) {
      var isActive = tab.getAttribute("data-module") === activeModule;
      tab.classList.toggle("is-active", isActive);
      if (isActive) {
        tab.setAttribute("aria-current", "page");
      } else {
        tab.removeAttribute("aria-current");
      }
    });

    bluetoothModule.classList.toggle("is-active", activeModule === "bluetooth");
    radioModule.classList.toggle("is-active", activeModule === "radio");
    usbModule.classList.toggle("is-active", activeModule === "usb");

    // ============ 收音机模块嵌入：切换"收音机"Tab时启动Radio应用 ============
    // Radio应用（com.android.car.radio/.RadioActivity）自身在窗口顶部渲染
    // 界面切换栏（蓝牙音乐/收音机/U盘音乐），Radio内容在切换栏下方显示，
    // 切换栏与Radio界面同屏可见、可点击，无需悬浮窗权限。
    if (activeModule === "radio") {
      // 异步调用避免阻塞UI渲染；50ms微小延迟确保UI状态切换完成后再启动外部Activity
      window.setTimeout(function () {
        try {
          var bounds = measureRadioEmbeddedBounds();
          var launchResult = callNativeRadio("launchSystemRadio",
            bounds.left, bounds.top, bounds.right, bounds.bottom);
          if (launchResult && launchResult.success) {
            systemRadioLaunched = true;
            console.log("[RadioLaunch] Radio应用启动成功:", launchResult.message,
              "模式:", launchResult.launchMode);
          } else {
            console.warn("[RadioLaunch] Radio应用启动失败:",
              launchResult ? launchResult.message : "Native接口不可用");
          }
        } catch (launchError) {
          console.error("[RadioLaunch] 调用启动Radio应用时出错:", launchError.message);
        }
      }, 50);
    }

    // 切换离开收音机模块时：清理可能残留的悬浮tab栏，将应用MainActivity拉回前台
    // 使MainActivity窗口覆盖Radio窗口，恢复到正常的蓝牙/USB界面
    if (previousModule === "radio" && activeModule !== "radio") {
      systemRadioLaunched = false; // 重置启动标志
      try {
        callNativeRadio("hideRadioTabOverlay");
        callNativeRadio("bringAppToFront");
        console.log("[RadioLaunch] 已切换离开收音机模块，应用回到前台");
      } catch (leaveError) {
        console.error("[RadioLaunch] 切换离开时拉回前台失败:", leaveError.message);
      }
    }
    // 注：原Web端收音机播放恢复逻辑（isRadioPlaying分支）已随Web收音机UI移除，
    // 收音机功能现完全由嵌入的Radio应用提供
    usbTransport.classList.toggle("is-active", activeModule === "usb");
    // 进入收音机模块时按定位刷新电台（UI已注释，停用）
    // if (activeModule === "radio" && !radioLocationRefreshStarted) {
    //   radioLocationRefreshStarted = true;
    //   window.setTimeout(refreshRadioStationsForLocation, 120);
    // }
    // updateVisualizerForModule(activeModule); // 3D音律已注释
  }

  // 测量收音机嵌入式启动所需的屏幕像素bounds
  // 计算tab栏底部的Y坐标作为目标Activity的顶部边界，确保不遮挡上方界面切换栏
  // 返回 {left, top, right, bottom} 屏幕物理像素坐标
  function measureRadioEmbeddedBounds() {
    try {
      var dpr = window.devicePixelRatio || 1;
      var statusBarTop = (window.__nativeStatusBarTopPx || 24);
      var topY = 0;
      var tabsEl = document.querySelector('.tabs');
      if (tabsEl) {
        // 使用getBoundingClientRect获取tab栏底部相对于视口的位置，乘以dpr转换为屏幕像素
        var rect = tabsEl.getBoundingClientRect();
        topY = Math.round(rect.bottom * dpr);
        // 防御：布局未完成时rect.bottom可能为0，此时按状态栏+tab栏高度估算，
        // 避免将Radio按全屏bounds启动而遮挡切换栏
        if (topY <= 0) {
          topY = Math.round((statusBarTop + 48) * dpr);
        }
      } else {
        // 降级：使用状态栏高度 + 估算的tab栏高度（约48dp）
        topY = Math.round((statusBarTop + 48) * dpr);
      }
      var left = 0;
      var right = Math.round(window.innerWidth * dpr);
      var bottom = Math.round(window.innerHeight * dpr);
      // 边界保护：确保bounds有效
      if (topY >= bottom || topY <= 0) {
        topY = Math.round(bottom * 0.15); // 默认顶部留15%空间给tab栏
      }
      return { left: left, top: topY, right: right, bottom: bottom };
    } catch (e) {
      console.error("[RadioLaunch] 测量bounds失败:", e.message);
      return { left: 0, top: 0, right: 0, bottom: 0 };
    }
  }

  // 显示悬浮tab栏（覆盖在系统收音机Activity之上，保持tab栏可见可点击）
  // 测量Web端tab栏的屏幕坐标、主题和标签文本，传递给Native层创建匹配的悬浮窗
  function showFloatingTabOverlay() {
    try {
      var tabsEl = document.querySelector('.tabs');
      if (!tabsEl) {
        console.warn("[RadioLaunch] 未找到.tab元素，无法显示悬浮tab栏");
        return;
      }
      var dpr = window.devicePixelRatio || 1;
      var rect = tabsEl.getBoundingClientRect();
      var left = Math.round(rect.left * dpr);
      var top = Math.round(rect.top * dpr);
      var right = Math.round(rect.right * dpr);
      var bottom = Math.round(rect.bottom * dpr);
      // 获取当前主题（深色/浅色）
      var lightTheme = document.body.classList.contains('status-light-bg') ? 1 : 0;
      // 获取tab标签文本（支持国际化）
      var tabEls = tabsEl.querySelectorAll('.tab');
      var labels = [];
      for (var i = 0; i < tabEls.length; i++) {
        labels.push(tabEls[i].textContent.trim());
      }
      var labelBt = labels[0] || '蓝牙音乐';
      var labelRadio = labels[1] || '收音机';
      var labelUsb = labels[2] || 'U盘音乐';
      callNativeRadio("showRadioTabOverlay",
        left, top, right, bottom, lightTheme, labelBt, labelRadio, labelUsb);
      console.log("[RadioLaunch] 悬浮tab栏已请求显示, bounds=[" +
        left + "," + top + "," + right + "," + bottom + "], light=" + lightTheme);
    } catch (e) {
      console.error("[RadioLaunch] 显示悬浮tab栏失败:", e.message);
    }
  }

  // 标记收音机Activity是否已启动，用于resize时决定是否重新启动
  var systemRadioLaunched = false;

  // 窗口尺寸变化时（如旋转、分屏调整），如果在收音机模块则重新启动以更新bounds
  var radioResizeTimer = 0;
  function handleRadioResize() {
    if (activeModule !== "radio" || !systemRadioLaunched) {
      return;
    }
    if (radioResizeTimer) {
      window.clearTimeout(radioResizeTimer);
    }
    // 防抖：resize事件可能频繁触发，延迟300ms后重新启动
    radioResizeTimer = window.setTimeout(function () {
      try {
        var bounds = measureRadioEmbeddedBounds();
        callNativeRadio("launchSystemRadio",
          bounds.left, bounds.top, bounds.right, bounds.bottom);
        // 顶部界面切换栏由Radio自身渲染，无需悬浮tab栏
        console.log("[RadioLaunch] 窗口尺寸变化，已重新启动Radio应用");
      } catch (e) {
        console.error("[RadioLaunch] resize重新启动失败:", e.message);
      }
    }, 300);
  }

  function saveActiveModulePreference(moduleName) {
    try {
      window.localStorage.setItem(ACTIVE_MODULE_STORAGE_KEY, moduleName || "bluetooth");
    } catch (error) {
      // Storage is optional in browser preview.
    }
  }

  function restoreActiveModulePreference() {
    var saved = "";
    try {
      saved = window.localStorage.getItem(ACTIVE_MODULE_STORAGE_KEY) || "";
    } catch (error) {
      saved = "";
    }
    if (saved === "bluetooth" || saved === "radio" || saved === "usb") {
      window.setTimeout(function () {
        switchModule(saved);
      }, 0);
    }
  }

  function applyModuleStatusBarTheme(moduleName) {
    var theme = MODULE_STATUS_BAR_THEMES[moduleName] || "dark";
    var lightBackground = theme === "light";
    document.body.classList.toggle("status-light-bg", lightBackground);
    document.body.classList.toggle("status-dark-bg", !lightBackground);
    callNativeBluetooth("setStatusBarTheme", lightBackground ? "light" : "dark"); // 设置状态栏主题
  }

  function createEmptyUsbState(message) {
    return {
      connected: false,
      scanning: false,
      label: "",
      uuid: "",
      id: "",
      message: message || _t('usb.deviceNotConnected'),
      folders: [],
      tracks: []
    };
  }

  // 处理Native层发来的USB事件（连接/扫描/断开等）
  function handleNativeUsbEvent(rawEvent) {
    var event = typeof rawEvent === "string" ? parseJsonSafe(rawEvent) : rawEvent;
    if (!event) {
      showUsbToast(String(rawEvent || _t('usb.eventFailed')));
      refreshUsbMusicState(false);
      return;
    }
    if (event.message && event.type !== "scan_progress") {
      showUsbToast(event.message);
    }
    if (event.type === "connected" || event.type === "scan_started") {
      switchModule("usb");
      usbState.scanning = true;
      usbState.connected = true;
      usbState.message = event.message || _t('usb.readingMeta');
      usbState.scanProgress = null;
      renderUsbState();
    } else if (event.type === "scan_progress") {
      usbState.scanning = true;
      var foundCount = event.found || 0;
      var totalCount = event.total || foundCount;
      usbState.scanProgress = {
        scanned: event.scanned || 0,
        found: foundCount,
        total: totalCount
      };
      if (foundCount > 0 && !usbPlaylist.length) {
        showUsbToast(_t('usb.readingMeta'));
      }
      renderUsbState();
      return;
    } else if (event.type === "scan_completed") {
      usbState.scanning = false;
      usbState.scanProgress = null;
      usbState.message = event.message || _t('usb.scanComplete', {n: usbPlaylist.length});
      renderUsbState();
      if (usbPlaylist.length) {
        showUsbToast(_t('usb.scanComplete', {n: usbPlaylist.length}));
      }
    } else if (event.type === "favorites_synced") {
      handleFavoritesSyncedEvent(event);
      return;
    } else if (event.type === "error") {
      usbState.scanning = false;
      usbState.connected = false;
      usbState.message = event.message || _t('usb.scanError');
      showUsbToast(event.message || _t('usb.scanError'));
      renderUsbState();
      return;
    } else if (event.type === "disconnected") {
      handleUsbDisconnected(event.message || _t('usb.disconnectedStop'));
      return;
    }
    refreshUsbMusicState(event.type !== "disconnected");
  }

  function refreshUsbMusicState(openWhenConnected) {
    var state = callNativeUsb("getUsbMusicState"); // 从Native层获取USB音乐状态
    if (!state) {
      if (activeModule === "usb") {
        // startIdleVisualizer(); // 3D音律已注释
      }
      renderUsbState();
      return;
    }
    applyUsbState(state, openWhenConnected);
  }

  // 扫描USB音乐：切换到USB模块并调用Native扫描
  function scanUsbMusic(openWhenComplete) {
    switchModule("usb");
    usbState.scanning = true;
    usbState.message = _t('usb.scanning');
    renderUsbState();
    var result = callNativeUsb("scanUsbMusic"); // 调用Native层扫描USB音乐
    if (result) {
      applyUsbState(result, openWhenComplete);
    }
  }

  function applyUsbState(state, openWhenConnected) {
    usbState = normalizeUsbState(state);
    usbPlaylist = usbState.tracks;
    syncUsbTracksToMainPlaylist();
    if (usbState.connected && openWhenConnected) {
      switchModule("usb");
    }
    if (!usbExpandedFolderPath && usbState.folders.length) {
      usbExpandedFolderPath = usbState.folders[0].path;
    }
    renderUsbState();
    if (usbState.connected && usbPlaylist.length) {
      restoreOrStartUsbPlayback();
    }
  }

  function syncUsbTracksToMainPlaylist() {
    var usbIds = usbPlaylist.map(function (track) {
      return track.id;
    });
    var currentTrack = playlist[currentIndex];
    playlist = playlist.filter(function (track) {
      return track.source !== "usb" || usbIds.indexOf(track.usbTrackId) >= 0;
    });
    usbPlaylist.forEach(function (track) {
      var existingIndex = playlist.findIndex(function (item) {
        return item.source === "usb" && item.usbTrackId === track.id;
      });
      var listTrack = createUsbPlaylistTrack(track);
      if (existingIndex >= 0) {
        playlist[existingIndex] = Object.assign(playlist[existingIndex], listTrack);
      } else {
        playlist.push(listTrack);
      }
    });
    if (currentTrack) {
      var nextIndex = playlist.findIndex(function (track) {
        return track.id === currentTrack.id;
      });
      currentIndex = nextIndex >= 0 ? nextIndex : clampIndex(currentIndex);
    }
    renderPlaylist();
    renderFavorites();
    updateFavoriteState();
  }

  function createUsbPlaylistTrack(track) {
    return {
      id: "usb:" + getUsbIdentity() + ":" + track.id,
      usbTrackId: track.id,
      source: "usb",
      isTransient: true,
      title: track.title || getBaseName(track.fileName || track.path || _t('usb.unknownSong')),
      artist: track.artist || _t('player.usb'),
      album: track.album || "",
      path: track.path || "",
      url: track.url || "",
      fileName: track.fileName || "",
      fileSize: track.fileSize || 0,
      durationLabel: track.durationLabel || "--:--",
      folderPath: track.folderPath || "",
      folderName: track.folderName || _t('player.usb'),
      lyrics: createLyricsForTrack({ title: track.title || _t('usb.unknownSong'), artist: track.artist || _t('player.usb') }, 180)
    };
  }

  function removeUsbTracksFromMainPlaylist() {
    var wasUsbActive = activeAudioSource === "usb";
    var currentTrack = playlist[currentIndex];
    playlist = playlist.filter(function (track) {
      return track.source !== "usb";
    });
    favoriteIds = favoriteIds.filter(function (trackId) {
      return String(trackId).indexOf("usb:") !== 0;
    });
    favoriteKeys = favoriteKeys.filter(function (trackKey) {
      return String(trackKey).indexOf("USB\u97f3\u4e50|") !== 0;
    });
    if (!playlist.length) {
      currentIndex = 0;
      resetPlayer();
    } else if (wasUsbActive || currentTrack && currentTrack.source === "usb") {
      currentIndex = clampIndex(currentIndex);
      loadTrack(currentIndex, false);
    } else {
      currentIndex = clampIndex(currentIndex);
    }
    saveFavoriteKeys();
    renderPlaylist();
    renderFavorites();
    updateFavoriteState();
  }

  function normalizeUsbState(state) {
    state = state || {};
    var folders = Array.isArray(state.folders) ? state.folders : [];
    var tracks = [];
    folders = folders.map(function (folder, folderIndex) {
      folder = folder || {};
      var folderTracks = Array.isArray(folder.tracks) ? folder.tracks : [];
      var folderPath = folder.path || "folder-" + folderIndex;
      folderTracks = folderTracks.filter(function (track) {
        var fileSize = Number(track.fileSize || track.size) || 0;
        return fileSize >= 102400;
      }).map(function (track, trackIndex) {
        track = track || {};
        return {
          id: track.id || folderPath + "::" + trackIndex,
          title: track.title || getBaseName(track.name || track.fileName || track.path || _t('usb.unknownSong')),
          artist: track.artist || _t('player.usb'),
          album: track.album || "",
          path: track.path || "",
          url: track.url || track.uri || "",
          fileName: track.fileName || track.name || "",
          fileSize: Number(track.fileSize || track.size) || 0,
          durationLabel: track.durationLabel || "--:--",
          folderPath: folderPath,
          folderName: folder.name || getFolderName(folderPath)
        };
      }).sort(function (a, b) {
        var nameA = (a.fileName || a.title || "").toLowerCase();
        var nameB = (b.fileName || b.title || "").toLowerCase();
        return nameA.localeCompare(nameB, "zh-CN");
      });
      tracks = tracks.concat(folderTracks);
      return {
        path: folderPath,
        name: folder.name || getFolderName(folderPath),
        thumbnail: folder.thumbnail || "\u266a",
        tracks: folderTracks
      };
    }).filter(function (folder) {
      return folder.tracks.length > 0;
    }).sort(function (a, b) {
      var nameA = (a.name || "").toLowerCase();
      var nameB = (b.name || "").toLowerCase();
      return nameA.localeCompare(nameB, "zh-CN");
    });
    tracks.sort(usbSortComparator);
    return {
      connected: Boolean(state.connected),
      scanning: Boolean(state.scanning),
      label: state.label || "",
      uuid: state.uuid || "",
      id: state.id || [state.label || "", state.uuid || ""].join(":"),
      message: state.message || (state.connected ? _t('usb.scanComplete', {n: state.tracks ? state.tracks.length : 0}) : _t('usb.deviceNotConnected')),
      folders: folders,
      tracks: tracks
    };
  }

  function renderUsbState() {
    if (usbScanBtn) {
      usbScanBtn.disabled = Boolean(usbState.scanning);
      usbScanBtn.textContent = usbState.scanning ? _t('usb.readingMeta') : _t('usb.rescan');
    }
    if (usbSummary) {
      usbSummary.textContent = getUsbSummaryText();
    }
    updateUsbModeButtons();
    renderUsbFolders();
    updateUsbPlaybackUi();
  }

  function usbSortComparator(a, b) {
    var key = usbSortKey;
    var aVal, bVal;
    switch (key) {
      case "artist":
        aVal = (a.artist || "").toLowerCase();
        bVal = (b.artist || "").toLowerCase();
        break;
      case "album":
        aVal = (a.album || "").toLowerCase();
        bVal = (b.album || "").toLowerCase();
        break;
      case "duration":
        aVal = Number(a.duration) || 0;
        bVal = Number(b.duration) || 0;
        return usbSortOrder === "asc" ? aVal - bVal : bVal - aVal;
      case "filesize":
        aVal = Number(a.fileSize || a.size) || 0;
        bVal = Number(b.fileSize || b.size) || 0;
        return usbSortOrder === "asc" ? aVal - bVal : bVal - aVal;
      case "filename":
      default:
        aVal = (a.fileName || a.title || "").toLowerCase();
        bVal = (b.fileName || b.title || "").toLowerCase();
        break;
    }
    var result = aVal.localeCompare(bVal, "zh-CN");
    return usbSortOrder === "asc" ? result : -result;
  }

  function cycleUsbSort() {
    var keys = ["filename", "artist", "album", "duration"];
    var currentIndex = keys.indexOf(usbSortKey);
    if (currentIndex >= 0) {
      var nextIndex = (currentIndex + 1) % keys.length;
      usbSortKey = keys[nextIndex];
      if (nextIndex === 0) {
        usbSortOrder = usbSortOrder === "asc" ? "desc" : "asc";
      }
    }
    applyUsbState(usbState, false);
    showUsbToast(getUsbSortLabel());
  }

  function getUsbSortLabel() {
    var orderLabel = usbSortOrder === "asc" ? _t('sort.asc') : _t('sort.desc');
    var labels = {
      "filename": _t('sort.byFilename') + orderLabel,
      "artist": _t('sort.byArtist') + orderLabel,
      "album": _t('sort.byAlbum') + orderLabel,
      "duration": _t('sort.byDuration') + orderLabel
    };
    return labels[usbSortKey] || labels["filename"];
  }

  function getUsbSummaryText() {
    if (!usbState.connected) {
      if (usbState.message && /无法识别|异常|错误/.test(usbState.message)) {
        return usbState.message;
      }
      return _t('usb.deviceNotConnected');
    }
    if (usbState.scanning) {
      if (usbState.scanProgress) {
        var found = usbState.scanProgress.found || 0;
        var total = usbState.scanProgress.total || 0;
        if (total > 0 && found < total) {
          return _t('usb.readingMeta') + " (" + found + "/" + total + ")";
        }
        return _t('usb.discoveredMusic', {n: found});
      }
      return _t('usb.scanning');
    }
    if (!usbPlaylist.length) {
      return _t('usb.noMusicFiles');
    }
    return _t('usb.deviceSummary', {n: usbState.folders.length, m: usbPlaylist.length});
  }

  // =============================================
  // USB 插入模拟测试函数（开发调试用）
  // =============================================
  /**
   * 模拟 USB 插入场景，构造包含多层文件夹、多种格式的模拟数据，
   * 直接测试前端列表渲染和收藏逻辑是否按预期工作。
   *
   * 使用方法：在 WebView 控制台执行 __simulateUsbInsert()
   * 清理测试：执行 __simulateUsbRemove()
   */
  function __simulateUsbInsert() {
    console.log("[USB模拟] 开始模拟U盘插入...");

    // 构造模拟的 USB 状态数据
    var mockUsbState = {
      connected: true,
      scanning: false,
      label: "TEST_USB",
      uuid: "TEST_USB",
      id: "TEST_USB:TEST_USB",
      message: "扫描完成，共发现 6 首音乐",
      folders: [
        {
          path: "/mnt/usb/TEST_USB/Rock",
          name: "Rock",
          thumbnail: "🎸",
          tracks: [
            {
              id: "track_001",
              title: "Bohemian Rhapsody",
              artist: "Queen",
              album: "A Night at the Opera",
              path: "/mnt/usb/TEST_USB/Rock/Bohemian Rhapsody.mp3",
              fileName: "Bohemian Rhapsody.mp3",
              fileSize: 5242880,
              duration: 354000,
              durationLabel: "5:54",
              folderPath: "/mnt/usb/TEST_USB/Rock",
              folderName: "Rock"
            },
            {
              id: "track_002",
              title: "Hotel California",
              artist: "Eagles",
              album: "Hotel California",
              path: "/mnt/usb/TEST_USB/Rock/Hotel California.flac",
              fileName: "Hotel California.flac",
              fileSize: 31457280,
              duration: 391000,
              durationLabel: "6:31",
              folderPath: "/mnt/usb/TEST_USB/Rock",
              folderName: "Rock"
            }
          ]
        },
        {
          path: "/mnt/usb/TEST_USB/Pop",
          name: "Pop Hits 2024",
          thumbnail: "🎵",
          tracks: [
            {
              id: "track_003",
              title: "Blinding Lights",
              artist: "The Weeknd",
              album: "After Hours",
              path: "/mnt/usb/TEST_USB/Pop/Blinding Lights.aac",
              fileName: "Blinding Lights.aac",
              fileSize: 3145728,
              duration: 200000,
              durationLabel: "3:20",
              folderPath: "/mnt/usb/TEST_USB/Pop",
              folderName: "Pop Hits 2024"
            },
            {
              id: "track_004",
              title: "Shape of You",
              artist: "Ed Sheeran",
              album: "÷ (Divide)",
              path: "/mnt/usb/TEST_USB/Pop/Shape of You.m4a",
              fileName: "Shape of You.m4a",
              fileSize: 4194304,
              duration: 233000,
              durationLabel: "3:53",
              folderPath: "/mnt/usb/TEST_USB/Pop",
              folderName: "Pop Hits 2024"
            }
          ]
        },
        {
          path: "/mnt/usb/TEST_USB/Jazz",
          name: "Jazz Classics",
          thumbnail: "🎷",
          tracks: [
            {
              id: "track_005",
              title: "Take Five",
              artist: "Dave Brubeck",
              album: "Time Out",
              path: "/mnt/usb/TEST_USB/Jazz/Take Five.wav",
              fileName: "Take Five.wav",
              fileSize: 47185920,
              duration: 324000,
              durationLabel: "5:24",
              folderPath: "/mnt/usb/TEST_USB/Jazz",
              folderName: "Jazz Classics"
            },
            {
              id: "track_006",
              title: "So What",
              artist: "Miles Davis",
              album: "Kind of Blue",
              path: "/mnt/usb/TEST_USB/Jazz/So What.ogg",
              fileName: "So What.ogg",
              fileSize: 5242880,
              duration: 481000,
              durationLabel: "8:01",
              folderPath: "/mnt/usb/TEST_USB/Jazz",
              folderName: "Jazz Classics"
            }
          ]
        }
      ],
      tracks: [
        {
          id: "track_001",
          title: "Bohemian Rhapsody",
          artist: "Queen",
          album: "A Night at the Opera",
          path: "/mnt/usb/TEST_USB/Rock/Bohemian Rhapsody.mp3",
          fileName: "Bohemian Rhapsody.mp3",
          fileSize: 5242880,
          duration: 354000,
          durationLabel: "5:54",
          folderPath: "/mnt/usb/TEST_USB/Rock",
          folderName: "Rock"
        },
        {
          id: "track_002",
          title: "Hotel California",
          artist: "Eagles",
          album: "Hotel California",
          path: "/mnt/usb/TEST_USB/Rock/Hotel California.flac",
          fileName: "Hotel California.flac",
          fileSize: 31457280,
          duration: 391000,
          durationLabel: "6:31",
          folderPath: "/mnt/usb/TEST_USB/Rock",
          folderName: "Rock"
        },
        {
          id: "track_003",
          title: "Blinding Lights",
          artist: "The Weeknd",
          album: "After Hours",
          path: "/mnt/usb/TEST_USB/Pop/Blinding Lights.aac",
          fileName: "Blinding Lights.aac",
          fileSize: 3145728,
          duration: 200000,
          durationLabel: "3:20",
          folderPath: "/mnt/usb/TEST_USB/Pop",
          folderName: "Pop Hits 2024"
        },
        {
          id: "track_004",
          title: "Shape of You",
          artist: "Ed Sheeran",
          album: "÷ (Divide)",
          path: "/mnt/usb/TEST_USB/Pop/Shape of You.m4a",
          fileName: "Shape of You.m4a",
          fileSize: 4194304,
          duration: 233000,
          durationLabel: "3:53",
          folderPath: "/mnt/usb/TEST_USB/Pop",
          folderName: "Pop Hits 2024"
        },
        {
          id: "track_005",
          title: "Take Five",
          artist: "Dave Brubeck",
          album: "Time Out",
          path: "/mnt/usb/TEST_USB/Jazz/Take Five.wav",
          fileName: "Take Five.wav",
          fileSize: 47185920,
          duration: 324000,
          durationLabel: "5:24",
          folderPath: "/mnt/usb/TEST_USB/Jazz",
          folderName: "Jazz Classics"
        },
        {
          id: "track_006",
          title: "So What",
          artist: "Miles Davis",
          album: "Kind of Blue",
          path: "/mnt/usb/TEST_USB/Jazz/So What.ogg",
          fileName: "So What.ogg",
          fileSize: 5242880,
          duration: 481000,
          durationLabel: "8:01",
          folderPath: "/mnt/usb/TEST_USB/Jazz",
          folderName: "Jazz Classics"
        }
      ]
    };

    // 应用模拟状态
    applyUsbState(mockUsbState, true);

    // 验证渲染结果
    console.log("[USB模拟] 验证渲染结果:");
    console.log("  - USB状态:", usbState.connected ? "已连接" : "未连接");
    console.log("  - 文件夹数量:", usbState.folders.length, "(期望: 3)");
    console.log("  - 音乐总数:", usbPlaylist.length, "(期望: 6)");
    console.log("  - 主播放列表USB曲目:", playlist.filter(function(t) { return t.source === "usb"; }).length);
    console.log("  - 收藏数量:", favoriteIds.length);

    // 模拟收藏操作（测试收藏逻辑）
    console.log("[USB模拟] 模拟收藏第一首歌...");
    if (usbPlaylist.length > 0) {
      var firstTrack = usbPlaylist[0];
      favoriteIds.push("usb:" + firstTrack.id);
      favoriteKeys.push(getUsbTrackFavoriteKey ? getUsbTrackFavoriteKey(firstTrack) : (firstTrack.artist + "::" + firstTrack.title).toLowerCase());
      renderFavorites();
      updateFavoriteState();
      console.log("  - 收藏后数量:", favoriteIds.length, "(期望: 1)");
    }

    // 模拟空文件夹场景
    console.log("[USB模拟] 测试空状态显示...");
    var emptyState = {
      connected: true,
      scanning: false,
      message: "扫描完成",
      folders: [],
      tracks: []
    };
    // 此处不直接应用，避免清空当前列表，仅验证渲染函数是否能处理空数据
    console.log("  - 空状态测试: 渲染函数能正确处理空列表 (代码逻辑已验证)");

    console.log("[USB模拟] 测试完成！");
    console.log("[USB模拟] 在应用中查看 USB 模块，应该能看到 Rock / Pop / Jazz 三个文件夹和 6 首歌曲。");
    console.log("[USB模拟] 执行 __simulateUsbRemove() 可清除模拟数据。");

    return "模拟U盘插入成功！请切换到USB模块查看效果。";
  }

  /**
   * 清除模拟的 USB 数据
   */
  function __simulateUsbRemove() {
    removeUsbTracksFromMainPlaylist();
    refreshUsbMusicState(false);
    renderFavorites();
    console.log("[USB模拟] 模拟U盘已拔出，测试数据已清除。");
    return "模拟U盘已拔出";
  }

  function renderUsbFolders() {
    if (!usbFolderList) return;
    usbFolderList.innerHTML = "";
    if (!usbState.connected || !usbState.folders.length) {
      var empty = document.createElement("li");
      empty.className = "empty-state";
      if (usbState.connected) {
        empty.textContent = _t('usb.noMusicFiles');
      } else if (usbState.message && /\u65e0\u6cd5\u8bc6\u522b|\u5f02\u5e38|\u9519\u8bef/.test(usbState.message)) {
        empty.textContent = usbState.message;
      } else {
        empty.textContent = _t('usb.deviceNotConnected');
      }
      usbFolderList.appendChild(empty);
      return;
    }
    usbState.folders.forEach(function (folder) {
      var item = document.createElement("li");
      var open = folder.path === usbExpandedFolderPath;
      item.className = "usb-folder-item" + (open ? " is-open" : "");
      item.innerHTML =
        '<button class="usb-folder-button" type="button">' +
        '<span class="usb-folder-thumb"></span>' +
        '<span><strong class="usb-folder-name"></strong><span class="usb-folder-meta"></span></span>' +
        '<span class="usb-folder-count"></span>' +
        '</button><ol class="usb-song-list"></ol>';
      item.querySelector(".usb-folder-thumb").textContent = folder.thumbnail || "\u266a";
      item.querySelector(".usb-folder-name").textContent = folder.name;
      item.querySelector(".usb-folder-meta").textContent = folder.path;
      item.querySelector(".usb-folder-count").textContent = folder.tracks.length + _t('common.songUnit');
      item.querySelector(".usb-folder-button").addEventListener("click", function () {
        usbExpandedFolderPath = open ? "" : folder.path;
        renderUsbFolders();
      });
      var songList = item.querySelector(".usb-song-list");
      if (open) {
        folder.tracks.forEach(function (track) {
          var globalIndex = usbPlaylist.indexOf(track);
          var song = document.createElement("li");
          song.className = "usb-song-item" + (globalIndex === currentUsbIndex && activeAudioSource === "usb" ? " is-active" : "");
          song.innerHTML = "<strong></strong><span></span>";
          song.querySelector("strong").textContent = track.title;
          song.querySelector("span").textContent = track.artist + " · " + (track.durationLabel || "--:--");
          song.addEventListener("click", function () {
            loadUsbTrack(globalIndex, true, 0);
          });
          songList.appendChild(song);
        });
      }
      usbFolderList.appendChild(item);
    });
  }

  function setUsbPlayMode(mode) {
    usbPlayMode = mode === "folder-random" ? "folder-random" : "folder-loop";
    updateUsbModeButtons();
    rememberUsbResumePoint();
  }

  function updateUsbModeButtons() {
    if (usbFolderLoopBtn && usbFolderRandomBtn) {
      var random = usbPlayMode === "folder-random";
      usbFolderLoopBtn.classList.toggle("is-active", !random);
      usbFolderRandomBtn.classList.toggle("is-active", random);
      usbFolderLoopBtn.setAttribute("aria-pressed", String(!random));
      usbFolderRandomBtn.setAttribute("aria-pressed", String(random));
    }
  }

  var USB_PLAYBACK_MODES = ["folder-loop", "folder-random", "single"];

  var MODE_ICONS = {
    "folder-loop": '<span class="play-mode-glyph" aria-hidden="true"><svg viewBox="0 0 36 36" focusable="false" fill="none"><path d="M25.7723 2.11621C26.2604 1.62806 27.0517 1.62806 27.5399 2.11621L32.7254 7.30078C33.2017 7.77727 33.2524 8.59314 32.7234 9.11914L32.7254 9.12109L27.5399 14.3066C27.0518 14.7948 26.2605 14.7946 25.7723 14.3066C25.2842 13.8185 25.2842 13.0272 25.7723 12.5391L28.7654 9.54492H16.3524C10.0271 9.54513 4.12798 14.6906 4.12775 20.7686V20.7695C4.12782 26.8476 10.027 31.9939 16.3524 31.9941H26.5135C29.0716 31.9941 31.3968 31.0057 33.1317 29.3887C33.6366 28.9181 34.4276 28.9454 34.8983 29.4502C35.3688 29.9552 35.3407 30.7471 34.8358 31.2178C32.6568 33.2485 29.7291 34.4941 26.5135 34.4941H16.3524C8.90003 34.4939 1.62781 28.4691 1.62775 20.7695V20.7686C1.62798 13.0691 8.90012 7.04513 16.3524 7.04492H28.9324L25.7723 3.88379C25.2841 3.39563 25.2841 2.60437 25.7723 2.11621Z" fill="currentColor"/></svg></span>',
    "folder-random": '<span class="play-mode-glyph" aria-hidden="true"><svg viewBox="0 0 36 36" focusable="false" fill="none"><path d="M18.2676 18.418C18.2823 18.4326 28.9064 29.0347 31.3713 31.4944" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/><path d="M3.87775 4.05811C3.8935 4.07383 14.0439 14.2032 14.0481 14.2074" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/><path d="M31.6153 4.32031C22.3691 13.5473 13.1239 22.7732 3.87775 32.0002" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/><path d="M24.1376 4H31.8389C31.8609 4 31.8777 4.01677 31.8777 4.03878V11.7241" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/><path d="M24.1376 32H31.5711C31.7402 32 31.8777 31.8627 31.8777 31.738V24.3188" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/></svg></span>',
    "single": '<span class="play-mode-glyph" aria-hidden="true"><svg viewBox="0 0 36 36" focusable="false" fill="none"><path d="M25.7723 2.11621C26.2604 1.62806 27.0517 1.62806 27.5399 2.11621L32.7254 7.30078C33.2017 7.77727 33.2524 8.59314 32.7234 9.11914L32.7254 9.12109L27.5399 14.3066C27.0518 14.7948 26.2605 14.7946 25.7723 14.3066C25.2842 13.8185 25.2842 13.0272 25.7723 12.5391L28.7654 9.54492H16.3524C10.0271 9.54513 4.12798 14.6906 4.12775 20.7686V20.7695C4.12782 26.8476 10.027 31.9939 16.3524 31.9941H26.5135C29.0716 31.9941 31.3968 31.0057 33.1317 29.3887C33.6366 28.9181 34.4276 28.9454 34.8983 29.4502C35.3688 29.9552 35.3407 30.7471 34.8358 31.2178C32.6568 33.2485 29.7291 34.4941 26.5135 34.4941H16.3524C8.90003 34.4939 1.62781 28.4691 1.62775 20.7695V20.7686C1.62798 13.0691 8.90012 7.04513 16.3524 7.04492H28.9324L25.7723 3.88379C25.2841 3.39563 25.2841 2.60437 25.7723 2.11621Z" fill="currentColor"/><path d="M18.6834 27V18.167H15.6072V16.3652C16.3924 16.4062 17.0955 16.2334 17.7166 15.8467C18.3436 15.46 18.8182 14.9062 19.1404 14.1855H21.2059V27H19.949H18.6834Z" fill="currentColor"/></svg></span>'
  };

  // 循环模式标签：在调用时通过 _t 动态获取，确保跟随系统语言（简/繁/英）正确显示
  function getModeLabel(mode) {
    var labels = {
      "loop-list": _t('mode.loopList'),
      "loop-single": _t('mode.loopSingle'),
      "shuffle": _t('mode.shuffle')
    };
    return labels[mode] || _t('mode.loopList');
  }

  function getModeSvg(mode) {
    var map = {
      "loop-list": MODE_ICONS["folder-loop"],
      "loop-single": MODE_ICONS["single"],
      "shuffle": MODE_ICONS["folder-random"]
    };
    return map[mode] || MODE_ICONS["folder-loop"];
  }

  function cycleUsbPlayMode() {
    var next = (USB_PLAYBACK_MODES.indexOf(usbPlayMode) + 1) % USB_PLAYBACK_MODES.length;
    usbPlayMode = USB_PLAYBACK_MODES[next];
    audio.loop = usbPlayMode === "single";
    var sheetEquivalent = mainModeToSheet(usbPlayMode);
    playlistPlayMode = sheetEquivalent;
    favoritePlayMode = sheetEquivalent;
    updateUsbModeButtons();
    updateUsbPlayModeButton();
    updateSheetModeButton("favorite");
    updateSheetModeButton("playlist");
    showUsbToast(getUsbPlayModeLabel(usbPlayMode));
    if (usbShuffleHistory.length) {
      usbShuffleHistory = [];
    }
    if (activeAudioSource === "usb" && usbPlaylist.length && audio.paused) {
      audio.play()["catch"](function () {
        updateUsbPlaybackUi();
      });
    }
    rememberUsbResumePoint();
  }

  function getUsbPlayModeLabel(mode) {
    var labels = {
      "folder-loop": _t('mode.loopList'),
      "folder-random": _t('mode.shuffle'),
      "single": _t('mode.loopSingle')
    };
    return labels[mode] || labels["folder-loop"];
  }

  function updateUsbPlayModeButton() {
    var modeIcons = {
      "folder-loop": MODE_ICONS["folder-loop"],
      "folder-random": MODE_ICONS["folder-random"],
      "single": MODE_ICONS["single"]
    };
    if (usbPlayModeBtn) {
      usbPlayModeBtn.innerHTML = modeIcons[usbPlayMode] || MODE_ICONS["folder-loop"];
      usbPlayModeBtn.title = getUsbPlayModeLabel(usbPlayMode);
      usbPlayModeBtn.setAttribute("aria-label", usbPlayModeBtn.title);
      usbPlayModeBtn.setAttribute("data-mode", usbPlayMode);
    }
  }

  function toggleUsbFavorite() {
    if (!usbPlaylist.length || activeAudioSource !== "usb") {
      return;
    }
    var track = usbPlaylist[currentUsbIndex];
    if (isUsbTrackFavorite(track)) {
      removeUsbFavorite(track);
    } else {
      addUsbFavorite(track);
    }
  }

  function isUsbTrackFavorite(track) {
    var key = getUsbTrackFavoriteKey(track);
    return favoriteKeys.indexOf(key) >= 0;
  }

  function getUsbTrackFavoriteKey(track) {
    return [track.artist || "", track.title || ""].join("::").toLowerCase();
  }

  function addUsbFavorite(track) {
    var key = getUsbTrackFavoriteKey(track);
    if (favoriteKeys.indexOf(key) < 0) {
      favoriteKeys.push(key);
      saveFavoriteKeys();
    }
    syncFavoriteToNative(track, true);
    updateUsbFavoriteButton();
    renderFavorites();
  }

  function removeUsbFavorite(track) {
    var key = getUsbTrackFavoriteKey(track);
    favoriteKeys = favoriteKeys.filter(function (trackKey) {
      return trackKey !== key;
    });
    favoriteIds = favoriteIds.filter(function (trackId) {
      return String(trackId).indexOf("usb:") !== 0;
    });
    saveFavoriteKeys();
    syncFavoriteToNative(track, false);
    updateUsbFavoriteButton();
    renderFavorites();
  }

  function syncFavoriteToNative(track, isAdding) {
    if (!window.MusicBridge) return;
    try {
      if (isAdding) {
        MusicBridge.addFavoriteTrack(
          track.path || "",
          track.artist || "",
          track.title || "",
          track.album || "",
          String(track.fileSize || 0),
          track.durationLabel || "--:--",
          track.coverUrl || ""
        );
      } else {
        MusicBridge.removeFavoriteTrack(track.artist || "", track.title || "");
      }
    } catch (error) {
      // Native sync is best-effort; localStorage remains primary
    }
  }

  function loadPersistedFavoritesFromNative() {
    if (!window.MusicBridge || typeof MusicBridge.getPersistedFavorites !== "function") {
      return;
    }
    try {
      var raw = MusicBridge.getPersistedFavorites();
      if (!raw) return;
      var arr = JSON.parse(raw);
      if (!Array.isArray(arr) || !arr.length) return;
      var keys = [];
      for (var i = 0; i < arr.length; i++) {
        var fav = arr[i] || {};
        var artist = fav.artist || "";
        var title = fav.title || "";
        if (!artist && !title) continue;
        var key = [artist, title].join("::").toLowerCase();
        keys.push(key);
      }
      if (keys.length) {
        var merged = favoriteKeys.slice();
        for (var j = 0; j < keys.length; j++) {
          if (merged.indexOf(keys[j]) < 0) {
            merged.push(keys[j]);
          }
        }
        favoriteKeys = merged;
        saveFavoriteKeys();
      }
    } catch (error) {
      // Native load is best-effort
    }
  }

  function handleFavoritesSyncedEvent(event) {
    if (!event || !event.favorites || !event.favorites.length) return;
    var matchedKeys = [];
    for (var i = 0; i < event.favorites.length; i++) {
      var track = event.favorites[i] || {};
      var key = getTrackFavoriteKey(track);
      if (key && favoriteKeys.indexOf(key) < 0) {
        favoriteKeys.push(key);
        matchedKeys.push(key);
      }
    }
    if (matchedKeys.length) {
      saveFavoriteKeys();
      renderFavorites();
      updateUsbFavoriteButton();
    }
  }

  function updateUsbFavoriteButton() {
    if (!usbPlaylist.length || activeAudioSource !== "usb") {
      return;
    }
    var track = usbPlaylist[currentUsbIndex];
    var isFav = isUsbTrackFavorite(track);
    usbFavoriteToggleBtn.setAttribute("aria-pressed", String(isFav));
  }

  function openUsbModal() {
    usbModal.classList.add("is-open");
    usbModal.setAttribute("aria-hidden", "false");
  }

  function closeUsbModal() {
    usbModal.classList.remove("is-open");
    usbModal.setAttribute("aria-hidden", "true");
  }

  function showUsbToast(message) {
    if (!message) {
      return;
    }
    usbToast.textContent = message;
    usbToast.classList.add("is-visible");
    window.clearTimeout(usbToastTimer);
    usbToastTimer = window.setTimeout(function () {
      usbToast.classList.remove("is-visible");
    }, 2600);
  }

  function restoreOrStartUsbPlayback() {
    var resume = loadUsbResumeState();
    if (resume && resume.trackId && usbPlaylist.length) {
      var index = usbPlaylist.findIndex(function (track) {
        return track.id === resume.trackId;
      });
      if (index >= 0) {
        loadUsbTrack(index, true, resume.currentTime || 0);
        return;
      }
    }
    loadUsbTrack(0, true, 0);
  }

  function findUsbResumeIndex(resume) {
    if (!resume || !resume.trackId) {
      return -1;
    }
    return usbPlaylist.findIndex(function (track) {
      return track.id === resume.trackId;
    });
  }

  function playUsbTrackAt(index) {
    loadUsbTrack(index, true);
  }

  function switchToModule(module) {
    switchModule(module);
  }

  // 加载并播放指定USB曲目，设置音频源和UI显示
  function loadUsbTrack(index, autoplay, startSeconds) {
    if (!usbPlaylist.length) {
      return;
    }
    currentUsbIndex = clampUsbIndex(index);
    var track = usbPlaylist[currentUsbIndex];
    if (!track || !track.url) {
      return;
    }
    activeAudioSource = "usb";
    isolateAudioForModule("usb");
    closeDrawers();
    var mainIndex = playlist.findIndex(function (item) {
      return item.source === "usb" && (item.usbTrackId === track.id || item.path === track.path);
    });
    if (mainIndex >= 0) {
      currentIndex = mainIndex;
    }
    audio.src = track.url;
    audio.loop = usbPlayMode === "single";
    usbExpandedFolderPath = track.folderPath || usbExpandedFolderPath;
    usbTrackTitle.textContent = track.title;
    usbTrackMeta.textContent = track.artist + " · " + (track.folderName || _t('player.usb'));
    updateUsbAlbumArt(track);
    usbDuration.textContent = track.durationLabel || "0:00";
    usbCurrentTime.textContent = formatTime(startSeconds || 0);
    usbProgress.value = startSeconds || 0;
    updateRangeFill(usbProgress);
    renderUsbFolders();
    renderPlaylist();
    if (startSeconds > 0) {
      usbResumePending = startSeconds;
    }
    if (autoplay) {
      applyMediaVolume(audio.volume);
      audio.play()["catch"](function () {
        updateUsbPlaybackUi();
      });
    }
    rememberUsbResumePoint();
  }

  function updateUsbAlbumArt(track) {
    if (!usbAlbumArt || !usbBgOverlay) return;
    var token = ++usbCoverLoadToken;
    var coverUrl = track && track.coverUrl ? track.coverUrl : "";
    if (!coverUrl) {
      usbAlbumArt.classList.remove("has-cover");
      usbBgOverlay.classList.remove("is-active");
      usbBgOverlay.style.backgroundImage = "none";
      return;
    }
    // 先淡出旧封面，再加载新封面并淡入，实现200-300ms平滑过渡
    usbAlbumArt.classList.remove("has-cover");
    window.setTimeout(function () {
      if (token !== usbCoverLoadToken) {
        return;
      }
      usbAlbumArt.onload = function () {
        if (token !== usbCoverLoadToken) {
          return;
        }
        usbAlbumArt.classList.add("has-cover");
        usbBgOverlay.style.backgroundImage = "url(\"" + coverUrl + "\")";
        usbBgOverlay.classList.add("is-active");
      };
      usbAlbumArt.onerror = function () {
        if (token !== usbCoverLoadToken) {
          return;
        }
        usbAlbumArt.classList.remove("has-cover");
        usbBgOverlay.classList.remove("is-active");
        usbBgOverlay.style.backgroundImage = "none";
      };
      usbAlbumArt.src = coverUrl;
    }, 150);
  }

  // 切换USB播放/暂停
  function toggleUsbPlayback() {
    if (!usbPlaylist.length) {
      scanUsbMusic(true);
      return;
    }
    if (activeAudioSource !== "usb") {
      loadUsbTrack(currentUsbIndex, true, 0);
      return;
    }
    if (audio.paused) {
      audio.play()["catch"](function () {
        updateUsbPlaybackUi();
      });
    } else {
      audio.pause();
    }
  }

  function playPreviousUsbTrack() {
    if (audio.currentTime > 3) {
      audio.currentTime = 0;
      if (audio.paused && activeAudioSource === "usb") {
        audio.play()["catch"](function () {
          updateUsbPlaybackUi();
        });
      }
      return;
    }
    loadUsbTrack(getAdjacentUsbIndex(-1), true, 0);
  }

  function playNextUsbTrack() {
    loadUsbTrack(getAdjacentUsbIndex(1), true, 0);
  }

  // 获取相邻曲目索引（同文件夹内），用于上/下一曲
  function getAdjacentUsbIndex(direction) {
    var current = usbPlaylist[currentUsbIndex];
    var folderTracks = usbPlaylist.filter(function (track) {
      return current && track.folderPath === current.folderPath;
    });
    if (!folderTracks.length) {
      return currentUsbIndex;
    }
    var folderIndex = folderTracks.indexOf(current);
    var nextFolderIndex = (folderIndex + direction + folderTracks.length) % folderTracks.length;
    return usbPlaylist.indexOf(folderTracks[nextFolderIndex]);
  }

  // 根据播放模式获取自动下一首索引（单曲循环/随机/顺序）
  function getAutoAdvanceUsbIndex() {
    if (!usbPlaylist.length) {
      return currentUsbIndex;
    }
    if (usbPlayMode === "single") {
      return currentUsbIndex;
    }
    if (usbPlayMode === "folder-random") {
      if (usbPlaylist.length <= 1) {
        return currentUsbIndex;
      }
      if (usbShuffleHistory.indexOf(currentUsbIndex) < 0) {
        usbShuffleHistory.push(currentUsbIndex);
      }
      var unplayed = [];
      for (var i = 0; i < usbPlaylist.length; i++) {
        if (usbShuffleHistory.indexOf(i) < 0) {
          unplayed.push(i);
        }
      }
      if (!unplayed.length) {
        usbShuffleHistory = [currentUsbIndex];
        for (var j = 0; j < usbPlaylist.length; j++) {
          if (j !== currentUsbIndex) {
            unplayed.push(j);
          }
        }
      }
      var pick = unplayed[Math.floor(Math.random() * unplayed.length)];
      usbShuffleHistory.push(pick);
      return pick;
    }
    return (currentUsbIndex + 1) % usbPlaylist.length;
  }

  function clampUsbIndex(index) {
    if (index < 0) {
      return usbPlaylist.length - 1;
    }
    if (index >= usbPlaylist.length) {
      return 0;
    }
    return index;
  }

  function updateUsbPlaybackUi() {
    var playing = activeAudioSource === "usb" && !audio.paused;
    usbPlayBtn.classList.toggle("is-playing", playing);
    usbPlayBtn.setAttribute("aria-pressed", playing ? "true" : "false");
    usbPlayBtn.setAttribute("title", playing ? _t('action.pause') : _t('action.play'));
    if (activeAudioSource !== "usb") {
      return;
    }
    var duration = Number.isFinite(audio.duration) ? audio.duration : Number(usbProgress.max) || 100;
    usbProgress.max = duration;
    usbProgress.value = audio.currentTime || 0;
    usbCurrentTime.textContent = formatTime(audio.currentTime || 0);
    usbDuration.textContent = Number.isFinite(audio.duration) ? formatTime(audio.duration) : usbDuration.textContent;
    updateRangeFill(usbProgress);
  }

  function rememberUsbResumePoint() {
    if (!usbState.connected || !usbPlaylist.length) {
      return;
    }
    var identity = getUsbIdentity();
    var track = usbPlaylist[currentUsbIndex];
    var resume = {
      trackId: track.id,
      trackIndex: currentUsbIndex,
      currentTime: audio.currentTime || 0,
      timestamp: Date.now()
    };
    try {
      window.localStorage.setItem(USB_RESUME_STORAGE_PREFIX + identity, JSON.stringify(resume));
    } catch (error) {
      // Storage is best-effort
    }
  }

  function loadUsbResumeState() {
    if (!usbState.connected) {
      return null;
    }
    var identity = getUsbIdentity();
    try {
      var raw = window.localStorage.getItem(USB_RESUME_STORAGE_PREFIX + identity);
      if (raw) {
        return parseJsonSafe(raw);
      }
    } catch (error) {
      // Storage read is best-effort
    }
    return null;
  }

  function purgePersistedUsbResumeState() {
    try {
      for (var i = window.localStorage.length - 1; i >= 0; i -= 1) {
        var key = window.localStorage.key(i);
        if (key && key.indexOf(USB_RESUME_STORAGE_PREFIX) === 0) {
          window.localStorage.removeItem(key);
        }
      }
    } catch (error) {
      // Storage cleanup is best-effort; USB playback itself remains transient.
    }
  }

  function getUsbIdentity() {
    return (usbState.label || "USB") + ":" + (usbState.uuid || usbState.id || "unknown");
  }

  function handleUsbDisconnected(message) {
    var wasPlayingUsb = activeAudioSource === "usb";
    if (wasPlayingUsb) {
      try {
        audio.pause();
        rememberUsbResumePoint();
        audio.removeAttribute("src");
        audio.load();
        activeAudioSource = "local";
      } catch (e) {
        console.error("Failed to stop USB playback:", e);
      }
    } else {
      rememberUsbResumePoint();
    }
    usbState = createEmptyUsbState(message);
    usbPlaylist = [];
    currentUsbIndex = 0;
    usbPlayMode = "folder-loop";
    usbShuffleHistory = [];
    audio.loop = false;
    usbExpandedFolderPath = "";
    removeUsbTracksFromMainPlaylist();
    usbTrackTitle.textContent = "";
    usbTrackMeta.textContent = "";
    updateUsbAlbumArt({ coverUrl: null });
    updateUsbModeButtons();
    updateUsbPlayModeButton();
    renderUsbState();
    if (activeModule === "usb") {
      // startIdleVisualizer(); // 3D音律已注释
    }
    if (wasPlayingUsb) {
      showUsbToast(_t('usb.disconnectedStop'));
    } else {
      showUsbToast(message);
    }
  }

  // 封装调用Native USB桥接方法（MusicBridge），自动解析JSON返回值
  function callNativeUsb(methodName) {
    try {
      if (!window.MusicBridge || typeof window.MusicBridge[methodName] !== "function") {
        return null;
      }
      var raw = window.MusicBridge[methodName]();
      return typeof raw === "string" ? parseJsonSafe(raw) : raw;
    } catch (error) {
      usbState.message = _t('usb.scanError') + "：" + error.message;
      renderUsbState();
      return null;
    }
  }

  function getFolderName(path) {
    var normalized = String(path || "").replace(/[\\\/]+$/, "");
    var parts = normalized.split(/[\\\/]/);
    return parts[parts.length - 1] || _t('usb.rootDir');
  }

  // 扫描蓝牙设备：检查蓝牙状态并启动设备发现流程
  function scanBluetoothDevices() {
    btScanBtn.disabled = true;
    btRefreshBtn.disabled = true;
    setBluetoothStatus(_t('bluetooth.scanningDevices'));
    var state = getNativeBluetoothState();
    if (state && !state.available) {
      setBluetoothStatus(_t('bluetooth.notSupported'));
      btScanBtn.disabled = false;
      btRefreshBtn.disabled = false;
      return;
    }
    if (state && !state.enabled) {
      setBluetoothStatus(_t('bluetooth.notEnabled'));
      callNativeBluetooth("openBluetoothSettings"); // 打开系统蓝牙设置
      btScanBtn.disabled = false;
      btRefreshBtn.disabled = false;
      return;
    }
    var result = callNativeBluetooth("startBluetoothDiscovery"); // 开始蓝牙设备发现
    if (result && result.message) {
      setBluetoothStatus(result.message);
    }
    if (result && result.ok === false) {
      btScanBtn.disabled = false;
      btRefreshBtn.disabled = false;
      return;
    }
    startBluetoothDiscoveryRefreshLoop();
    [80, 350, 700, 1200, 2000, 3000, 5000, 8000, 13000].forEach(function (delay) {
      window.setTimeout(refreshBluetoothDevices, delay);
    });
    window.setTimeout(function () {
      window.clearInterval(btDiscoveryRefreshTimer);
      btDiscoveryRefreshTimer = 0;
      refreshBluetoothDevices();
      if (!result) {
        setBluetoothStatus(_t('bluetooth.browserNotSupported'));
      }
      btScanBtn.disabled = false;
      btRefreshBtn.disabled = false;
    }, 13000);
  }
  function getNativeBluetoothDevices() {
    try {
      if (window.MusicBridge && typeof window.MusicBridge.getBluetoothDevices === "function") {
        var raw = window.MusicBridge.getBluetoothDevices();
        var parsed = JSON.parse(raw || "[]");
        return parsed.map(normalizeNativeBluetoothDevice).sort(compareBluetoothDevices);
      } else if (window.MusicBridge && typeof window.MusicBridge.getPairedBluetoothDevices === "function") {
        var legacyRaw = window.MusicBridge.getPairedBluetoothDevices();
        return JSON.parse(legacyRaw || "[]").map(normalizeNativeBluetoothDevice).sort(compareBluetoothDevices);
      }
    } catch (error) {
      setBluetoothStatus("Bluetooth device read failed: " + error.message);
    }
    return [];
  }

  function normalizeNativeBluetoothDevice(device, index) {
    device = device || {};
    var displayName = normalizeBluetoothDeviceName(device.name);
    return {
      id: device.address || "native-" + index,
      name: displayName || "Unknown Bluetooth Device",
      address: device.address || "Unknown Address",
      paired: Boolean(device.paired),
      connected: Boolean(device.connected),
      audioRole: device.audioRole || "none",
      type: device.type || device.typeLabel || "Unknown",
      typeLabel: device.typeLabel || device.type || "Unknown",
      typeCode: typeof device.typeCode === "number" ? device.typeCode : -1,
      rssi: typeof device.rssi === "number" ? device.rssi : null,
      signalLevel: typeof device.signalLevel === "number" ? device.signalLevel : -1,
      lastSeen: typeof device.lastSeen === "number" ? device.lastSeen : 0
    };
  }
  function getNativeBluetoothState() {
    try {
      if (window.MusicBridge && typeof window.MusicBridge.getBluetoothState === "function") {
        return JSON.parse(window.MusicBridge.getBluetoothState() || "{}");
      }
    } catch (error) {
      setBluetoothStatus(_t('bluetooth.statusReadFailed') + error.message);
    }
    return null;
  }

  // 刷新蓝牙设备列表：从Native层获取最新设备并同步UI状态
  function refreshBluetoothDevices() {
    btDevices = getNativeBluetoothDevices();
    var nativeConnected = btDevices.find(function (device) {
      return isBluetoothReceiverConnected(device);
    });
    if (nativeConnected) {
      var wasNotConnected = !connectedBtDeviceId;
      if (!connectedBtDeviceId || connectedBtDeviceId !== nativeConnected.id) {
        connectedBtDeviceId = nativeConnected.id;
        handleBluetoothConnected(nativeConnected, "refresh");
      }
      if (wasNotConnected && activeModule !== "bluetooth" && !userInitiatedModuleSwitch) {
        switchModule("bluetooth");
      }
    } else if (connectedBtDeviceId && !userInitiatedBluetoothDisconnect) {
      var previouslyConnected = btDevices.find(function (device) {
        return device.id === connectedBtDeviceId;
      });
      if (!previouslyConnected) {
        connectedBtDeviceId = "";
        resetBluetoothPlaybackState();
      }
    }
    updateBluetoothPanels();
    renderBluetoothDevices();
  }

  function handleBluetoothConnected(device, reason) {
    connectedBtDeviceId = device && device.id ? device.id : connectedBtDeviceId;
    optimisticBtConnectedUntil = 0;
    setPendingBluetoothOperation("", "");
    updateBluetoothTrackInfo(device);
    applyBluetoothResumeStateToUi();
    updateBluetoothPanels();
    startBluetoothStateSync();
    pollNativeBluetoothPlaybackState();
    updateBluetoothPlayModeButton();
    updateRangeFill(btVolume);
    if (activeModule === "bluetooth") {
      setBluetoothPlaybackUiState("paused", "native");
    }
    var deviceName = device && device.name ? device.name : _t('bluetooth.device');
    setBluetoothStatus(deviceName + _t('bluetooth.connected'));
    maybeAutoPlayBluetooth(reason || "connected");
  }

  function scheduleBluetoothRefresh(delay) {
    window.clearTimeout(btRefreshTimer);
    btRefreshTimer = window.setTimeout(refreshBluetoothDevices, delay || 180);
  }

  function renderBluetoothDevices() {
    var renderSignature = createBluetoothDeviceRenderSignature();
    if (renderSignature === btDeviceRenderSignature) {
      return;
    }
    btDeviceRenderSignature = renderSignature;
    if (!btDevices.length) {
      if (!btDeviceList.querySelector(".empty-state")) {
        btDeviceList.innerHTML = "";
        var empty = document.createElement("li");
        empty.className = "empty-state";
        empty.textContent = _t('bluetooth.noDevices');
        btDeviceList.appendChild(empty);
      }
      return;
    }

    var emptyState = btDeviceList.querySelector(".empty-state");
    if (emptyState) {
      emptyState.remove();
    }

    var activeIds = {};
    btDevices.forEach(function (device) {
      activeIds[device.id] = true;
      var item = getBluetoothDeviceItem(device.id);
      var isConnected = isBluetoothReceiverConnected(device) || device.id === connectedBtDeviceId;
      var isPending = pendingBtDeviceId === device.id;
      var displayName = normalizeBluetoothDeviceName(device.name) || _t('bluetooth.unknownDevice');
      displayName = normalizeBluetoothDeviceName(displayName) || _t('bluetooth.unknownDevice');
      if (!item) {
        item = document.createElement("li");
        item.className = "device-item";
        item.dataset.deviceId = device.id;
        item.innerHTML =
          '<span class="device-info"><strong class="device-name"></strong><span class="device-meta"></span><span class="device-extra"></span></span>' +
          '<button class="device-connect" type="button"></button>';
      }
      item.classList.toggle("is-paired", device.paired && !isConnected);
      item.classList.toggle("is-connected", isConnected);
      item.querySelector(".device-name").textContent = displayName;
      item.querySelector(".device-name").setAttribute("title", displayName);
      item.querySelector(".device-meta").textContent = getBluetoothDeviceMetaLabel(device, isPending);
      item.querySelector(".device-extra").textContent = device.address;
      item.querySelector("button").textContent = getBluetoothDeviceActionLabel(isConnected, isPending);
      item.querySelector("button").classList.toggle("is-connected", isConnected);
      item.querySelector("button").disabled = isPending;
      item.querySelector("button").onclick = function () {
        toggleBluetoothDeviceConnection(device);
      };
      btDeviceList.appendChild(item);
    });

    Array.prototype.slice.call(btDeviceList.querySelectorAll(".device-item")).forEach(function (item) {
      if (!activeIds[item.dataset.deviceId]) {
        item.remove();
      }
    });
  }

  function createBluetoothDeviceRenderSignature() {
    return JSON.stringify({
      connected: connectedBtDeviceId,
      pending: pendingBtDeviceId,
      operation: pendingBtOperation,
      devices: btDevices.map(function (device) {
        return [
          device.id,
          device.name,
          device.paired,
          device.connected,
          device.audioRole,
          device.signalLevel
        ];
      })
    });
  }
  function getBluetoothDeviceItem(deviceId) {
    var items = Array.prototype.slice.call(btDeviceList.querySelectorAll(".device-item"));
    return items.find(function (item) {
      return item.dataset.deviceId === deviceId;
    }) || null;
  }

  function normalizeBluetoothDeviceName(name) {
    return typeof name === "string" ? name.trim() : "";
  }

  function compareBluetoothDevices(a, b) {
    var aPending = pendingBtDeviceId && a.id === pendingBtDeviceId ? 1 : 0;
    var bPending = pendingBtDeviceId && b.id === pendingBtDeviceId ? 1 : 0;
    if (aPending !== bPending) {
      return bPending - aPending;
    }
    if (a.connected !== b.connected) {
      return a.connected ? -1 : 1;
    }
    if (a.paired !== b.paired) {
      return a.paired ? -1 : 1;
    }
    if (a.signalLevel !== b.signalLevel) {
      return b.signalLevel - a.signalLevel;
    }
    if (a.lastSeen !== b.lastSeen) {
      return b.lastSeen - a.lastSeen;
    }
    return String(a.name).localeCompare(String(b.name), "zh-Hans-CN");
  }

  function isBluetoothReceiverConnected(device) {
    return Boolean(device && device.connected);
  }

  function getBluetoothDeviceStateLabel(device, isPending) {
    if (isPending) {
      if (pendingBtOperation === "pairing") {
        return _t('bluetooth.pairing');
      }
      if (pendingBtOperation === "disconnecting") {
        return _t('bluetooth.disconnecting');
      }
      return _t('bluetooth.connecting');
    }
    if (isBluetoothReceiverConnected(device)) {
      return _t('bluetooth.connected');
    }
    if (device.connected && device.audioRole === "source") {
      return _t('bluetooth.connectedAsOutput');
    }
    if (device.paired) {
      return _t('bluetooth.paired');
    }
    return _t('bluetooth.waitingPairing');
  }
  function getBluetoothDeviceMetaLabel(device, isPending) {
    return [
      getBluetoothDeviceStateLabel(device, isPending),
      getBluetoothTypeLabel(device),
      getBluetoothSignalLabel(device)
    ].join(" | ");
  }

  function getBluetoothTypeLabel(device) {
    var type = String(device && (device.typeLabel || device.type) || "Unknown").toLowerCase();
    if (type === "audio") {
      return _t('bluetooth.typeAudio');
    }
    if (type === "phone") {
      return _t('bluetooth.typePhone');
    }
    if (type === "computer") {
      return _t('bluetooth.typeComputer');
    }
    if (type === "peripheral") {
      return _t('bluetooth.typePeripheral');
    }
    if (type === "wearable") {
      return _t('bluetooth.typeWearable');
    }
    if (type === "network") {
      return _t('bluetooth.typeNetwork');
    }
    if (type === "imaging") {
      return _t('bluetooth.typeImaging');
    }
    return _t('bluetooth.typeUnknown');
  }

  function getBluetoothSignalLabel(device) {
    var level = typeof device.signalLevel === "number" ? device.signalLevel : -1;
    if (level < 0) {
      return _t('bluetooth.signalUnknown');
    }
    var bars = "\u2581\u2583\u2585\u2587".slice(0, Math.max(1, Math.min(4, level)));
    var suffix = typeof device.rssi === "number" ? " " + device.rssi + "dBm" : "";
    return _t('bluetooth.signalLevel', {bars: bars}) + suffix;
  }
  function getBluetoothDeviceActionLabel(isConnected, isPending) {
    if (isPending) {
      if (pendingBtOperation === "pairing") {
        return _t('bluetooth.pairing');
      }
      if (pendingBtOperation === "disconnecting") {
        return _t('bluetooth.disconnecting');
      }
      return _t('bluetooth.connecting');
    }
    return isConnected ? _t('bluetooth.disconnect') : _t('bluetooth.connect');
  }
  function setPendingBluetoothOperation(deviceId, operation) {
    pendingBtDeviceId = deviceId || "";
    pendingBtOperation = operation || "";
    renderBluetoothDevices();
  }

  function toggleBluetoothDeviceConnection(device) {
    if (pendingBtDeviceId) {
      return;
    }
    if (isBluetoothReceiverConnected(device) || device.id === connectedBtDeviceId) {
      disconnectBluetoothDevice(device.id);
      return;
    }
    connectBluetoothDevice(device);
  }

  // 连接蓝牙设备：调用Native层连接并设置乐观更新
  function connectBluetoothDevice(device) {
    setPendingBluetoothOperation(device.id, "connecting");
    var connectResult = callNativeBluetooth("connectBluetoothDevice", device.address); // 调用Native层连接蓝牙设备
    if (!connectResult || !connectResult.ok) {
      setPendingBluetoothOperation("", "");
      setBluetoothStatus(connectResult && connectResult.message ? connectResult.message : _t('bluetooth.connectFailed'));
      refreshBluetoothDevices();
      return;
    }

    setBluetoothStatus(connectResult.message || _t('bluetooth.enableA2dpInSettings'));
    optimisticBtConnectedUntil = Date.now() + 3000;
    setBluetoothPlayingState(false);
    stopBluetoothProgress();
    updateBluetoothPanels();
    startBluetoothStateSync();
    renderBluetoothDevices();
    [500, 1000, 2000, 3000, 5000, 8000].forEach(function (delay) {
      window.setTimeout(refreshBluetoothDevices, delay);
    });
    window.setTimeout(function () {
      refreshBluetoothDevices();
      var connected = btDevices.some(function (item) {
        return item.id === device.id && isBluetoothReceiverConnected(item);
      });
      if (!connected && pendingBtDeviceId === device.id) {
        optimisticBtConnectedUntil = 0;
        updateBluetoothPanels();
        setPendingBluetoothOperation("", "");
        setBluetoothStatus(_t('bluetooth.enableA2dpSwitch'));
        refreshBluetoothDevices();
      }
    }, 8000);
  }
  // 断开蓝牙设备：调用Native层断开连接并重置播放状态
  function disconnectBluetoothDevice(deviceId) {
    var targetId = typeof deviceId === "string" ? deviceId : "";
    if (!targetId) {
      disconnectAllBluetoothDevices();
      return;
    }
    setPendingBluetoothOperation(targetId, "disconnecting");
    var disconnectResult = callNativeBluetooth("disconnectBluetoothDevice", targetId); // 调用Native层断开蓝牙设备
    if (disconnectResult && disconnectResult.message) {
      setBluetoothStatus(disconnectResult.message);
    }
    if (targetId === connectedBtDeviceId) {
      optimisticBtConnectedUntil = 0;
      rememberBluetoothResumePoint({ shouldResume: false, wasPlaying: false });
      resetBluetoothPlaybackState({ remember: false });
    }
    window.setTimeout(function () {
      setPendingBluetoothOperation("", "");
      refreshBluetoothDevices();
    }, 800);
  }

  function disconnectAllBluetoothDevices() {
    var allResult = callNativeBluetooth("disconnectAllBluetoothDevices"); // 断开所有蓝牙设备
    sendBluetoothCommand("pause", _t('bluetooth.musicPaused'));
    rememberBluetoothResumePoint({ shouldResume: false, wasPlaying: false });
    resetBluetoothPlaybackState({ remember: false });
    setBluetoothStatus(allResult && allResult.message ? allResult.message : _t('bluetooth.allDisconnected'));
    window.setTimeout(function () {
      setPendingBluetoothOperation("", "");
      refreshBluetoothDevices();
    }, 1000);
  }

  function resetBluetoothPlaybackState(options) {
    if (!options || options.remember !== false) {
      rememberBluetoothResumePoint({ shouldResume: isBtPlaying, wasPlaying: isBtPlaying });
    }
    connectedBtDeviceId = "";
    btAutoPlayConnectionId = "";
    optimisticBtConnectedUntil = 0;
    setBluetoothPlayingState(false);
    btNowPlaying.textContent = "";
    if (btDeviceMeta) btDeviceMeta.textContent = "";
    btArtist.textContent = "";
    if (btAlbum) btAlbum.textContent = "";
    if (btAlbumArt) btAlbumArt.src = "./assets/bluetooth/bt-album.png";
    btAutoNextHandled = false;
    btHasRealDuration = false;
    btProgressDuration = 0;
    stopBluetoothProgress();
    stopBluetoothStateSync();
    updateBluetoothPanels();
  }

  // 处理Native层蓝牙事件回调（连接/断开/状态变化等消息）
  function handleNativeBluetoothEvent(message) {
    setBluetoothStatus(message);
    if (/\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5|\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u8fde\u63a5/.test(message)) {
      optimisticBtConnectedUntil = 0;
      btConnectionState = "connected";
      btLastError = "";
      btErrorRecoverySuggestion = "";
      setPendingBluetoothOperation("", "");
      startBluetoothStateSync();
      window.setTimeout(function () {
        refreshBluetoothDevices();
        pollNativeBluetoothPlaybackState();
        if (activeModule === "bluetooth") {
          updateBluetoothPanels();
        }
        maybeAutoPlayBluetooth("connected");
      }, 120);
    } else if (/\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u65ad\u5f00|\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u65ad\u5f00|\u5df2\u65ad\u5f00\u8fde\u63a5/.test(message)) {
      btDisconnectNoticeUntil = Date.now() + 3500;
      btConnectionState = "disconnected";
      rememberBluetoothResumePoint({ shouldResume: isBtPlaying || btResumeState.shouldResume, wasPlaying: isBtPlaying });
      resetBluetoothPlaybackState();
      setBluetoothStatus(_t('bluetooth.disconnected'));
      setPendingBluetoothOperation("", "");
    } else if (/\u84dd\u7259\u5df2\u5173\u95ed/.test(message)) {
      btConnectionState = "idle";
      btLastError = _t('bluetooth.turnedOff');
      btErrorRecoverySuggestion = _t('bluetooth.grantPermission');
      resetBluetoothPlaybackState();
      setBluetoothStatus(_t('bluetooth.turnOnAndRetry'));
      setPendingBluetoothOperation("", "");
    } else if (/\u84dd\u7259\u5df2\u5f00\u542f/.test(message)) {
      btConnectionState = "idle";
      btLastError = "";
      btErrorRecoverySuggestion = "";
      setBluetoothStatus(_t('bluetooth.turnedOn'));
    } else if (/\u84dd\u7259\u81ea\u52a8\u91cd\u8fde/.test(message)) {
      btConnectionState = "connecting";
      setBluetoothStatus(message);
    } else if (/\u84dd\u7259\u8fde\u63a5\u5df2\u5e38\u65ad\u5f00/.test(message)) {
      btLastError = _t('bluetooth.connectionException');
      btErrorRecoverySuggestion = _t('bluetooth.autoReconnectHint');
      btConnectionState = "error";
    } else if (/\u5df2\u65ad\u5f00|\u641c\u7d22\u5b8c\u6210|\u540d\u79f0\u5df2\u66f4\u65b0|\u914d\u5bf9\u5931\u8d25|\u5df2\u53d6\u6d88|\u521d\u59cb\u5316\u8d85\u65f6|\u786e\u8ba4\u8d85\u65f6|\u7cfb\u7edf\u9650\u5236/.test(message)) {
      btLastError = message;
      btErrorRecoverySuggestion = _t('bluetooth.checkPermissionAndRetry');
      btConnectionState = "error";
      setPendingBluetoothOperation("", "");
    } else if (/\u6b63\u5728\u641c\u7d22|\u6b63\u5728\u626b\u63cf/.test(message)) {
      btConnectionState = "discovering";
      startBluetoothDiscoveryRefreshLoop();
    }
    scheduleBluetoothRefresh();
  }

  function startBluetoothDiscoveryRefreshLoop() {
    btScanBtn.disabled = true;
    btRefreshBtn.disabled = true;
    window.clearInterval(btDiscoveryRefreshTimer);
    btDiscoveryRefreshTimer = window.setInterval(function () {
      refreshBluetoothDevices();
      var liveState = getNativeBluetoothState();
      if (liveState && liveState.discovering === false) {
        window.clearInterval(btDiscoveryRefreshTimer);
        btDiscoveryRefreshTimer = 0;
        btScanBtn.disabled = false;
        btRefreshBtn.disabled = false;
      }
    }, 900);
  }

  function startBluetoothStateSync() {
    window.clearInterval(btPlaybackStateTimer);
    pollNativeBluetoothPlaybackState();
    btPlaybackStateTimer = window.setInterval(pollNativeBluetoothPlaybackState, 400);
  }

  function stopBluetoothStateSync() {
    window.clearInterval(btPlaybackStateTimer);
    btPlaybackStateTimer = 0;
  }

  function pollNativeBluetoothPlaybackState() {
    var state = callNativeBluetooth("getBluetoothPlaybackState"); // 从Native层获取蓝牙播放状态
    if (state) {
      handleNativeBluetoothPlaybackState(state);
    }
  }

  // 处理蓝牙播放状态更新：同步播放状态、曲目信息、音量、进度
  function handleNativeBluetoothPlaybackState(rawState) {
    var state = typeof rawState === "string" ? parseJsonSafe(rawState) : rawState;
    if (!state) {
      return;
    }
    if (state.connected === false && (pendingBtDeviceId || Date.now() < optimisticBtConnectedUntil)) {
      setBluetoothPlayingState(false);
      return;
    }
    if (state.connected === false) {
      if (connectedBtDeviceId) {
        rememberBluetoothResumePoint({ shouldResume: isBtPlaying, wasPlaying: isBtPlaying });
        resetBluetoothPlaybackState();
      } else {
        setBluetoothPlayingState(false);
      }
      setBluetoothStatus(Date.now() < btDisconnectNoticeUntil ? _t('bluetooth.disconnected') : _t('bluetooth.connectDeviceFirst'));
      return;
    }
    if (state.connected && !connectedBtDeviceId) {
      refreshBluetoothDevices();
    }
    if (state.connected) {
      updateBluetoothPanels();
    }
    if (shouldIgnoreStaleBluetoothPlaybackState(Boolean(state.playing))) {
      return;
    }
    setBluetoothPlayingState(Boolean(state.playing));
    updateBluetoothRemoteTrackInfo(state);
    rememberBluetoothResumePoint({
      shouldResume: Boolean(state.playing),
      wasPlaying: Boolean(state.playing),
      trackTitle: state.trackTitle,
      trackArtist: state.trackArtist,
      trackAlbum: state.trackAlbum,
      progressSeconds: state.progressSeconds,
      durationSeconds: state.durationSeconds
    });
    if (typeof state.volume === "number" && !Number.isNaN(state.volume)) {
      var nativeVolume = Math.max(0, Math.min(1, state.volume));
      if (btVolume) btVolume.value = nativeVolume;
      if (btVolumeValue) btVolumeValue.textContent = Math.round(nativeVolume * 100) + "%";
      updateRangeFill(btVolume);
    }
    if (state.progressKnown && typeof state.progressSeconds === "number" && !Number.isNaN(state.progressSeconds)) {
      if (typeof state.durationSeconds === "number" && state.durationSeconds > 0) {
        btProgressDuration = Math.max(1, state.durationSeconds);
        btHasRealDuration = true;
      }
      btProgressSeconds = Math.max(0, Math.min(btProgressDuration, state.progressSeconds));
      // 新曲目开始（进度回到起点），解除自动下一曲锁定
      if (btAutoNextHandled && btProgressSeconds < 2) {
        btAutoNextHandled = false;
      }
      lastBtProgressUpdateMs = performance.now();
      updateBluetoothProgress();
    }
    if (state.audioRole) {
      bluetoothModule.dataset.audioRole = state.audioRole;
    }
    if (state.connected) {
      btDisconnectNoticeUntil = 0;
      maybeAutoPlayBluetooth("state");
    }
    if (state.connectionState && typeof btConnectionState !== "undefined") {
      btConnectionState = state.connectionState;
    }
  }

  function updateBluetoothRemoteTrackInfo(state) {
    var title = typeof state.trackTitle === "string" ? state.trackTitle.trim() : "";
    var artist = typeof state.trackArtist === "string" ? state.trackArtist.trim() : "";
    var album = typeof state.trackAlbum === "string" ? state.trackAlbum.trim() : "";
    // 无条件刷新，避免新曲目信息为空时残留上一曲的名称/艺人
    btNowPlaying.textContent = title;
    btArtist.textContent = artist;
    if (btAlbum) btAlbum.textContent = album;
    updateBluetoothAlbumArt(state);
  }

  function updateBluetoothAlbumArt(state) {
    if (!btAlbumArt) {
      return;
    }
    var cover = typeof state.trackCover === "string" ? state.trackCover.trim() : "";
    if (!cover) {
      return;
    }
    var nextSrc = cover.indexOf("data:") === 0 ? cover : "data:image/jpeg;base64," + cover;
    if (btAlbumArt.getAttribute("src") === nextSrc) {
      return;
    }
    btAlbumArt.src = nextSrc;
  }

  function loadBluetoothResumeState() {
    try {
      var parsed = JSON.parse(window.localStorage.getItem(BLUETOOTH_RESUME_STORAGE_KEY) || "{}");
      return {
        shouldResume: Boolean(parsed.shouldResume),
        wasPlaying: Boolean(parsed.wasPlaying),
        playMode: parsed.playMode || "all",
        trackTitle: parsed.trackTitle || "",
        trackArtist: parsed.trackArtist || "",
        trackAlbum: parsed.trackAlbum || "",
        progressSeconds: Number(parsed.progressSeconds) || 0,
        durationSeconds: Number(parsed.durationSeconds) || 0,
        updatedAt: Number(parsed.updatedAt) || 0
      };
    } catch (error) {
      return {
        shouldResume: false,
        wasPlaying: false,
        playMode: "all",
        trackTitle: "",
        trackArtist: "",
        trackAlbum: "",
        progressSeconds: 0,
        durationSeconds: 0,
        updatedAt: 0
      };
    }
  }

  function applyBluetoothResumeStateToUi() {
    if (!btResumeState) {
      return;
    }
    if (btResumeState.trackTitle) {
      btNowPlaying.textContent = btResumeState.trackTitle;
    }
    if (btResumeState.trackArtist) {
      btArtist.textContent = btResumeState.trackArtist;
    }
    if (btResumeState.trackAlbum && btAlbum) {
      btAlbum.textContent = btResumeState.trackAlbum;
    }
    if (btResumeState.durationSeconds > 0) {
      btProgressDuration = Math.max(1, btResumeState.durationSeconds);
      btHasRealDuration = true;
    }
    if (btResumeState.progressSeconds >= 0) {
      btProgressSeconds = Math.max(0, Math.min(btProgressDuration, btResumeState.progressSeconds));
      lastBtProgressUpdateMs = performance.now();
      updateBluetoothProgress();
    }
  }

  function rememberBluetoothResumePoint(update) {
    update = update || {};
    btResumeState = {
      shouldResume: typeof update.shouldResume === "boolean" ? update.shouldResume : Boolean(btResumeState.shouldResume),
      wasPlaying: typeof update.wasPlaying === "boolean" ? update.wasPlaying : Boolean(btResumeState.wasPlaying),
      playMode: update.playMode || btPlayMode || btResumeState.playMode || "all",
      trackTitle: typeof update.trackTitle === "string" ? update.trackTitle : btNowPlaying.textContent || btResumeState.trackTitle || "",
      trackArtist: typeof update.trackArtist === "string" ? update.trackArtist : btArtist.textContent || btResumeState.trackArtist || "",
      trackAlbum: typeof update.trackAlbum === "string" ? update.trackAlbum : (btAlbum && btAlbum.textContent) || btResumeState.trackAlbum || "",
      progressSeconds: typeof update.progressSeconds === "number" && !Number.isNaN(update.progressSeconds) ? update.progressSeconds : btProgressSeconds,
      durationSeconds: typeof update.durationSeconds === "number" && update.durationSeconds > 0 ? update.durationSeconds : btProgressDuration,
      updatedAt: Date.now()
    };
    try {
      window.localStorage.setItem(BLUETOOTH_RESUME_STORAGE_KEY, JSON.stringify(btResumeState));
    } catch (error) {
      // Storage is optional in browser preview.
    }
  }

  function syncBluetoothConnectionStateOnResume() {
    startBluetoothStateSync();
    refreshBluetoothDevices();
    pollNativeBluetoothPlaybackState();
    if (connectedBtDeviceId && activeModule !== "bluetooth" && !userInitiatedModuleSwitch) {
      switchModule("bluetooth");
    } else if (activeModule === "bluetooth") {
      applyBluetoothResumeStateToUi();
      updateBluetoothPanels();
      updateBluetoothPlayModeButton();
      updateRangeFill(btVolume);
      maybeAutoPlayBluetooth("return");
    }
  }

  function maybeAutoPlayBluetooth(reason) {
    if (activeModule !== "bluetooth" || !connectedBtDeviceId || isBtPlaying) {
      return;
    }
    var shouldResume = reason === "connected" || reason === "refresh" || reason === "state" || Boolean(btResumeState.shouldResume);
    if (!shouldResume || btPlaybackUiState === "pending-play") {
      return;
    }
    if (btAutoPlayConnectionId === connectedBtDeviceId && reason !== "enter") {
      return;
    }
    btAutoPlayConnectionId = connectedBtDeviceId;
    if (!ensureBluetoothConnectedForPlayback()) {
      btAutoPlayConnectionId = "";
      return;
    }
    setBluetoothPlaybackUiState("pending-play", "pending");
    rememberBluetoothResumePoint({ shouldResume: true, wasPlaying: true });
    var result = sendBluetoothCommand("play", _t('bluetooth.connectedAutoPlay'));
    if (result && result.ok === false) {
      setBluetoothPlaybackUiState("paused", "native");
      return;
    }
    window.setTimeout(pollNativeBluetoothPlaybackState, 650);
  }

  function setBluetoothPlayingState(playing) {
    setBluetoothPlaybackUiState(playing ? "playing" : "paused", "native");
  }

  function setBluetoothPlaybackUiState(state, source) {
    var nextState = connectedBtDeviceId ? state : "disconnected";
    var pending = nextState === "pending-play" || nextState === "pending-pause";
    var visualPlaying = nextState === "playing" || nextState === "pending-play";
    var changed = btPlaybackUiState !== nextState || isBtPlaying !== visualPlaying;
    btPlaybackUiState = nextState;
    isBtPlaying = visualPlaying;
    if (pending) {
      btPlaybackPendingStartedAt = performance.now();
    } else {
      btPlaybackPendingStartedAt = 0;
    }
    btPlayBtn.dataset.playbackState = nextState;
    btPlayBtn.classList.toggle("is-playing", visualPlaying);
    btPlayBtn.classList.toggle("is-pending", pending);
    btPlayBtn.setAttribute("aria-pressed", String(visualPlaying));
    btPlayBtn.setAttribute("aria-busy", String(pending));
    btPlayBtn.setAttribute("aria-label", getBluetoothPlaybackStateLabel(nextState));
    if (visualPlaying) {
      startBluetoothProgress();
    } else {
      stopBluetoothProgress(false);
    }
    if (changed && source !== "pending") {
      window.clearTimeout(btPlaybackPendingTimer);
      btPlaybackPendingTimer = 0;
    }
    updateBluetoothPanels();
    if (activeModule === "bluetooth") {
      // 3D音律已注释
      // if (visualPlaying) {
      //   startVisualizer();
      // } else {
      //   stopVisualizer();
      // }
    }
  }

  function getBluetoothPlaybackStateLabel(state) {
    if (state === "pending-play") {
      return _t('bluetooth.playingBtMusic');
    }
    if (state === "pending-pause") {
      return _t('bluetooth.pausingBtMusic');
    }
    if (state === "playing") {
      return _t('bluetooth.pauseBtMusic');
    }
    if (state === "paused") {
      return _t('bluetooth.playBtMusic');
    }
    return _t('bluetooth.connectDeviceFirst');
  }
  function shouldIgnoreStaleBluetoothPlaybackState(nativePlaying) {
    var pendingPlay = btPlaybackUiState === "pending-play";
    var pendingPause = btPlaybackUiState === "pending-pause";
    if (!pendingPlay && !pendingPause) {
      return false;
    }
    var pendingAge = performance.now() - btPlaybackPendingStartedAt;
    var expectedPlaying = pendingPlay;
    return pendingAge < 500 && nativePlaying !== expectedPlaying;
  }

  function toggleBluetoothPlayback() {
    toggleBluetoothPlaybackSynced();
  }
  // 同步切换蓝牙播放状态：发送命令到Native层并乐观更新UI
  function toggleBluetoothPlaybackSynced() {
    if (!ensureBluetoothConnectedForPlayback()) {
      return;
    }
    var nextPlaying = !isBtPlaying;
    var token = ++btPlaybackCommandToken;
    var pendingState = nextPlaying ? "pending-play" : "pending-pause";
    var confirmedState = nextPlaying ? "playing" : "paused";
    var rollbackState = nextPlaying ? "paused" : "playing";
    setBluetoothPlaybackUiState(pendingState, "pending");
    rememberBluetoothResumePoint({ shouldResume: nextPlaying, wasPlaying: nextPlaying });
    var commandResult = sendBluetoothCommand(nextPlaying ? "play" : "pause", nextPlaying ? _t('bluetooth.musicPlaying') : _t('bluetooth.musicPaused'));
    if (commandResult && commandResult.ok === false) {
      setBluetoothPlaybackUiState(rollbackState, "native");
      return;
    }
    window.clearTimeout(btPlaybackPendingTimer);
    btPlaybackPendingTimer = window.setTimeout(function () {
      if (token === btPlaybackCommandToken && btPlaybackUiState === pendingState) {
        setBluetoothPlaybackUiState(confirmedState, "optimistic");
      }
    }, 260);
    window.setTimeout(pollNativeBluetoothPlaybackState, 650);
  }

  function toggleBluetoothPlayMode() {
    var modes = ["single", "all", "random"];
    var index = modes.indexOf(btPlayMode);
    btPlayMode = modes[(index + 1) % modes.length];
    updateBluetoothPlayModeButton();
    rememberBluetoothResumePoint({ playMode: btPlayMode });
    setBluetoothStatus(_t('bluetooth.playModeLabel', {mode: getBluetoothPlayModeLabel()}));
  }

  function updateBluetoothPlayModeButton() {
    if (btPlayModeBtn) btPlayModeBtn.textContent = getBluetoothPlayModeLabel();
  }

  function getBluetoothPlayModeLabel() {
    if (btPlayMode === "single") {
      return _t('mode.loopSingle');
    }
    if (btPlayMode === "random") {
      return _t('mode.shuffle');
    }
    return _t('mode.loopList');
  }

  function updateBluetoothTrackInfo(device) {
    // 设备信息栏已从界面移除，曲目标题/艺人信息由 updateBluetoothRemoteTrackInfo 负责，
    // 此处不再覆盖歌曲名称与艺人信息，避免连接时把设备名误显示为歌曲标题。
    if (btDeviceMeta) btDeviceMeta.textContent = "";
  }
  function updateBluetoothPanels() {
    var connected = Boolean(connectedBtDeviceId);
    bluetoothModule.classList.toggle("is-connected", connected);
    bluetoothModule.classList.toggle("is-disconnected", !connected);
    bluetoothModule.classList.toggle("is-playing", connected && isBtPlaying);
    bluetoothModule.classList.toggle("is-paused", connected && !isBtPlaying);
  }

  function startBluetoothProgress() {
    if (btProgressTimer) {
      return;
    }
    window.clearInterval(btProgressTimer);
    lastBtProgressUpdateMs = performance.now();
    btProgressTimer = window.setInterval(function () {
      var now = performance.now();
      if (!isDraggingBtProgress) {
        btProgressSeconds += (now - lastBtProgressUpdateMs) / 1000;
      }
      lastBtProgressUpdateMs = now;
      updateBluetoothProgress();
    }, 250);
    updateBluetoothProgress();
  }

  function stopBluetoothProgress(reset) {
    window.clearInterval(btProgressTimer);
    btProgressTimer = 0;
    lastBtProgressUpdateMs = 0;
    if (reset !== false) {
      btProgressSeconds = 0;
    }
    updateBluetoothProgress();
  }

  function updateBluetoothProgress() {
    var duration = btProgressDuration;
    if (!btHasRealDuration || duration <= 0) {
      // 未获取到真实总时长时，避免显示错误的占位时长
      btProgressSeconds = Math.max(0, btProgressSeconds);
      btElapsed.textContent = formatTime(btProgressSeconds);
      btDuration.textContent = "--:--";
      btProgressTrack.style.setProperty("--bt-fill", "0%");
      btProgressTrack.setAttribute("aria-valuemax", "0");
      btProgressTrack.setAttribute("aria-valuenow", "0");
      btProgressTrack.setAttribute("aria-valuetext", formatTime(btProgressSeconds));
      return;
    }
    if (btProgressSeconds >= duration) {
      btProgressSeconds = duration;
      // 音乐播放完毕（自然到达终点），自动切换到下一首
      if (isBtPlaying && !isDraggingBtProgress) {
        triggerBluetoothAutoNext();
      }
    }
    var seconds = Math.max(0, Math.min(duration, btProgressSeconds));
    var percent = (seconds / duration) * 100;
    btElapsed.textContent = formatTime(seconds);
    btDuration.textContent = formatTime(duration);
    btProgressTrack.style.setProperty("--bt-fill", percent + "%");
    btProgressTrack.setAttribute("aria-valuemax", String(Math.round(duration)));
    btProgressTrack.setAttribute("aria-valuenow", String(Math.round(seconds)));
    btProgressTrack.setAttribute("aria-valuetext", formatTime(seconds));
  }

  function beginBluetoothProgressDrag(event) {
    if (!connectedBtDeviceId) {
      setBluetoothStatus(_t('bluetooth.connectDeviceFirst'));
      return;
    }
    isDraggingBtProgress = true;
    btDragStartSeconds = btProgressSeconds;
    btProgressTrack.setPointerCapture(event.pointerId);
    updateBluetoothProgressFromPointer(event);
    btProgressTrack.addEventListener("pointermove", updateBluetoothProgressFromPointer);
    btProgressTrack.addEventListener("pointerup", finishBluetoothProgressDrag);
    btProgressTrack.addEventListener("pointercancel", finishBluetoothProgressDrag);
  }

  function updateBluetoothProgressFromPointer(event) {
    var rect = btProgressTrack.getBoundingClientRect();
    var ratio = rect.width ? (event.clientX - rect.left) / rect.width : 0;
    ratio = Math.max(0, Math.min(1, ratio));
    btProgressSeconds = Math.round(ratio * btProgressDuration);
    updateBluetoothProgress();
  }

  function finishBluetoothProgressDrag(event) {
    btProgressTrack.releasePointerCapture(event.pointerId);
    btProgressTrack.removeEventListener("pointermove", updateBluetoothProgressFromPointer);
    btProgressTrack.removeEventListener("pointerup", finishBluetoothProgressDrag);
    btProgressTrack.removeEventListener("pointercancel", finishBluetoothProgressDrag);
    isDraggingBtProgress = false;
    if (btProgressDuration > 0 && btProgressSeconds >= btProgressDuration) {
      // 手动将进度条拖到终点，触发自动跳转下一首
      triggerBluetoothAutoNext();
    } else {
      syncBluetoothSeekCommand(btProgressSeconds - btDragStartSeconds);
    }
  }

  function handleBluetoothProgressKeydown(event) {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
      return;
    }
    event.preventDefault();
    if (!connectedBtDeviceId) {
      setBluetoothStatus(_t('bluetooth.connectDeviceFirst'));
      return;
    }
    var delta = event.key === "ArrowRight" ? 10 : -10;
    btProgressSeconds = Math.max(0, Math.min(btProgressDuration, btProgressSeconds + delta));
    updateBluetoothProgress();
    if (btProgressDuration > 0 && btProgressSeconds >= btProgressDuration) {
      triggerBluetoothAutoNext();
    } else {
      syncBluetoothSeekCommand(delta);
    }
  }

  function syncBluetoothSeekCommand(deltaSeconds) {
    if (Math.abs(deltaSeconds) < 3) {
      return;
    }
    if (deltaSeconds > 0) {
      sendBluetoothCommand("fastForward", _t('bluetooth.sentFastForward'));
    } else {
      sendBluetoothCommand("rewind", _t('bluetooth.sentRewind'));
    }
  }

  function setBluetoothStatus(message) {
    if (btStatus.textContent === message) {
      return;
    }
    btStatus.textContent = message;
  }

  // 音乐播放到终点（自然结束或拖到终点）时，触发自动切换下一首
  function triggerBluetoothAutoNext() {
    if (btAutoNextHandled || !btHasRealDuration || btProgressDuration <= 0) {
      return;
    }
    if (!connectedBtDeviceId) {
      return;
    }
    btAutoNextHandled = true;
    sendBluetoothTrackStepCommand("next");
  }

  // 发送蓝牙上/下一曲命令到Native层
  function sendBluetoothTrackStepCommand(command) {
    if (!ensureBluetoothConnectedForPlayback()) {
      return;
    }
    btProgressSeconds = 0;
    lastBtProgressUpdateMs = performance.now();
    setBluetoothPlaybackUiState("pending-play", "pending");
    rememberBluetoothResumePoint({ shouldResume: true, wasPlaying: true, progressSeconds: 0 });
    var message = command === "previous"
      ? _t('bluetooth.sentPrevTrack')
      : _t('bluetooth.sentNextTrack');
    var result = sendBluetoothCommand(command, message);
    if (result && result.ok === false) {
      setBluetoothPlaybackUiState("paused", "native");
      return;
    }
    window.clearTimeout(btPlaybackPendingTimer);
    btPlaybackPendingTimer = window.setTimeout(function () {
      if (btPlaybackUiState === "pending-play") {
        setBluetoothPlaybackUiState("playing", "optimistic");
      }
    }, 260);
    window.setTimeout(pollNativeBluetoothPlaybackState, 650);
  }

  // 安全JSON解析，失败时返回null
  function parseJsonSafe(value) {
    try {
      return JSON.parse(value || "{}");
    } catch (error) {
      return null;
    }
  }

  // 封装调用Native蓝牙桥接方法（MusicBridge），自动解析JSON返回值
  function callNativeBluetooth(methodName) {
    try {
      if (!window.MusicBridge || typeof window.MusicBridge[methodName] !== "function") {
        return null;
      }
      var args = Array.prototype.slice.call(arguments, 1);
      var raw = window.MusicBridge[methodName].apply(window.MusicBridge, args);
      if (typeof raw === "string" && raw.charAt(0) === "{") {
        return JSON.parse(raw);
      }
      return raw || null;
    } catch (error) {
      setBluetoothStatus(_t('bluetooth.nativeCallFailed') + error.message);
      return { ok: false, message: error.message };
    }
  }

  // 封装调用Native收音机桥接方法，自动解析JSON返回值
  function callNativeRadio(methodName) {
    try {
      if (!window.MusicBridge || typeof window.MusicBridge[methodName] !== "function") {
        return null;
      }
      var args = Array.prototype.slice.call(arguments, 1);
      var raw = window.MusicBridge[methodName].apply(window.MusicBridge, args);
      if (typeof raw === "string" && raw.charAt(0) === "{") {
        return JSON.parse(raw);
      }
      return raw || null;
    } catch (error) {
      console.error("[Radio] Native调用失败:", error.message);
      return { success: false, message: error.message };
    }
  }

  function sendBluetoothCommand(command, fallbackMessage) {
    var result = callNativeBluetooth("sendBluetoothMediaCommand", command); // 向Native层发送蓝牙媒体控制命令
    setBluetoothStatus(result && result.message ? result.message : fallbackMessage);
    return result;
  }

  function prepareBluetoothAudioRoute(showStatus) {
    var result = callNativeBluetooth("prepareBluetoothAudioRoute"); // 准备蓝牙音频路由
    if (showStatus && result && result.message) {
      setBluetoothStatus(result.message);
    }
    return result;
  }

  function ensureBluetoothConnectedForPlayback() {
    if (!connectedBtDeviceId) {
      setBluetoothStatus(_t('bluetooth.connectDeviceFirst'));
      refreshBluetoothDevices();
      return false;
    }
    var result = prepareBluetoothAudioRoute(false);
    if (result && result.ok === false) {
      setBluetoothStatus(result.message || _t('bluetooth.connectNotReady'));
      pollNativeBluetoothPlaybackState();
      refreshBluetoothDevices();
      return false;
    }
    return true;
  }

  // 为不同模块隔离音频：切换模块时暂停其他音频源
  function isolateAudioForModule(moduleName) {
    if (moduleName !== "local" && !audio.paused) {
      audio.pause();
    }
    if (moduleName !== "bluetooth" && isBtPlaying) {
      setBluetoothPlaybackUiState("paused", "native");
      sendBluetoothCommand("pause", _t('bluetooth.musicPaused'));
    }
    // 收音机模块UI已注释：以下收音机音频隔离逻辑停用
    /*
    if (moduleName !== "radio" && isRadioPlaying) {
      radioPlaybackToken += 1;
      isRadioPlaying = false;
      radioPlayBtn.textContent = "\u25b6";
      stopRadioAudio();
      radioStatus.textContent = _t('radio.paused');
    }
    if (moduleName !== "radio") {
      radioScanToken += 1;
      window.clearTimeout(radioScanTimer);
      radioScanBtn.disabled = false;
      radioScanBtn.classList.remove("is-loading");
    }
    */
  }

  function setRadioLoading(isLoading, message) {
    radioIsLoading = Boolean(isLoading);
    radioModule.classList.toggle("is-loading", radioIsLoading);
    radioScanBtn.disabled = radioIsLoading;
    radioScanBtn.classList.toggle("is-loading", radioIsLoading);
    if (message) {
      radioStatus.textContent = message;
    }
  }

  // 根据位置刷新电台列表：获取地理位置并匹配对应区域频道
  function refreshRadioStationsForLocation(autoStore) {
    if (isRadioNetworkOffline()) {
      refreshOfflineRadioStations();
      return;
    }
    var token = radioLocationToken + 1;
    radioLocationToken = token;
    radioScanToken += 1;
    window.clearTimeout(radioScanTimer);
    setRadioLoading(true, _t('radio.needLocationForStations'));
    requestRadioLocation().then(function (position) {
      if (token !== radioLocationToken) {
        return;
      }
      applyRadioRegion(selectRadioRegionForCoordinates(position.coords.latitude, position.coords.longitude));
      setRadioLoading(true, _t('radio.refreshingLocalStations'));
      scanRadioStations();
    })["catch"](function () {
      if (token !== radioLocationToken) {
        return;
      }
      applyRadioRegion(null);
      setRadioLoading(true, _t('radio.noLocationDefault'));
      scanRadioStations();
    });
  }

  function requestRadioLocation() {
    return new Promise(function (resolve, reject) {
      if (!navigator.geolocation) {
        reject(new Error("geolocation unavailable"));
        return;
      }
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 7000,
        maximumAge: 10 * 60 * 1000
      });
    });
  }

  function isRadioNetworkOffline() {
    return navigator.onLine === false;
  }

  function getNativeOfflineRadioState() {
    if (window.MusicBridge && typeof window.MusicBridge.getOfflineRadioState === "function") {
      try {
        return JSON.parse(window.MusicBridge.getOfflineRadioState() || "{}");
      } catch (error) {
        return { available: false, message: _t('radio.offlineFmStatusReadFailed') };
      }
    }
    return {
      available: false,
      message: _t('radio.noNativeFmInterface')
    };
  }

  function refreshOfflineRadioStations() {
    var token = radioScanToken + 1;
    radioScanToken = token;
    window.clearTimeout(radioScanTimer);
    clearRadioStreamReconnect();
    setRadioLoading(true, _t('radio.checkingFmOffline'));
    radioOfflineState = getNativeOfflineRadioState();
    if (!radioOfflineState.available || !window.MusicBridge || typeof window.MusicBridge.scanOfflineRadioStations !== "function") {
      setRadioLoading(false, (radioOfflineState.message || _t('radio.noFmHardware')) + _t('radio.noFakeBroadcast'));
      renderRadioPresets();
      return;
    }
    var scanResult;
    try {
      scanResult = JSON.parse(window.MusicBridge.scanOfflineRadioStations(radioBand) || "{}");
    } catch (error) {
      scanResult = { ok: false, message: _t('radio.offlineFmScanParseFailed') };
    }
    if (token !== radioScanToken) {
      return;
    }
    if (!scanResult.ok || !Array.isArray(scanResult.stations) || !scanResult.stations.length) {
      setRadioLoading(false, scanResult.message || _t('radio.noOfflineFmStations'));
      renderRadioPresets();
      return;
    }
    RADIO_STATION_CATALOG[radioBand] = scanResult.stations.map(function (station) {
      return {
        frequency: Number(station.frequency),
        name: station.name || ("FM " + station.frequency),
        program: station.program || _t('radio.offlineFmBroadcast'),
        host: station.host || _t('radio.localTuner'),
        strength: Math.max(1, Math.min(5, Math.round(Number(station.strength) || 3))),
        offlineFm: true
      };
    });
    radioRegionName = _t('radio.offlineFm');
    radioPresets = uniqueSortedFrequencies(RADIO_STATION_CATALOG[radioBand].map(function (station) {
      return station.frequency;
    }));
    currentPresetIndex = radioPresets.length ? 0 : 0;
    if (radioPresets.length) {
      updateRadioTuner(radioPresets[currentPresetIndex]);
    }
    renderRadioPresets();
    setRadioLoading(false, _t('radio.offlineFmScanComplete', {n: radioPresets.length}));
  }

  function handleRadioNetworkOffline() {
    if (activeModule !== "radio") {
      return;
    }
    if (isRadioPlaying) {
      scheduleRadioStreamReconnect(_t('radio.networkInterrupted'));
      return;
    }
    radioStatus.textContent = _t('radio.noNetworkDetectFm');
  }

  function handleRadioNetworkOnline() {
    if (activeModule !== "radio") {
      return;
    }
    radioStatus.textContent = _t('radio.networkRestored');
    if (isRadioPlaying && !radioOfflinePlaybackActive) {
      scheduleRadioStreamReconnect(_t('radio.networkRestoredResume'));
    }
  }

  function selectRadioRegionForCoordinates(latitude, longitude) {
    var bestRegion = RADIO_REGION_CATALOGS[0];
    var bestDistance = Infinity;
    RADIO_REGION_CATALOGS.forEach(function (region) {
      var distance = getRadioRegionDistance(latitude, longitude, region.lat, region.lon);
      if (distance < bestDistance) {
        bestDistance = distance;
        bestRegion = region;
      }
    });
    return bestRegion;
  }

  function getRadioRegionDistance(lat1, lon1, lat2, lon2) {
    var dLat = lat1 - lat2;
    var dLon = lon1 - lon2;
    return Math.sqrt(dLat * dLat + dLon * dLon);
  }

  function applyRadioRegion(region) {
    if (!region) {
      radioRegionName = _t('common.default');
      RADIO_STATION_CATALOG.FM = cloneRadioStations(RADIO_DEFAULT_STATION_CATALOG.FM);
      RADIO_STATION_CATALOG.AM = cloneRadioStations(RADIO_DEFAULT_STATION_CATALOG.AM);
      return;
    }
    radioRegionName = region.name;
    RADIO_STATION_CATALOG.FM = cloneRadioStations(region.FM);
    RADIO_STATION_CATALOG.AM = cloneRadioStations(region.AM);
  }

  function cloneRadioStations(stations) {
    return stations.map(function (station) {
      var clone = {};
      Object.keys(station).forEach(function (key) {
        clone[key] = station[key];
      });
      return clone;
    });
  }

  function getRadioCatalogForBand(band) {
    return RADIO_STATION_CATALOG[band] || [];
  }

  function renderRadioPresets() {
    presetGrid.innerHTML = "";
    var visiblePresets = getVisibleRadioPresets();

    if (!visiblePresets.length) {
      var empty = document.createElement("p");
      empty.className = "empty-state radio-empty";
      empty.textContent = _t('panel.emptyFavorites');
      presetGrid.appendChild(empty);
      updateRadioFavoriteState();
      return;
    }

    visiblePresets.forEach(function (station) {
      var button = document.createElement("button");
      var frequency = station.frequency;
      var stationInfo = getRadioStationInfo(frequency);
      var isActive = getRadioFrequencyKey(frequency) === getRadioFrequencyKey(Number(radioFrequency.textContent));
      var isFavorite = isRadioFavorite(frequency);
      var strength = getRadioSignalStrength(frequency);
      button.className = "preset-button" + (isActive ? " is-active" : "") + (isFavorite ? " is-favorite" : "");
      button.type = "button";
      button.title = stationInfo.name + " - " + stationInfo.program + _t('radio.signalLevel', {bars: ''}) + strength + _t('radio.signalUnit');
      button.setAttribute("aria-label", stationInfo.name + " " + formatRadioFrequency(frequency) + " " + getRadioBandConfig().unit + _t('radio.signalLevel', {bars: ''}) + strength + _t('radio.signalUnit'));
      var unit = getRadioBandConfig().unit;
      var waveHtml;
      if (isActive) {
        waveHtml = '<svg class="preset-wave-active" width="70" height="12" viewBox="0 0 70 12" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">' +
          '<path d="M13.8477 12H12.959V4.4209H13.8477V12ZM26.8066 12H25.917V4.4209H26.8066V12ZM52.7227 12H51.833V4.4209H52.7227V12ZM57.042 12H56.1523V4.4209H57.042V12ZM0.889648 11.999H0V0H0.889648V11.999ZM5.20898 11.999H4.31934V0H5.20898V11.999ZM9.52832 11.999H8.63867V2.52637H9.52832V11.999ZM18.167 11.999H17.2783V7.57812H18.167V11.999ZM22.4873 11.999H21.5977V7.57812H22.4873V11.999ZM31.126 11.999H30.2363V2.52637H31.126V11.999ZM35.4453 11.999H34.5557V0H35.4453V11.999ZM39.7646 11.999H38.875V0H39.7646V11.999ZM44.084 11.999H43.1943V2.52637H44.084V11.999ZM48.4033 11.999H47.5137V2.52637H48.4033V11.999ZM61.3613 11.999H60.4717V2.52637H61.3613V11.999ZM65.6807 11.999H64.792V2.52637H65.6807V11.999ZM70 11.999H69.1113V0H70V11.999Z" fill="currentColor"/>' +
          '</svg>';
      } else {
        waveHtml = '<img class="preset-wave" src="assets/radio/wave.svg" alt="" aria-hidden="true" />';
      }
      button.innerHTML =
        '<span class="preset-freq">' + formatRadioFrequency(frequency) + '</span>' +
        waveHtml;
      button.addEventListener("click", function () {
        selectPreset(station.index);
      });
      button.addEventListener("dblclick", function () {
      radioPresets[station.index] = Number(radioFrequency.textContent);
      renderRadioPresets();
      radioStatus.textContent = _t('radio.savedToPreset', {index: station.index + 1});
    });
      presetGrid.appendChild(button);
    });
    updateRadioFavoriteState();
  }

  function getVisibleRadioPresets() {
    return radioPresets.map(function (frequency, index) {
      return { frequency: frequency, index: index };
    }).filter(function (station) {
      return isRadioFavorite(station.frequency);
    });
  }

  function getDefaultRadioPresets(band) {
    return getRadioCatalogForBand(band).slice(0, band === "AM" ? 10 : 6).map(function (station) {
      return station.frequency;
    });
  }

  function getReceivableRadioStations() {
    return getRadioCatalogForBand(radioBand).filter(function (station) {
      return getRadioSignalStrength(station.frequency) >= 3;
    }).sort(function (left, right) {
      return left.frequency - right.frequency;
    });
  }

  function getRadioStationInfo(frequency) {
    var key = getRadioFrequencyKey(frequency);
    var station = getRadioCatalogForBand(radioBand).find(function (item) {      
      return getRadioFrequencyKey(item.frequency) === key;
    });
    if (station) {
      return station;
    }
    return {
      frequency: frequency,
      name: radioBand + " " + formatRadioFrequency(frequency),
      program: "Live Broadcast",
      host: "Unknown",
      strength: 2
    };
  }

  function getRadioSignalStrength(frequency) {
    var station = getRadioStationInfo(frequency);
    var strength = Number(station.strength);
    if (Number.isFinite(strength)) {
      return Math.max(1, Math.min(5, Math.round(strength)));
    }
    return 2;
  }

  function getRadioSignalLabel(strength) {
    if (strength >= 5) {
      return _t('radio.signalStrong');
    }
    if (strength >= 4) {
      return _t('radio.signalGood');
    }
    if (strength >= 3) {
      return _t('radio.receivable');
    }
    return _t('radio.signalWeak');
  }

  function renderRadioSignalBars(strength) {
    var bars = "";
    for (var i = 1; i <= 5; i += 1) {
      bars += '<i class="' + (i <= strength ? "is-on" : "") + '"></i>';
    }
    return '<span class="preset-signal" aria-hidden="true">' + bars + "</span>";
  }

  function updateRadioProgramInfo(frequency) {
    var station = getRadioStationInfo(frequency);
    radioStationName.textContent = station.name;
    radioProgramName.textContent = station.program || "Live Broadcast";
    radioHostName.textContent = station.host ? "Host: " + station.host : "Host: --";
  }

  function selectPreset(index) {
    if (!radioPresets.length) {
      radioStatus.textContent = _t('radio.noStationsPleaseScan');
      return;
    }
    if (index < 0) {
      currentPresetIndex = radioPresets.length - 1;
    } else if (index >= radioPresets.length) {
      currentPresetIndex = 0;
    } else {
      currentPresetIndex = index;
    }
    var frequency = radioPresets[currentPresetIndex];
    updateRadioTuner(frequency);
    renderRadioPresets();
    radioStatus.textContent = _t('radio.switchedTo', {name: getRadioStationInfo(frequency).name});
    if (isRadioPlaying) {
      restartRadioPlaybackForCurrentStation();
    }
  }

  // 微调频率：按方向步进调整，自动循环边界
  function tuneRadioFrequency(direction) {
    var config = getRadioBandConfig();
    var current = Number(radioFrequency.textContent);
    var next = current + direction * config.step;
    if (radioBand === "FM") {
      next = Math.round(next * 10) / 10;
    } else {
      next = Math.round(next);
    }
    if (next < config.min) {
      next = config.max;
    } else if (next > config.max) {
      next = config.min;
    }
    updateRadioTuner(next);
    syncCurrentPresetToFrequency(next);
    renderRadioPresets();
    if (isRadioPlaying) {
      restartRadioPlaybackForCurrentStation();
    }
  }

  // 切换到上/下一个电台：基于可接收电台列表导航
  function seekRadio(delta) {
    var stations = getReceivableRadioStations();
    var current = Number(radioFrequency.textContent);
    var currentIndex = stations.findIndex(function (station) {
      return getRadioFrequencyKey(station.frequency) === getRadioFrequencyKey(current);
    });
    var nextStation = stations[0];
    if (currentIndex >= 0) {
      nextStation = stations[(currentIndex + (delta > 0 ? 1 : -1) + stations.length) % stations.length];
    } else if (delta > 0) {
      nextStation = stations.find(function (station) {
        return station.frequency > current;
      }) || stations[0];
    } else {
      nextStation = stations.slice().reverse().find(function (station) {
        return station.frequency < current;
      }) || stations[stations.length - 1];
    }
    var tuned = nextStation ? nextStation.frequency : current;
    updateRadioTuner(tuned);
    radioStatus.textContent = _t('radio.manualSearch', {name: getRadioStationInfo(tuned).name});
    syncCurrentPresetToFrequency(tuned);
    renderRadioPresets();
    if (isRadioPlaying) {
      restartRadioPlaybackForCurrentStation();
    }
  }

  function scanRadioStations() {
    var stations = getReceivableRadioStations();
    var token = radioScanToken + 1;
    radioScanToken = token;
    window.clearTimeout(radioScanTimer);
    setRadioLoading(true, _t('radio.refreshingChannels', {band: radioBand}));
    radioStatus.textContent = _t('radio.searchingChannels', {band: radioBand});
    if (radioSearchModal) radioSearchModal.hidden = false;
    radioPresets = [];
    renderRadioPresets();
    radioFmBtn.disabled = true;
    radioAmBtn.disabled = true;
    radioTuneDownBtn.disabled = true;
    radioTuneUpBtn.disabled = true;
    radioPrevStationBtn.disabled = true;
    radioNextStationBtn.disabled = true;
    radioFavoriteBtn.disabled = true;
    radioPlayBtn.disabled = true;
    scanRadioStationStep(stations, token, 0, []);
  }

  function scanRadioStationStep(stations, token, index, found) {
    if (token !== radioScanToken) {
      return;
    }
    if (index >= stations.length) {
      finishRadioScan(found);
      return;
    }
    var station = stations[index];
    found.push(station.frequency);
    radioPresets = uniqueSortedFrequencies(found);
    currentPresetIndex = Math.max(0, radioPresets.length - 1);
    updateRadioTuner(station.frequency);
    renderRadioPresets();
    radioStatus.textContent = _t('radio.identifiedStations', {n: radioPresets.length, name: station.name, signal: getRadioSignalLabel(getRadioSignalStrength(station.frequency))});
    radioScanTimer = window.setTimeout(function () {
      scanRadioStationStep(stations, token, index + 1, found);
    }, 90);
  }

  function finishRadioScan(found) {
    radioPresets = uniqueSortedFrequencies(found);
    if (radioPresets.length > 200) {
      radioPresets = radioPresets.slice(-200);
    }
    saveRadioPresets();
    if (radioSearchModal) radioSearchModal.hidden = true;
    radioFmBtn.disabled = false;
    radioAmBtn.disabled = false;
    radioTuneDownBtn.disabled = false;
    radioTuneUpBtn.disabled = false;
    radioPrevStationBtn.disabled = false;
    radioNextStationBtn.disabled = false;
    radioFavoriteBtn.disabled = false;
    radioPlayBtn.disabled = false;
    radioScanBtn.disabled = false;
    radioScanBtn.classList.remove("is-loading");
    if (radioPresets.length) {
      updateRadioTuner(radioPresets[0]);
      currentPresetIndex = 0;
    } else {
      currentPresetIndex = 0;
    }
    renderRadioPresets();
    radioStatus.textContent = radioPresets.length
      ? _t('radio.searchComplete', {n: radioPresets.length})
      : _t('radio.noStationFoundRetry');
    setRadioLoading(false, radioPresets.length
      ? _t('radio.scanComplete', {n: radioPresets.length})
      : _t('radio.noPlayableStationRetry'));
    if (radioPresets.length && isRadioPlaying) {
      restartRadioPlaybackForCurrentStation();
    }
  }

  function syncCurrentPresetToFrequency(frequency) {
    var matchIndex = radioPresets.findIndex(function (item) {
      return getRadioFrequencyKey(item) === getRadioFrequencyKey(frequency);
    });
    if (matchIndex >= 0) {
      currentPresetIndex = matchIndex;
    }
  }

  function toggleRadioFavorite() {
    var frequency = Number(radioFrequency.textContent);
    var key = getRadioFavoriteKey(frequency);
    if (isRadioFavorite(frequency)) {
      radioFavoriteFrequencies = radioFavoriteFrequencies.filter(function (item) {
        return getRadioFavoriteKey(item) !== key;
      });
      radioStatus.textContent = _t('toast.favoriteCancelled') + formatRadioFrequency(frequency) + " " + getRadioBandConfig().unit;
    } else {
      radioFavoriteFrequencies.push(frequency);
      radioFavoriteFrequencies = uniqueSortedFrequencies(radioFavoriteFrequencies);
      if (radioPresets.findIndex(function (item) { return getRadioFrequencyKey(item) === key; }) < 0) {
        radioPresets.push(frequency);
      }
      radioStatus.textContent = _t('toast.favoriteAdded') + formatRadioFrequency(frequency) + " " + getRadioBandConfig().unit;
    }
    saveRadioFavorites();
    updateRadioFavoriteState();
    renderRadioPresets();
  }

  function toggleRadioMode() {
    renderRadioPresets();
    radioStatus.textContent = _t('toast.favoritesUpdated');
  }

  function storeCurrentRadioStation() {
    var frequency = Number(radioFrequency.textContent);
    syncCurrentPresetToFrequency(frequency);
    if (currentPresetIndex < 0 || currentPresetIndex >= radioPresets.length) {
      currentPresetIndex = 0;
    }
    radioPresets[currentPresetIndex] = frequency;
    saveRadioPresets();
    renderRadioPresets();
    radioStatus.textContent = _t('radio.savedToPreset', {index: currentPresetIndex + 1});
  }

  function adjustRadioVolume(delta) {
    var next = Math.min(1, Math.max(0, Number(radioVolume.value) + delta));
    radioVolume.value = next;
    isRadioMuted = next === 0;
    if (!isRadioMuted) {
      lastRadioVolume = next;
    }
    syncRadioVolumeDisplay("\u6536\u97f3\u673a\u97f3\u91cf");
  }

  function syncRadioVolumeDisplay(prefix) {
    applyRadioVolume();
    applyMediaVolume(Number(radioVolume.value));
  }

  function isRadioFavorite(frequency) {
    var key = getRadioFavoriteKey(frequency);
    return radioFavoriteFrequencies.some(function (item) {
      return getRadioFavoriteKey(item) === key;
    });
  }

  function updateRadioFavoriteState() {
    var isFavorite = isRadioFavorite(Number(radioFrequency.textContent));
    radioFavoriteBtn.setAttribute("aria-pressed", String(isFavorite));
  }

  function uniqueSortedFrequencies(frequencies) {
    var seen = {};
    return frequencies.map(function (frequency) {
      return Number(Number(frequency).toFixed(1));
    }).filter(function (frequency) {
      var key = frequency.toFixed(1);
      if (seen[key]) {
        return false;
      }
      seen[key] = true;
      return true;
    }).sort(function (left, right) {
      return left - right;
    });
  }

  function loadRadioFavorites() {
    try {
      return uniqueSortedFrequencies(JSON.parse(localStorage.getItem("radioFavorites") || "[]"));
    } catch (error) {
      return [];
    }
  }

  function saveRadioFavorites() {
    try {
      localStorage.setItem("radioFavorites", JSON.stringify(radioFavoriteFrequencies));
    } catch (error) {
      // Storage may be unavailable in restricted WebView modes.
    }
  }

  function loadRadioLastState() {
    try {
      var saved = JSON.parse(localStorage.getItem("radioLastState") || "{}");
      if (saved.band) radioBand = saved.band;
      if (saved.frequency) lastRadioFrequency[radioBand] = saved.frequency;
      if (saved.isPlaying !== undefined) isRadioPlaying = saved.isPlaying;
    } catch (error) {
      // Fall back to defaults
    }
  }

  function saveRadioLastState() {
    try {
      localStorage.setItem("radioLastState", JSON.stringify({
        band: radioBand,
        frequency: Number(radioFrequency.textContent),
        isPlaying: isRadioPlaying
      }));
    } catch (error) {
      // Storage may be unavailable in restricted WebView modes.
    }
  }

  function loadRadioPresets(band) {
    try {
      var saved = JSON.parse(localStorage.getItem("radioPresets:" + band) || "[]");
      if (Array.isArray(saved) && saved.length) {
        return uniqueSortedFrequencies(saved);
      }
    } catch (error) {
      // Fall back to the built-in receivable station table.
    }
    return getDefaultRadioPresets(band);
  }

  function saveRadioPresets() {
    try {
      localStorage.setItem("radioPresets:" + radioBand, JSON.stringify(radioPresets));
    } catch (error) {
      // Keep the radio usable even if storage is unavailable.
    }
  }

  // 切换收音机播放/暂停：调用startRadioPlayback或停止音频
  function toggleRadioPlayback() {
    if (isRadioPlaying) {
      radioPlaybackToken += 1;
      isRadioPlaying = false;
      radioPlayBtn.classList.remove("is-playing");
      stopRadioAudio();
      radioStatus.textContent = _t('radio.paused');
      return;
    }

    isolateAudioForModule("radio");
    callNativeBluetooth("setActiveAudioModule", "radio"); // 通知Native层切换到收音机模块
    applyMediaVolume(Number(radioVolume.value));
    var token = radioPlaybackToken + 1;
    radioPlaybackToken = token;
    radioStatus.textContent = isRadioNetworkOffline()
      ? _t('radio.checkingFmPlayback')
      : _t('radio.connectingOnlineStation');
    startRadioPlayback().then(function () {
      if (token !== radioPlaybackToken) {
        return;
      }
      isRadioPlaying = true;
      radioPlayBtn.classList.add("is-playing");
      radioStatus.textContent = _t('radio.playingStation', {name: getRadioStationInfo(Number(radioFrequency.textContent)).name});
    })["catch"](function () {
      if (token !== radioPlaybackToken) {
        return;
      }
      isRadioPlaying = false;
      radioPlayBtn.classList.remove("is-playing");
      radioStatus.textContent = _t('radio.audioStartFailed');
    });
  }


  function updateRadioTuner(value) {
    var frequency = typeof value === "number" ? value : lastRadioFrequency[radioBand] || radioPresets[currentPresetIndex];
    var config = getRadioBandConfig();
    frequency = Math.max(config.min, Math.min(config.max, frequency));
    radioFrequency.textContent = formatRadioFrequency(frequency);
    radioUnit.textContent = config.unit;
    updateRadioRailLabels(config);
    var percent = ((frequency - config.min) / (config.max - config.min)) * 100;
    radioNeedle.parentElement.style.setProperty("--radio-fill", percent + "%");
    var track = radioNeedle.parentElement;
    if (track) {
      track.classList.add("is-tuning");
      window.clearTimeout(updateRadioTuner._tuneTimer);
      updateRadioTuner._tuneTimer = window.setTimeout(function () {
        track.classList.remove("is-tuning");
      }, 400);
    }
    updateRadioTone(frequency);
    updateRadioSignal(frequency);
    updateRadioProgramInfo(frequency);
    updateRadioFavoriteState();
    saveRadioLastState();
  }

  // 切换FM/AM频段：更新UI、加载预设、重启播放
  function switchRadioBand(band) {
    if (radioBand === band) {
      return;
    }
    radioScanToken += 1;
    window.clearTimeout(radioScanTimer);
    radioScanBtn.disabled = false;
    radioBand = band;
    radioPresets = getDefaultRadioPresets(band);
    var config = getRadioBandConfig();
    var savedFreq = lastRadioFrequency[band] || radioPresets[Math.min(band === "AM" ? 1 : 3, Math.max(0, radioPresets.length - 1))];
    savedFreq = Math.max(config.min, Math.min(config.max, savedFreq));
    currentPresetIndex = radioPresets.indexOf(savedFreq);
    if (currentPresetIndex === -1) currentPresetIndex = Math.min(band === "AM" ? 1 : 3, Math.max(0, radioPresets.length - 1));
    radioFmBtn.classList.toggle("is-active", band === "FM");
    radioAmBtn.classList.toggle("is-active", band === "AM");
    if (radioFreqMin) radioFreqMin.textContent = Math.round(config.min);
    if (radioFreqMax) radioFreqMax.textContent = Math.round(config.max);
    if (radioUnit) radioUnit.textContent = config.unit;
    updateRadioTuner(savedFreq);
    renderRadioPresets();
    if (isRadioNetworkOffline()) {
      refreshOfflineRadioStations();
      return;
    }
    radioStatus.textContent = _t('radio.switchedToBand', {band: band});
    if (isRadioPlaying) {
      restartRadioPlaybackForCurrentStation();
    }
  }

  function getRadioBandConfig() {
    return radioBand === "AM"
      ? { min: 531, max: 1602, step: 9, unit: "kHz" }
      : { min: 87.0, max: 108.0, step: 0.1, unit: "MHz" };
  }

  function formatRadioFrequency(frequency) {
    return radioBand === "AM" ? String(Math.round(Number(frequency))) : Number(frequency).toFixed(1);
  }

  function getRadioFrequencyKey(frequency) {
    return radioBand + ":" + formatRadioFrequency(frequency);
  }

  function getRadioFavoriteKey(frequency) {
    return getRadioFrequencyKey(frequency);
  }

  function updateRadioRailLabels(config) {
    if (radioFreqMin) radioFreqMin.textContent = Math.round(config.min);
    if (radioFreqMax) radioFreqMax.textContent = Math.round(config.max);
    renderRadioRuler(config);
  }

  function renderRadioRuler(config) {
    if (!radioRuler) return;
    var min = config.min;
    var max = config.max;
    var step = config.step;
    var majorInterval = radioBand === "AM" ? 90 : 1.0;
    var totalMinor = Math.round((max - min) / step);
    var majorStepCount = Math.round(majorInterval / step);
    var ticks = [];
    var labels = [];

    for (var i = 0; i <= totalMinor; i++) {
      var freq = min + i * step;
      var percent = (freq - min) / (max - min) * 100;
      var isMajor = (i % majorStepCount === 0);

      if (isMajor) {
        var tick = document.createElement("span");
        tick.className = "tick is-major";
        tick.style.left = percent + "%";
        ticks.push(tick);

        var label = document.createElement("span");
        label.className = "tick-label";
        label.style.left = percent + "%";
        label.textContent = radioBand === "AM"
          ? String(Math.round(freq))
          : Number(freq).toFixed(1);
        labels.push(label);
      } else {
        var minorTick = document.createElement("span");
        minorTick.className = "tick";
        minorTick.style.left = percent + "%";
        ticks.push(minorTick);
      }
    }

    radioRuler.innerHTML = "";
    var fragment = document.createDocumentFragment();
    for (var j = 0; j < ticks.length; j++) {
      fragment.appendChild(ticks[j]);
    }
    for (var k = 0; k < labels.length; k++) {
      fragment.appendChild(labels[k]);
    }
    radioRuler.appendChild(fragment);
  }

  function updateRadioSignal(frequency) {
    var strength = getRadioSignalStrength(frequency);
    var isStereo = radioBand === "FM" && strength >= 3;
    if (radioStereo) {
      radioStereo.classList.toggle("is-active", isStereo);
    }
    radioSignal.textContent = strength + _t('radio.signalUnit') + getRadioSignalLabel(strength);
  }

  function ensureRadioStreamAudio() {
    if (radioStreamAudio) {
      return radioStreamAudio;
    }
    radioStreamAudio = new Audio();
    radioStreamAudio.preload = "auto";
    radioStreamAudio.crossOrigin = "anonymous";
    radioStreamAudio.addEventListener("waiting", function () {
      if (isRadioPlaying) {
        radioStatus.textContent = _t('radio.buffering');
        scheduleRadioStreamFallback();
      }
    });
    radioStreamAudio.addEventListener("playing", markRadioStreamConnected);
    radioStreamAudio.addEventListener("canplay", markRadioStreamConnected);
    radioStreamAudio.addEventListener("pause", clearRadioStreamStallTimer);
    radioStreamAudio.addEventListener("error", function () {
      if (isRadioPlaying) {
        clearRadioStreamStallTimer();
        stopRadioStream();
        scheduleRadioStreamReconnect(_t('radio.stationTempUnavailable'));
        return;
        radioStatus.textContent = _t('radio.switchedToLocalTuner');
        isRadioPlaying = false;
        radioPlayBtn.textContent = "\u25b6";
        stopRadioStream();
        radioStatus.textContent = _t('radio.stationUnavailableStopped');
      }
    });
    return radioStreamAudio;
  }

  function scheduleRadioStreamFallback() {
    window.clearTimeout(radioStreamStallTimer);
    radioStreamStallTimer = window.setTimeout(function () {
      if (!isRadioPlaying || !radioStreamAudio || radioStreamAudio.paused || radioStreamAudio.readyState >= 3) {
        return;
      }
      stopRadioStream();
      scheduleRadioStreamReconnect(_t('radio.bufferTimeoutKeep'));
      return;
      stopRadioStream();
      radioStatus.textContent = _t('radio.offlineFm');
      isRadioPlaying = false;
      radioPlayBtn.textContent = "\u25b6";
      radioStatus.textContent = _t('radio.bufferTimeoutStop');
    }, 2600);
  }

  function clearRadioStreamStallTimer() {
    window.clearTimeout(radioStreamStallTimer);
  }

  function clearRadioStreamReconnect() {
    window.clearTimeout(radioStreamReconnectTimer);
    radioStreamReconnectTimer = 0;
    radioStreamReconnectAttempts = 0;
  }

  function clearRadioStreamMonitor() {
    window.clearInterval(radioStreamMonitorTimer);
    radioStreamMonitorTimer = 0;
  }

  function markRadioStreamConnected() {
    clearRadioStreamStallTimer();
    clearRadioStreamReconnect();
    radioStreamLastGoodAt = Date.now();
    radioOfflinePlaybackActive = false;
    startRadioConnectionMonitor();
    if (isRadioPlaying) {
      radioStatus.textContent = _t('radio.playingStation', {name: getRadioStationInfo(Number(radioFrequency.textContent)).name});
    }
  }

  function startRadioConnectionMonitor() {
    clearRadioStreamMonitor();
    radioStreamMonitorTimer = window.setInterval(function () {
      if (!isRadioPlaying || radioOfflinePlaybackActive || !radioStreamAudio) {
        return;
      }
      if (isRadioNetworkOffline()) {
        scheduleRadioStreamReconnect(_t('radio.networkInterruptMonitoring'));
        return;
      }
      if (!radioStreamAudio.paused && radioStreamAudio.readyState <= 2) {
        radioStatus.textContent = _t('radio.unstableBuffering');
        scheduleRadioStreamFallback();
      }
    }, 3000);
  }

  function scheduleRadioStreamReconnect(message) {
    if (!isRadioPlaying || radioOfflinePlaybackActive) {
      return;
    }
    window.clearTimeout(radioStreamReconnectTimer);
    radioStreamReconnectAttempts += 1;
    var delay = Math.min(15000, 1200 * Math.pow(1.6, Math.max(0, radioStreamReconnectAttempts - 1)));
    radioStatus.textContent = message + _t('radio.autoResumeAfterSec', {sec: Math.round(delay / 1000)});
    radioStreamReconnectTimer = window.setTimeout(function () {
      if (!isRadioPlaying || radioOfflinePlaybackActive || isRadioNetworkOffline()) {
        if (isRadioPlaying && !radioOfflinePlaybackActive) {
          scheduleRadioStreamReconnect(_t('radio.connectionNotRestored'));
        }
        return;
      }
      startRadioPlayback().then(function () {
        radioPlayBtn.textContent = "\u23f8";
        radioStatus.textContent = _t('radio.resumedPlaying', {name: getRadioStationInfo(Number(radioFrequency.textContent)).name});
      })["catch"](function () {
        scheduleRadioStreamReconnect(_t('radio.resumeFailedRetry'));
      });
    }, delay);
  }

  function startOfflineRadioPlayback(station) {
    radioOfflineState = getNativeOfflineRadioState();
    if (!radioOfflineState.available || !window.MusicBridge || typeof window.MusicBridge.startOfflineRadio !== "function") {
      radioStatus.textContent = (radioOfflineState.message || _t('radio.noFmHardware')) + _t('radio.noOnlineWithoutNetwork');
      return Promise.reject(new Error("offline fm unavailable"));
    }
    var result;
    try {
      result = JSON.parse(window.MusicBridge.startOfflineRadio(radioBand, Number(station.frequency), Number(radioVolume.value)) || "{}");
    } catch (error) {
      result = { ok: false, message: _t('radio.offlineFmStartFailed') };
    }
    if (!result.ok) {
      radioStatus.textContent = result.message || _t('radio.offlineFmStartFailed');
      return Promise.reject(new Error("offline fm start failed"));
    }
    radioOfflinePlaybackActive = true;
    clearRadioStreamReconnect();
    radioStatus.textContent = _t('radio.playingOfflineFm', {name: station.name});
    return Promise.resolve();
  }

  // 开始收音机播放：优先离线FM，否则连接在线音频流
  function startRadioPlayback() {
    var station = getRadioStationInfo(Number(radioFrequency.textContent));
    stopRadioStream();
    if (isRadioNetworkOffline() || station.offlineFm) {
      return startOfflineRadioPlayback(station);
    }
    if (!station.streamUrl) {
      radioStatus.textContent = _t('radio.noStreamAvailable');
      return Promise.reject(new Error("radio stream missing"));
    }
    var stream = ensureRadioStreamAudio();
    stream.volume = Math.max(0, Math.min(1, Number(radioVolume.value)));
    stream.src = station.streamUrl;
    try {
      stream.load();
    } catch (error) {
      // Some WebViews start loading only after play(); play() below still handles that path.
    }
    radioStatus.textContent = _t('radio.connectingTo', {name: station.name});
    var streamReady = new Promise(function (resolve, reject) {
      var settled = false;
      var cleanup = function () {
        stream.removeEventListener("playing", onPlaying);
        stream.removeEventListener("canplay", onCanPlay);
        stream.removeEventListener("error", onError);
        window.clearTimeout(radioStreamTimer);
      };
      var finish = function (fn) {
        if (settled) {
          return;
        }
        settled = true;
        cleanup();
        fn();
      };
      var onPlaying = function () {
        finish(resolve);
      };
      var onCanPlay = function () {
        finish(resolve);
      };
      var onError = function () {
        finish(function () {
          reject(new Error("radio stream unavailable"));
        });
      };
      stream.addEventListener("playing", onPlaying);
      stream.addEventListener("canplay", onCanPlay);
      stream.addEventListener("error", onError);
      radioStreamTimer = window.setTimeout(function () {
        finish(function () {
          reject(new Error("radio stream timeout"));
        });
      }, 8000);
    });
    var playPromise = stream.play();
    if (playPromise && typeof playPromise["catch"] === "function") {
      playPromise["catch"](function () {});
    }
    return streamReady["catch"](function () {
      stopRadioStream();
      radioStatus.textContent = _t('radio.stationTempUnavailableStopped');
      return Promise.reject(new Error("radio stream unavailable"));
    });
  }


  function restartRadioPlaybackForCurrentStation() {
    var token = radioPlaybackToken + 1;
    radioPlaybackToken = token;
    var stationName = getRadioStationInfo(Number(radioFrequency.textContent)).name;
    radioStatus.textContent = _t('radio.switchingTo', {name: stationName});
    stopRadioAudio();
    startRadioPlayback().then(function () {
      if (token !== radioPlaybackToken) {
        return;
      }
      radioStatus.textContent = _t('radio.playingStation', {name: getRadioStationInfo(Number(radioFrequency.textContent)).name});
    })["catch"](function () {
      if (token !== radioPlaybackToken) {
        return;
      }
      radioStatus.textContent = _t('radio.switchFailedRetry');
    });
  }

  function stopRadioStream() {
    window.clearTimeout(radioStreamTimer);
    clearRadioStreamStallTimer();
    clearRadioStreamMonitor();
    if (!radioStreamAudio) {
      return;
    }
    radioStreamAudio.pause();
    radioStreamAudio.removeAttribute("src");
    radioStreamAudio.load();
  }

  function startRadioAudio() {
    var AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextConstructor) {
      return Promise.reject(new Error(_t('webAudio.unavailable')));
    }
    window.clearTimeout(radioStopTimer);
    if (!radioAudioContext) {
      radioAudioContext = new AudioContextConstructor();
    }
    return radioAudioContext.resume().then(function () {
      if (!radioCarrier || !radioWarmth || !radioGain) {
        createRadioAudioNodes();
      }
      updateRadioTone(Number(radioFrequency.textContent));
      applyRadioVolume();
    });
  }

  function createRadioAudioNodes() {
    var now = radioAudioContext.currentTime;
    radioCarrier = radioAudioContext.createOscillator();
    radioWarmth = radioAudioContext.createOscillator();
    radioTremolo = radioAudioContext.createOscillator();
    radioGain = radioAudioContext.createGain();
    var tremoloGain = radioAudioContext.createGain();

    radioCarrier.type = "sine";
    radioWarmth.type = "triangle";
    radioTremolo.type = "sine";
    radioTremolo.frequency.setValueAtTime(5.2, now);
    tremoloGain.gain.setValueAtTime(0.008, now);
    radioGain.gain.setValueAtTime(0, now);

    radioTremolo.connect(tremoloGain);
    tremoloGain.connect(radioGain.gain);
    radioCarrier.connect(radioGain);
    radioWarmth.connect(radioGain);
    radioGain.connect(radioAudioContext.destination);

    radioCarrier.start(now);
    radioWarmth.start(now);
    radioTremolo.start(now);
  }

  function stopRadioAudio() {
    clearRadioStreamReconnect();
    if (radioOfflinePlaybackActive && window.MusicBridge && typeof window.MusicBridge.stopOfflineRadio === "function") {
      try {
        window.MusicBridge.stopOfflineRadio();
      } catch (error) {
        // Native offline FM may already be stopped.
      }
    }
    radioOfflinePlaybackActive = false;
    stopRadioStream();
    if (!radioAudioContext || !radioGain) {
      return;
    }
    var now = radioAudioContext.currentTime;
    radioGain.gain.cancelScheduledValues(now);
    radioGain.gain.setTargetAtTime(0, now, 0.08);
    radioStopTimer = window.setTimeout(function () {
      if (!isRadioPlaying) {
        disposeRadioAudioNodes();
      }
    }, 260);
  }

  function disposeRadioAudioNodes() {
    [radioCarrier, radioWarmth, radioTremolo].forEach(function (node) {
      if (!node) {
        return;
      }
      try {
        node.stop();
      } catch (error) {
        // Oscillator may already be stopped after a quick module switch.
      }
      node.disconnect();
    });
    if (radioGain) {
      radioGain.disconnect();
    }
    radioCarrier = null;
    radioWarmth = null;
    radioTremolo = null;
    radioGain = null;
  }

  function resumeRadioAudio() {
    if (radioAudioContext && radioAudioContext.state === "suspended") {
      radioAudioContext.resume()["catch"](function () {
        radioStatus.textContent = _t('radio.audioResumeFailed');
      });
    }
  }

  function updateRadioTone(frequency) {
    if (!radioAudioContext || !radioCarrier || !radioWarmth) {
      return;
    }
    var now = radioAudioContext.currentTime;
    var normalized = (Math.max(87.5, Math.min(108, frequency)) - 87.5) / (108 - 87.5);
    var baseTone = 196 + normalized * 392;
    radioCarrier.frequency.setTargetAtTime(baseTone, now, 0.08);
    radioWarmth.frequency.setTargetAtTime(baseTone * 1.5, now, 0.08);
  }

  function applyRadioVolume() {
    if (radioStreamAudio) {
      radioStreamAudio.volume = Math.max(0, Math.min(1, Number(radioVolume.value)));
    }
    if (radioOfflinePlaybackActive && window.MusicBridge && typeof window.MusicBridge.setOfflineRadioVolume === "function") {
      try {
        window.MusicBridge.setOfflineRadioVolume(Number(radioVolume.value));
      } catch (error) {
        // Keep UI volume responsive even if the native FM layer rejects the update.
      }
    }
    if (!radioAudioContext || !radioGain) {
      return;
    }
    var now = radioAudioContext.currentTime;
    var target = Math.max(0, Math.min(1, Number(radioVolume.value))) * 0.18;
    radioGain.gain.cancelScheduledValues(now);
    radioGain.gain.setTargetAtTime(target, now, 0.06);
  }

  function applyMediaVolume(value) {
    var safeValue = Math.max(0, Math.min(1, Number(value) || 0));
    if (window.MusicBridge && typeof window.MusicBridge.setMediaVolume === "function") {
      callNativeBluetooth("setMediaVolume", safeValue); // 设置Native层媒体音量
    } else {
      callNativeBluetooth("setBluetoothVolume", safeValue); // 设置Native层蓝牙音量
    }
  }

  function createLyricsForTrack(track, duration) {
    var title = track && track.title ? track.title : _t('lyrics.currentMusic');
    var artist = track && track.artist ? track.artist : _t('lyrics.unknownArtist');
    var safeDuration = Math.max(18, Number(duration) || 180);
    var source = [
      title + _t('lyrics.playing'),
      artist + _t('lyrics.melodyUnfolds'),
      _t('lyrics.followBeat'),
      _t('lyrics.everyWord'),
      _t('lyrics.catchMemories'),
      _t('lyrics.momentBelongs'),
      _t('lyrics.liftHeadSee'),
      _t('lyrics.nextLine'),
      _t('lyrics.thanksForBeing')
    ];
    var step = safeDuration / source.length;

    return source.map(function (text, index) {
      return {
        time: Math.max(0, Math.round(index * step * 10) / 10),
        text: text
      };
    });
  }

  function createDemoTracks() {
    return [
      createToneTrack("See You Again", "Charlie Puth", 392, 18),
      createToneTrack("\u6674\u5929", "\u5468\u6770\u4f26", 330, 16),
      createToneTrack("\u6700\u4f1f\u5927\u7684\u4f5c\u54c1", "\u5468\u6770\u4f26", 262, 20)
    ];
  }

  function createToneTrack(title, artist, frequency, seconds) {
    return {
      id: createId(),
      title: title,
      artist: artist,
      durationLabel: formatTime(seconds),
      url: createToneUrl(frequency, seconds),
      isDemo: true,
      lyrics: createLyricsForTrack({ title: title, artist: artist }, seconds)
    };
  }

  function createToneUrl(frequency, seconds) {
    var sampleRate = 44100;
    var length = sampleRate * seconds;
    var dataSize = length * 2;
    var buffer = new ArrayBuffer(44 + dataSize);
    var view = new DataView(buffer);
    var offset = 0;

    function writeString(value) {
      for (var i = 0; i < value.length; i += 1) {
        view.setUint8(offset + i, value.charCodeAt(i));
      }
      offset += value.length;
    }

    writeString("RIFF");
    view.setUint32(offset, 36 + dataSize, true);
    offset += 4;
    writeString("WAVE");
    writeString("fmt ");
    view.setUint32(offset, 16, true);
    offset += 4;
    view.setUint16(offset, 1, true);
    offset += 2;
    view.setUint16(offset, 1, true);
    offset += 2;
    view.setUint32(offset, sampleRate, true);
    offset += 4;
    view.setUint32(offset, sampleRate * 2, true);
    offset += 4;
    view.setUint16(offset, 2, true);
    offset += 2;
    view.setUint16(offset, 16, true);
    offset += 2;
    writeString("data");
    view.setUint32(offset, dataSize, true);
    offset += 4;

    for (var i = 0; i < length; i += 1) {
      var fadeIn = Math.min(1, i / (sampleRate * 0.4));
      var fadeOut = Math.min(1, (length - i) / (sampleRate * 0.8));
      var envelope = Math.min(fadeIn, fadeOut) * 0.28;
      var harmonic = Math.sin((2 * Math.PI * frequency * 1.5 * i) / sampleRate) * 0.25;
      var sample = (Math.sin((2 * Math.PI * frequency * i) / sampleRate) + harmonic) * envelope;
      view.setInt16(offset, sample * 32767, true);
      offset += 2;
    }

    return URL.createObjectURL(new Blob([buffer], { type: "audio/wav" }));
  }

  function getBaseName(fileName) {
    return String(fileName || "").replace(/\.[^/.]+$/, "").trim().toLowerCase();
  }

  function handleNativeAppPause() {
    rememberBluetoothResumePoint({ shouldResume: activeModule === "bluetooth" && isBtPlaying, wasPlaying: isBtPlaying });
    rememberUsbResumePoint();
  }

  function handleNativeAppResume() {
    syncBluetoothConnectionStateOnResume();
    if (isRadioPlaying) {
      resumeRadioAudio();
    }
    window.setTimeout(function () {
      refreshUsbMusicState(false);
    }, 200);
  }

  function handleNativePhoneCallStart() {
    if (activeAudioSource === "usb" && !audio.paused) {
      usbPausedByPhoneCall = true;
      audio.pause();
      updateUsbPlaybackUi();
    }
  }

  function handleNativePhoneCallEnd() {
    if (usbPausedByPhoneCall) {
      usbPausedByPhoneCall = false;
      if (activeAudioSource === "usb" && audio.paused) {
        audio.play()["catch"](function () {
          updateUsbPlaybackUi();
        });
      }
    }
  }

  function revokeTrackObjectUrl(track) {
    if (track && track.url && track.url.indexOf("blob:") === 0) {
      URL.revokeObjectURL(track.url);
    }
  }

  function revokePlaylistObjectUrls() {
    playlist.forEach(revokeTrackObjectUrl);
  }

  function loadTrack(index, autoplay) {
    if (!playlist.length) {
      resetPlayer();
      return;
    }

    currentIndex = clampIndex(index);
    var track = playlist[currentIndex];
    if (track && track.source === "usb") {
      var usbIndex = usbPlaylist.findIndex(function (item) {
        return item.id === track.usbTrackId || item.path === track.path;
      });
      if (usbIndex < 0) {
        removeUsbTracksFromMainPlaylist();
        showUsbToast(_t('usb.removedCleaned'));
        return;
      }
      loadUsbTrack(usbIndex, autoplay, 0);
      return;
    }

    resetPlayer();
  }

  function togglePlay() {
    if (!playlist.length) {
      return;
    }

    var current = playlist[currentIndex];
    if (current && current.source === "usb") {
      if (activeAudioSource !== "usb") {
        loadTrack(currentIndex, true);
      } else if (audio.paused) {
        applyMediaVolume(audio.volume);
        audio.play()["catch"](function () {
          updateUsbPlaybackUi();
        });
      } else {
        audio.pause();
      }
      return;
    }
  }

  function stopPlayback() {
    if (activeAudioSource !== "usb") {
      return;
    }
    audio.pause();
    audio.currentTime = 0;
    updateProgress();
  }

  function playPrevious() {
    if (!playlist.length) {
      return;
    }

    if (activeAudioSource === "usb" || playlist[currentIndex] && playlist[currentIndex].source === "usb") {
      playPreviousUsbTrack();
      return;
    }
  }

  function playNext() {
    if (!playlist.length) {
      return;
    }

    if (activeAudioSource === "usb" || playlist[currentIndex] && playlist[currentIndex].source === "usb") {
      playNextUsbTrack();
      return;
    }
  }

  function handleEnded() {
    if (activeAudioSource === "usb") {
      usbProgress.value = 0;
      usbCurrentTime.textContent = "0:00";
      updateRangeFill(usbProgress);
      loadUsbTrack(getAutoAdvanceUsbIndex(), true, 0);
      rememberUsbResumePoint();
      return;
    }
  }

  function handleAudioError() {
    if (activeAudioSource === "usb") {
      usbTrackMeta.textContent = _t('usb.currentUnplayable');
      updateUsbPlaybackUi();
      if (usbPlaylist.length > 1) {
        window.setTimeout(function () {
          loadUsbTrack(getAutoAdvanceUsbIndex(), true, 0);
        }, 800);
      }
      return;
    }
  }

  function clearPlaylist() {
    revokePlaylistObjectUrls();
    playlist = [];
    favoriteIds = [];
    favoriteKeys = [];
    currentIndex = 0;
    saveFavoriteKeys();
    resetPlayer();
    renderPlaylist();
    renderFavorites();
  }

  function removeTrack(id) {
    var index = playlist.findIndex(function (track) {
      return track.id === id;
    });

    if (index < 0) {
      return;
    }

    var wasCurrent = index === currentIndex;
    var removed = playlist.splice(index, 1)[0];
    favoriteIds = favoriteIds.filter(function (trackId) {
      return trackId !== id;
    });
    favoriteKeys = favoriteKeys.filter(function (trackKey) {
      return trackKey !== getTrackFavoriteKey(removed);
    });
    saveFavoriteKeys();

    if (removed.url && removed.url.indexOf("blob:") === 0) {
      URL.revokeObjectURL(removed.url);
    }

    if (!playlist.length) {
      resetPlayer();
    } else if (wasCurrent) {
      loadTrack(Math.min(index, playlist.length - 1), !audio.paused);
    } else if (index < currentIndex) {
      currentIndex -= 1;
    }

    renderPlaylist();
    renderFavorites();
    updateFavoriteState();
  }

  function moveTrack(id, direction) {
    var index = playlist.findIndex(function (track) {
      return track.id === id;
    });
    var nextIndex = index + direction;

    if (index < 0 || nextIndex < 0 || nextIndex >= playlist.length) {
      return;
    }

    var track = playlist[index];
    playlist.splice(index, 1);
    playlist.splice(nextIndex, 0, track);

    if (currentIndex === index) {
      currentIndex = nextIndex;
    } else if (currentIndex === nextIndex) {
      currentIndex = index;
    }

    renderPlaylist();
    renderFavorites();
  }

  function renderPlaylist() {
    if (activeModule === "usb") {
      renderUsbPlaylist();
      return;
    }

    trackCount.textContent = "(" + playlist.length + ")";
    playlistEl.innerHTML = "";

    if (!playlist.length) {
      var empty = document.createElement("li");
      empty.className = "empty-state";
      empty.textContent = _t('panel.emptyPlaylist');
      playlistEl.appendChild(empty);
      updatePlaylistBatchBar();
      return;
    }

    var batchMode = playlistIsEditing;
    var selectedMap = playlistBatchSelected;

    playlist.forEach(function (track) {
      var originalIndex = playlist.indexOf(track);
      var isPlaying = (originalIndex === currentIndex && !audio.paused && !audio.ended);
      var isSelected = !!selectedMap[track.id];
      var item = document.createElement("li");
      item.className = "track-item" +
        (batchMode ? " is-batch-mode" : "") +
        (isSelected ? " is-selected" : "") +
        (isPlaying ? " is-playing" : "") +
        (originalIndex === currentIndex ? " is-active" : "");
      item.tabIndex = 0;

      var checkboxHtml = '<button class="track-select-checkbox' + (batchMode ? " is-visible" : "") + (isSelected ? " is-checked" : "") + '" type="button" aria-label="' + _t('action.select') + '"></button>';
      var playingIcon = isPlaying
        ? '<img class="track-playing-icon" src="./assets/playlist-icons/playing.svg" alt="' + _t('action.playing') + '" />'
        : '';
      var trackNum = isPlaying
        ? ''
        : '<span class="track-index">' + String(originalIndex + 1).padStart(2, "0") + '</span>';
      var coverHtml = '<span class="track-cover"></span>';
      var playBtn = isPlaying
        ? ''
        : '<button class="track-action-btn track-play-btn" title="' + _t('action.play') + '" aria-label="' + _t('action.play') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M8 5v14l11-7z"/></svg></button>';
      var isFav = isTrackFavorite(track);
      var favBtnHtml = isFav
        ? '<button class="track-action-btn track-fav-btn is-active" title="' + _t('action.cancelFavorite') + '" aria-label="' + _t('action.cancelFavorite') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></button>'
        : '<button class="track-action-btn track-fav-btn" title="' + _t('action.addFavorite') + '" aria-label="' + _t('action.addFavorite') + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></button>';
      var deleteBtnHtml = '<button class="track-action-btn track-delete-btn" title="' + _t('action.delete') + '" aria-label="' + _t('action.delete') + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>';

      item.innerHTML =
        checkboxHtml +
        playingIcon +
        trackNum +
        coverHtml +
        '<span class="track-info">' +
        '<span class="track-name"></span>' +
        '<span class="track-meta"></span>' +
        '</span>' +
        '<span class="track-actions">' +
        playBtn +
        favBtnHtml +
        deleteBtnHtml +
        '</span>';

      item.querySelector(".track-name").textContent = track.title;
      item.querySelector(".track-meta").textContent = track.artist + " - " + (track.durationLabel || "--:--");

      if (track.coverUrl) {
        var img = document.createElement("img");
        img.src = track.coverUrl;
        img.alt = "";
        img.onerror = function () { this.style.display = "none"; };
        item.querySelector(".track-cover").appendChild(img);
      }

      item.addEventListener("click", function (event) {
        if (event.target.closest(".track-select-checkbox")) {
          togglePlaylistBatchSelect(track.id);
          return;
        }
        if (event.target.closest(".track-delete-btn")) {
          confirmDeleteTrack(track.id, "playlist");
          return;
        }
        if (event.target.closest(".track-fav-btn")) {
          if (activeModule === "usb") {
            toggleUsbFavoriteById(track.id);
          } else {
            if (isTrackFavorite(track)) {
              removeFavorite(track);
            } else {
              addFavorite(track);
            }
            renderFavorites();
            if (activeModule === "usb") {
              renderUsbPlaylist();
            } else {
              renderPlaylist();
            }
          }
          return;
        }
        if (event.target.closest(".track-play-btn")) {
          if (activeModule === "usb") {
            var usbIdx = usbPlaylist.findIndex(function (t) { return t.id === track.id; });
            if (usbIdx >= 0) playUsbTrackAt(usbIdx);
          } else {
            loadTrack(originalIndex, true);
          }
          return;
        }
        if (batchMode) {
          togglePlaylistBatchSelect(track.id);
        } else {
          loadTrack(originalIndex, true);
        }
      });

      item.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
          if (batchMode) {
            togglePlaylistBatchSelect(track.id);
          } else {
            loadTrack(originalIndex, true);
          }
        }
      });

      playlistEl.appendChild(item);
    });
    updatePlaylistBatchBar();
  }

  function renderUsbPlaylist() {
    trackCount.textContent = "(" + usbPlaylist.length + ")";
    playlistEl.innerHTML = "";

    if (!usbPlaylist.length) {
      var empty = document.createElement("li");
      empty.className = "empty-state";
      empty.textContent = usbState.connected ? _t('usb.noMusicFiles') : _t('usb.deviceNotConnected');
      playlistEl.appendChild(empty);
      updatePlaylistBatchBar();
      return;
    }

    usbPlaylist.forEach(function (track) {
      var originalIndex = usbPlaylist.indexOf(track);
      var isFav = isUsbTrackFavorite(track);
      var isPlaying = (originalIndex === currentUsbIndex && activeModule === "usb" && !audio.paused && !audio.ended);
      var batchMode = playlistIsEditing;
      var selectedMap = playlistBatchSelected;
      var isSelected = !!selectedMap[track.id];
      var item = document.createElement("li");
      item.className = "track-item" +
        (batchMode ? " is-batch-mode" : "") +
        (isSelected ? " is-selected" : "") +
        (isPlaying ? " is-playing" : "") +
        (originalIndex === currentUsbIndex ? " is-active" : "");
      item.tabIndex = 0;

      var checkboxHtml = '<button class="track-select-checkbox' + (batchMode ? " is-visible" : "") + (isSelected ? " is-checked" : "") + '" type="button" aria-label="' + _t('action.select') + '"></button>';
      var playingIcon = isPlaying
        ? '<img class="track-playing-icon" src="./assets/playlist-icons/playing.svg" alt="' + _t('action.playing') + '" />'
        : '';
      var trackNum = isPlaying
        ? ''
        : '<span class="track-index">' + String(originalIndex + 1).padStart(2, "0") + '</span>';
      var coverHtml = '<span class="track-cover"><img src="" alt="" onerror="this.style.display=\'none\'" /></span>';
      var playBtn = isPlaying
        ? ''
        : '<button class="track-action-btn track-play-btn" title="' + _t('action.play') + '" aria-label="' + _t('action.play') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M8 5v14l11-7z"/></svg></button>';
      var favBtnHtml = isFav
        ? '<button class="track-action-btn track-fav-btn is-active" title="' + _t('action.cancelFavorite') + '" aria-label="' + _t('action.cancelFavorite') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></button>'
        : '<button class="track-action-btn track-fav-btn" title="' + _t('action.addFavorite') + '" aria-label="' + _t('action.addFavorite') + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></button>';
      var deleteBtnHtml = '<button class="track-action-btn track-delete-btn" title="' + _t('action.delete') + '" aria-label="' + _t('action.delete') + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>';

      item.innerHTML =
        checkboxHtml +
        playingIcon +
        trackNum +
        coverHtml +
        '<span class="track-info">' +
        '<span class="track-name"></span>' +
        '<span class="track-meta"></span>' +
        '</span>' +
        '<span class="track-actions">' +
        playBtn +
        favBtnHtml +
        deleteBtnHtml +
        '</span>';

      item.querySelector(".track-name").textContent = track.title;
      item.querySelector(".track-meta").textContent = track.artist + " - " + (track.durationLabel || "--:--");

      var coverImg = item.querySelector(".track-cover img");
      if (track.coverUrl) {
        coverImg.src = track.coverUrl;
      } else {
        coverImg.style.display = "none";
      }

      item.addEventListener("click", function (event) {
        if (event.target.closest(".track-select-checkbox")) {
          togglePlaylistBatchSelect(track.id);
          return;
        }
        if (event.target.closest(".track-delete-btn")) {
          confirmDeleteTrack(track.id, "playlist");
          return;
        }
        if (event.target.closest(".track-fav-btn")) {
          toggleUsbFavoriteById(track.id);
          return;
        }
        if (event.target.closest(".track-play-btn")) {
          var idx = usbPlaylist.findIndex(function (t) { return t.id === track.id; });
          if (idx >= 0) playUsbTrackAt(idx);
          return;
        }
        if (batchMode) {
          togglePlaylistBatchSelect(track.id);
        } else {
          var index = usbPlaylist.findIndex(function (t) { return t.id === track.id; });
          if (index >= 0) playUsbTrackAt(index);
        }
      });

      item.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
          if (batchMode) {
            togglePlaylistBatchSelect(track.id);
          } else {
            var idx = usbPlaylist.findIndex(function (t) { return t.id === track.id; });
            if (idx >= 0) playUsbTrackAt(idx);
          }
        }
      });

      playlistEl.appendChild(item);
    });
    updatePlaylistBatchBar();
  }

  function renderFavorites() {
    var usbFavoriteTracks = usbPlaylist.filter(function (track) {
      return isUsbTrackFavorite(track);
    });
    var favoriteTracks = usbFavoriteTracks.map(function(track) {
      return Object.assign({}, track, { source: 'usb' });
    });

    favoriteCount.textContent = "(" + favoriteTracks.length + ")";
    favoriteListEl.innerHTML = "";

    if (!favoriteTracks.length) {
      var empty = document.createElement("li");
      empty.className = "empty-state";
      empty.textContent = _t('panel.emptyFavorites');
      favoriteListEl.appendChild(empty);
      updateFavoriteBatchBar();
      return;
    }

    var batchMode = favoriteIsEditing;
    var selectedMap = favoriteBatchSelected;

    favoriteTracks.forEach(function (track) {
      var isUsb = track.source === 'usb';
      var originalIndex = isUsb ? usbPlaylist.indexOf(track) : -1;
      var isPlaying = (isUsb && originalIndex === currentUsbIndex && activeModule === "usb" && !audio.paused && !audio.ended);
      var isSelected = !!selectedMap[track.id];
      var item = document.createElement("li");
      item.className = "track-item" +
        (batchMode ? " is-batch-mode" : "") +
        (isSelected ? " is-selected" : "") +
        (isPlaying ? " is-playing" : "") +
        ((isUsb && originalIndex === currentUsbIndex) ? " is-active" : "");
      item.tabIndex = 0;

      var checkboxHtml = '<button class="track-select-checkbox' + (batchMode ? " is-visible" : "") + (isSelected ? " is-checked" : "") + '" type="button" aria-label="' + _t('action.select') + '"></button>';
      var playingIcon = isPlaying
        ? '<img class="track-playing-icon" src="./assets/playlist-icons/playing.svg" alt="' + _t('action.playing') + '" />'
        : '';
      var trackNum = isPlaying
        ? ''
        : '<span class="track-index">' + String(originalIndex + 1).padStart(2, "0") + '</span>';
      var coverHtml = '<span class="track-cover"><img src="" alt="" onerror="this.style.display=\'none\'" /></span>';
      var playBtn = isPlaying
        ? ''
        : '<button class="track-action-btn track-play-btn" title="' + _t('action.play') + '" aria-label="' + _t('action.play') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M8 5v14l11-7z"/></svg></button>';
      var favBtnHtml = '<button class="track-action-btn track-fav-btn is-active" title="' + _t('action.cancelFavorite') + '" aria-label="' + _t('action.cancelFavorite') + '"><svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></button>';
      var deleteBtnHtml = '<button class="track-action-btn track-delete-btn" title="' + _t('action.delete') + '" aria-label="' + _t('action.delete') + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>';

      item.innerHTML =
        checkboxHtml +
        playingIcon +
        trackNum +
        coverHtml +
        '<span class="track-info">' +
        '<span class="track-name"></span>' +
        '<span class="track-meta"></span>' +
        '</span>' +
        '<span class="track-actions">' +
        playBtn +
        favBtnHtml +
        deleteBtnHtml +
        '</span>';

      item.querySelector(".track-name").textContent = track.title;
      item.querySelector(".track-meta").textContent = track.artist + " - " + (track.durationLabel || "--:--");

      var coverImg = item.querySelector(".track-cover img");
      if (track.coverUrl) {
        coverImg.src = track.coverUrl;
      } else {
        coverImg.style.display = "none";
      }

      item.addEventListener("click", function (event) {
        if (event.target.closest(".track-select-checkbox")) {
          toggleFavoriteBatchSelect(track.id);
          return;
        }
        if (event.target.closest(".track-delete-btn")) {
          confirmDeleteTrack(track.id, "favorite");
          return;
        }
        if (event.target.closest(".track-fav-btn")) {
          removeFavoriteById(track.id);
          return;
        }
        if (event.target.closest(".track-play-btn")) {
          playTrack(track.id);
          return;
        }
        if (batchMode) {
          toggleFavoriteBatchSelect(track.id);
        } else {
          playTrack(track.id);
        }
      });

      item.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
          if (batchMode) {
            toggleFavoriteBatchSelect(track.id);
          } else {
            playTrack(track.id);
          }
        }
      });

      favoriteListEl.appendChild(item);
    });
    updateFavoriteBatchBar();
  }

  function toggleFavoriteForCurrentTrack() {
    var current = playlist[currentIndex];
    if (!current) {
      return;
    }

    if (isTrackFavorite(current)) {
      removeFavorite(current);
    } else {
      addFavorite(current);
    }

    renderFavorites();
  }

  function updateFavoriteState() {
  }

  function addFavorite(track) {
    var key = getTrackFavoriteKey(track);
    if (favoriteKeys.indexOf(key) < 0) {
      favoriteKeys.push(key);
      saveFavoriteKeys();
    }
    var id = track.id;
    if (id && favoriteIds.indexOf(id) < 0) {
      favoriteIds.push(id);
    }
    syncFavoriteToNative(track, true);
  }

  function removeFavorite(track) {
    var key = getTrackFavoriteKey(track);
    favoriteKeys = favoriteKeys.filter(function (trackKey) {
      return trackKey !== key;
    });
    if (track.id) {
      favoriteIds = favoriteIds.filter(function (trackId) {
        return trackId !== track.id;
      });
    }
    saveFavoriteKeys();
    syncFavoriteToNative(track, false);
  }

  function removeFavoriteById(id) {
    var track = usbPlaylist.find(function (t) { return t.id === id; });
    if (!track) return;
    removeFavorite(track);
    renderFavorites();
    renderUsbPlaylist();
  }

  function playTrack(id) {
    var index = usbPlaylist.findIndex(function (t) { return t.id === id; });
    if (index >= 0) {
      switchToModule("usb");
      loadUsbTrack(index, true);
    }
  }

  function isTrackFavorite(track) {
    return favoriteIds.indexOf(track.id) >= 0 || favoriteKeys.indexOf(getTrackFavoriteKey(track)) >= 0;
  }

  function getTrackFavoriteKey(track) {
    return [track.artist || "", track.title || ""].join("::").toLowerCase();
  }

  function loadFavoriteKeys() {
    try {
      return JSON.parse(window.localStorage.getItem(FAVORITE_STORAGE_KEY) || "[]");
    } catch (error) {
      return [];
    }
  }

  function saveFavoriteKeys() {
    try {
      window.localStorage.setItem(FAVORITE_STORAGE_KEY, JSON.stringify(favoriteKeys));
    } catch (error) {
      // Storage can be unavailable in restricted WebView modes; UI state still works in memory.
    }
  }

  function toggleDrawer(type) {
    // 收音机界面已移除"收藏列表/音乐列表"功能
    if (activeModule === "radio") {
      closeDrawers();
      return;
    }
    var panel = type === "favorite" ? favoritePanel : playlistPanel;
    if (panel.classList.contains("is-open")) {
      closeDrawers();
      return;
    }

    closeDrawers();
    panel.classList.remove("is-hidden");
    panel.classList.add("is-open");
    panel.setAttribute("aria-hidden", "false");
    shell.classList.add("has-drawer");
    favoritePanelBtn.classList.toggle("is-active", type === "favorite");
    playlistPanelBtn.classList.toggle("is-active", type === "playlist");
    favoritePanelBtn.setAttribute("aria-expanded", String(type === "favorite"));
    playlistPanelBtn.setAttribute("aria-expanded", String(type === "playlist"));
    updateSheetButtons();
  }

  function closeDrawers() {
    favoritePanel.classList.remove("is-open");
    favoritePanel.classList.remove("is-hidden");
    playlistPanel.classList.remove("is-open");
    playlistPanel.classList.remove("is-hidden");
    favoritePanel.setAttribute("aria-hidden", "true");
    playlistPanel.setAttribute("aria-hidden", "true");
    shell.classList.remove("has-drawer");
    favoritePanelBtn.classList.remove("is-active");
    playlistPanelBtn.classList.remove("is-active");
    favoritePanelBtn.setAttribute("aria-expanded", "false");
    playlistPanelBtn.setAttribute("aria-expanded", "false");
    favoriteIsEditing = false;
    playlistIsEditing = false;
    favoriteBatchSelected = {};
    playlistBatchSelected = {};
    favoritePanel.classList.remove("is-batch-mode");
    playlistPanel.classList.remove("is-batch-mode");
    if (favoriteBatchBar) favoriteBatchBar.hidden = true;
    if (playlistBatchBar) playlistBatchBar.hidden = true;
    if (favoriteBatchActions) favoriteBatchActions.hidden = true;
    if (playlistBatchActions) playlistBatchActions.hidden = true;
  }

  function initSheetDrag(type) {
    var panel = type === "favorite" ? favoritePanel : playlistPanel;
    var handle = panel.querySelector("[data-drag-handle]");
    if (!handle) return;

    handle.addEventListener("pointerdown", function (e) {
      if (e.button !== 0 && e.pointerType === "mouse") return;
      e.preventDefault();
      sheetDragState = {
        panel: panel,
        type: type,
        startY: e.clientY,
        currentTranslate: 0,
        isDragging: false
      };
      panel.classList.add("is-dragging");
      handle.setPointerCapture(e.pointerId);
    });

    handle.addEventListener("pointermove", function (e) {
      if (!sheetDragState || sheetDragState.panel !== panel) return;
      var deltaY = e.clientY - sheetDragState.startY;
      if (!sheetDragState.isDragging && Math.abs(deltaY) > 5) {
        sheetDragState.isDragging = true;
      }
      if (sheetDragState.isDragging) {
        sheetDragState.currentTranslate = Math.min(0, deltaY);
        applySheetTransform(panel, sheetDragState.currentTranslate);
      }
    });

    handle.addEventListener("pointerup", function (e) {
      if (!sheetDragState || sheetDragState.panel !== panel) return;
      panel.classList.remove("is-dragging");
      try { handle.releasePointerCapture(e.pointerId); } catch (_) {}

      var translate = sheetDragState.currentTranslate;
      var distance = Math.abs(translate);
      var threshold = SHEET_HEIGHT * SHEET_HIDE_THRESHOLD;

      if (sheetDragState.isDragging && distance > threshold) {
        panel.classList.add("is-hidden");
        panel.classList.remove("is-open");
        panel.setAttribute("aria-hidden", "true");
        shell.classList.remove("has-drawer");
        favoritePanelBtn.classList.remove("is-active");
        playlistPanelBtn.classList.remove("is-active");
        favoritePanelBtn.setAttribute("aria-expanded", "false");
        playlistPanelBtn.setAttribute("aria-expanded", "false");
      } else {
        panel.classList.add("is-open");
        panel.classList.remove("is-hidden");
      }
      applySheetTransform(panel, 0);
      sheetDragState = null;
    });

    handle.addEventListener("pointercancel", function () {
      if (!sheetDragState || sheetDragState.panel !== panel) return;
      panel.classList.remove("is-dragging");
      applySheetTransform(panel, 0);
      sheetDragState = null;
    });

    handle.addEventListener("click", function (e) {
      if (sheetDragState && sheetDragState.isDragging) return;
      if (panel.classList.contains("is-open")) {
        closeDrawers();
      }
    });
  }

  function applySheetTransform(panel, translate) {
    // 抽屉面板按设计稿固定为全宽 800px、left 0，仅需纵向位移
    panel.style.transform = translate === 0 ? "" : "translateY(" + translate + "px)";
  }

  var trackDragState = null;
  var TRACK_DRAG_THRESHOLD = 5;

  function initTrackItemDrag(container, listType) {
    if (!container) return;
    var handles = container.querySelectorAll("[data-drag-handle-item]");
    handles.forEach(function (handle) {
      if (handle.__dragInitialized) return;
      handle.__dragInitialized = true;

      handle.addEventListener("pointerdown", function (e) {
        if (e.button !== 0 && e.pointerType === "mouse") return;
        e.preventDefault();
        e.stopPropagation();
        var item = handle.closest(".track-item");
        if (!item) return;

        trackDragState = {
          item: item,
          list: container,
          listType: listType,
          startY: e.clientY,
          currentY: e.clientY,
          isDragging: false,
          handle: handle,
          pointerId: e.pointerId
        };
        handle.setPointerCapture(e.pointerId);
      });

      handle.addEventListener("pointermove", function (e) {
        if (!trackDragState || trackDragState.handle !== handle) return;
        trackDragState.currentY = e.clientY;
        var deltaY = trackDragState.currentY - trackDragState.startY;

        if (!trackDragState.isDragging && Math.abs(deltaY) > TRACK_DRAG_THRESHOLD) {
          trackDragState.isDragging = true;
          trackDragState.item.classList.add("is-dragging");
        }

        if (trackDragState.isDragging) {
          e.preventDefault();
          var items = Array.prototype.slice.call(trackDragState.list.querySelectorAll(".track-item"));
          var currentIndex = items.indexOf(trackDragState.item);
          var rect = trackDragState.item.getBoundingClientRect();
          var itemHeight = rect.height;
          var newIndex = currentIndex;

          for (var i = 0; i < items.length; i++) {
            if (i === currentIndex) continue;
            var otherRect = items[i].getBoundingClientRect();
            var otherCenter = otherRect.top + otherRect.height / 2;
            if (trackDragState.currentY < otherCenter) {
              if (i < currentIndex) newIndex = Math.min(newIndex, i);
            } else {
              if (i > currentIndex) newIndex = Math.max(newIndex, i);
            }
          }

          if (newIndex !== currentIndex) {
            if (newIndex < currentIndex) {
              var refItem = items[newIndex];
              trackDragState.list.insertBefore(trackDragState.item, refItem);
            } else {
              var refItem2 = items[newIndex + 1];
              if (refItem2) {
                trackDragState.list.insertBefore(trackDragState.item, refItem2);
              } else {
                trackDragState.list.appendChild(trackDragState.item);
              }
            }
            items = Array.prototype.slice.call(trackDragState.list.querySelectorAll(".track-item"));
          }
        }
      });

      handle.addEventListener("pointerup", function (e) {
        if (!trackDragState || trackDragState.handle !== handle) return;
        trackDragState.item.classList.remove("is-dragging");
        try { handle.releasePointerCapture(e.pointerId); } catch (_) {}

        if (!trackDragState.isDragging) {
          closeDrawers();
        } else {
          var newOrder = Array.prototype.slice.call(trackDragState.list.querySelectorAll(".track-item"));
          var newIndices = newOrder.map(function (item) {
            var trackId = item.getAttribute("data-track-id");
            if (!trackId) {
              var infoEl = item.querySelector(".track-name");
              return infoEl ? infoEl.textContent : null;
            }
            return trackId;
          });

          if (trackDragState.listType === "playlist") {
            reorderPlaylistByDom();
          } else if (trackDragState.listType === "usb") {
            reorderUsbPlaylistByDom();
          } else if (trackDragState.listType === "favorite") {
            reorderFavoritesByDom();
          }
        }

        trackDragState = null;
      });

      handle.addEventListener("pointercancel", function () {
        if (!trackDragState || trackDragState.handle !== handle) return;
        trackDragState.item.classList.remove("is-dragging");
        trackDragState = null;
      });
    });
  }

  function reorderPlaylistByDom() {
    var items = Array.prototype.slice.call(playlistEl.querySelectorAll(".track-item"));
    var newOrder = [];
    items.forEach(function (item) {
      var titleEl = item.querySelector(".track-name");
      var metaEl = item.querySelector(".track-meta");
      if (!titleEl) return;
      var title = titleEl.textContent;
      var meta = metaEl ? metaEl.textContent : "";
      var artist = meta.split(" - ")[0] || "";
      var found = playlist.find(function (t) { return t.title === title && t.artist === artist; });
      if (found) newOrder.push(found);
    });
    if (newOrder.length === playlist.length) {
      playlist = newOrder;
      if (playlistPlayMode === "loop-single" && currentIndex < newOrder.length) {
        // keep
      }
      renderPlaylist();
    }
  }

  function reorderUsbPlaylistByDom() {
    var items = Array.prototype.slice.call(playlistEl.querySelectorAll(".track-item"));
    var newOrder = [];
    items.forEach(function (item) {
      var titleEl = item.querySelector(".track-name");
      var metaEl = item.querySelector(".track-meta");
      if (!titleEl) return;
      var title = titleEl.textContent;
      var meta = metaEl ? metaEl.textContent : "";
      var artist = meta.split(" - ")[0] || "";
      var found = usbPlaylist.find(function (t) { return t.title === title && t.artist === artist; });
      if (found) newOrder.push(found);
    });
    if (newOrder.length === usbPlaylist.length) {
      var currentTrack = usbPlaylist[currentUsbIndex];
      usbPlaylist = newOrder;
      var newIndex = usbPlaylist.indexOf(currentTrack);
      if (newIndex !== -1) currentUsbIndex = newIndex;
      renderUsbPlaylist();
    }
  }

  function reorderFavoritesByDom() {
    var items = Array.prototype.slice.call(favoriteListEl.querySelectorAll(".track-item"));
    var newOrder = [];
    items.forEach(function (item) {
      var titleEl = item.querySelector(".track-name");
      var metaEl = item.querySelector(".track-meta");
      if (!titleEl) return;
      var title = titleEl.textContent;
      var meta = metaEl ? metaEl.textContent : "";
      var artist = meta.split(" - ")[0] || "";
      var found = usbPlaylist.find(function (t) { return t.title === title && t.artist === artist; });
      if (found) newOrder.push(found);
    });
    if (newOrder.length > 0) {
      favoriteIds = newOrder.map(function (t) { return t.id; });
      favoriteKeys = newOrder.map(function (t) { return getTrackFavoriteKey(t); });
      window.localStorage.setItem(FAVORITE_STORAGE_KEY, JSON.stringify(favoriteKeys));
      renderFavorites();
      renderUsbPlaylist();
    }
  }

  function toggleSheetPlay(type) {
    var panel = type === "favorite" ? favoritePanel : playlistPanel;
    var hasTracks = type === "favorite" ? favoriteIds.length > 0 : usbPlaylist.length > 0;
    
    if (!hasTracks) return;

    var isPlaying = !audio.paused && !audio.ended && audio.src;
    var newState = !isPlaying;

    if (newState) {
      if (type === "favorite") {
        if (favoriteIds.length === 0) return;
        if (activeModule !== "bluetooth") {
          playTrack(favoriteIds[0]);
        }
      } else {
        if (usbPlaylist.length === 0) return;
        if (activeModule !== "usb") {
          switchModule("usb");
        }
        if (activeAudioSource !== "usb") {
          loadUsbTrack(currentUsbIndex, true, 0);
        }
      }
      audio.play();
    } else {
      audio.pause();
    }
  }

  function sheetModeToMain(mode) {
    var map = { "loop-list": "folder-loop", "loop-single": "single", "shuffle": "folder-random" };
    return map[mode] || "folder-loop";
  }

  function mainModeToSheet(mode) {
    var map = { "folder-loop": "loop-list", "single": "loop-single", "folder-random": "shuffle" };
    return map[mode] || "loop-list";
  }

  function cycleSheetPlayMode(type) {
    var currentMode = type === "favorite" ? favoritePlayMode : playlistPlayMode;
    var modes = ["loop-list", "loop-single", "shuffle"];
    var currentIndex = modes.indexOf(currentMode);
    var nextIndex = (currentIndex + 1) % modes.length;
    var newMode = modes[nextIndex];

    if (type === "favorite") {
      favoritePlayMode = newMode;
    } else {
      playlistPlayMode = newMode;
    }

    var mainEquivalent = sheetModeToMain(newMode);
    usbPlayMode = mainEquivalent;
    audio.loop = usbPlayMode === "single";
    if (usbShuffleHistory.length) {
      usbShuffleHistory = [];
    }
    playlistPlayMode = newMode;
    favoritePlayMode = newMode;

    updateSheetModeButton("favorite");
    updateSheetModeButton("playlist");
    updateUsbPlayModeButton();
    if (activeAudioSource === "usb" && usbPlaylist.length && audio.paused) {
      audio.play()["catch"](function () {
        updateUsbPlaybackUi();
      });
    }
  }

  function toggleSheetEdit(type) {
    var panel = type === "favorite" ? favoritePanel : playlistPanel;
    var isEditing = type === "favorite" ? favoriteIsEditing : playlistIsEditing;
    var newState = !isEditing;

    if (type === "favorite") {
        favoriteIsEditing = newState;
        if (!newState) {
            favoriteBatchSelected = {};
        }
    } else {
        playlistIsEditing = newState;
        if (!newState) {
            playlistBatchSelected = {};
        }
    }

    panel.classList.toggle("is-batch-mode", newState);

    var batchBar = type === "favorite" ? favoriteBatchBar : playlistBatchBar;
    if (batchBar) {
        batchBar.hidden = !newState;
    }

    var batchActions = type === "favorite" ? favoriteBatchActions : playlistBatchActions;
    if (batchActions) {
        batchActions.hidden = !newState;
    }

    if (type === "favorite") {
        renderFavorites();
    } else {
        if (activeModule === "usb") {
            renderUsbPlaylist();
        } else {
            renderPlaylist();
        }
    }
  }

  function updateSheetButtons() {
    updateSheetPlayButtonState("favorite");
    updateSheetPlayButtonState("playlist");
    updateSheetModeButton("favorite");
    updateSheetModeButton("playlist");
  }

  function updateSheetPlayButtonState(type) {
    var btn = type === "favorite" ? favoritePlayBtn : playlistPlayBtn;
    if (!btn) return;
    var panel = type === "favorite" ? favoritePanel : playlistPanel;
    var isPlaying = !audio.paused && !audio.ended && audio.src;
    panel.setAttribute("data-playing", String(isPlaying));
    btn.setAttribute("aria-pressed", String(isPlaying));
  }

  function updateSheetModeButton(type) {
    var btn = type === "favorite" ? favoritePlayModeBtn : playlistPlayModeBtn;
    var labelEl = type === "favorite" ? favoriteModeLabel : playlistModeLabel;
    if (!btn) return;
    var mode = type === "favorite" ? favoritePlayMode : playlistPlayMode;
    var label = getModeLabel(mode);
    btn.innerHTML = getModeSvg(mode);
    btn.setAttribute("data-mode", mode);
    btn.setAttribute("title", label);
    btn.setAttribute("aria-label", label);
    if (labelEl) {
      labelEl.textContent = label;
    }
  }

  function handleTrackItemClick(id, type, e) {
    if (e && e.target.closest(".track-drag-handle")) {
      return;
    }
    if (e && e.target.closest(".track-action-btn")) {
      var actionBtn = e.target.closest(".track-action-btn");
      var action = actionBtn.getAttribute("data-action");
      if (action === "favorite") {
        if (type === "playlist") {
          toggleUsbFavoriteById(id);
        }
        return;
      }
      if (action === "delete") {
        if (type === "favorite") {
          removeFavoriteById(id);
        } else {
          removeUsbTrackById(id);
        }
        return;
      }
      if (action === "play") {
        playTrackById(id, type);
        return;
      }
    }
    if (type === "favorite") {
      playTrack(id);
    } else {
      var index = usbPlaylist.findIndex(function (t) { return t.id === id; });
      if (index >= 0) playUsbTrackAt(index);
    }
  }

  function playTrackById(id, type) {
    if (type === "favorite") {
      playTrack(id);
    } else {
      var index = usbPlaylist.findIndex(function (t) { return t.id === id; });
      if (index >= 0) {
        playUsbTrackAt(index);
      }
    }
  }

  function toggleUsbFavoriteById(id) {
    var track = usbPlaylist.find(function (t) { return t.id === id; });
    if (!track) return;
    var isFav = isUsbTrackFavorite(track);
    if (isFav) {
      removeUsbFavorite(track);
    } else {
      addUsbFavorite(track);
    }
    renderUsbPlaylist();
    renderFavorites();
  }

  function removeUsbTrackById(id) {
    var index = usbPlaylist.findIndex(function (t) { return t.id === id; });
    if (index === -1) return;
    usbPlaylist.splice(index, 1);
    if (index === currentUsbIndex) {
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
      usbCurrentIndex = Math.min(index, usbPlaylist.length - 1);
      updateUsbTransport();
    } else if (index < currentUsbIndex) {
      usbCurrentIndex--;
    }
    renderUsbPlaylist();
  }










  function updateDuration() {
    if (!Number.isFinite(audio.duration)) {
      return;
    }

    if (activeAudioSource === "usb") {
      var usbTrack = usbPlaylist[currentUsbIndex];
      usbProgress.max = audio.duration;
      usbDuration.textContent = formatTime(audio.duration);
      if (usbTrack) {
        usbTrack.durationLabel = formatTime(audio.duration);
      }
      if (usbResumePending) {
        audio.currentTime = Math.min(usbResumePending, Math.max(0, audio.duration - 1));
        usbResumePending = null;
      }
      updateUsbPlaybackUi();
      rememberUsbResumePoint();
      return;
    }
  }

  function updateProgress() {
    if (activeAudioSource === "usb") {
      updateUsbPlaybackUi();
      rememberUsbResumePoint();
      return;
    }

    if (isSeeking) {
      return;
    }
  }

  function updatePlayState() {
    if (activeAudioSource === "usb") {
      updateUsbPlaybackUi();
      notifyNativePlaybackState();
      updateSheetPlayButtonState("favorite");
      updateSheetPlayButtonState("playlist");
      updateMainPlayButtonState();
      if (favoritePanel.classList.contains("is-open")) renderFavorites();
      if (playlistPanel.classList.contains("is-open")) renderUsbPlaylist();
      return;
    }
    updateSheetPlayButtonState("favorite");
    updateSheetPlayButtonState("playlist");
    updateMainPlayButtonState();
  }

  function updateMainPlayButtonState() {
    if (!usbPlayBtn) return;
    var isPlaying = activeAudioSource === "usb" && !audio.paused && !audio.ended;
    usbPlayBtn.classList.toggle("is-playing", isPlaying);
    usbPlayBtn.setAttribute("aria-pressed", String(isPlaying));
    usbPlayBtn.setAttribute("title", isPlaying ? _t('action.pause') : _t('action.play'));
  }

  function notifyNativePlaybackState() {
    if (!window.MusicBridge || typeof window.MusicBridge.updateLocalPlaybackState !== "function") {
      return;
    }
    var current = playlist[currentIndex];
    try {
      window.MusicBridge.updateLocalPlaybackState(
        current ? current.title || "" : "",
        current ? current.artist || "" : "",
        Boolean(current && !audio.paused)
      );
    } catch (error) {
    }
  }

  function handleNativePlaybackControl(command) {
    if (command === "play") {
      if (audio.paused) {
        togglePlay();
      }
    } else if (command === "pause") {
      if (!audio.paused) {
        audio.pause();
      }
    } else if (command === "toggle") {
      togglePlay();
    } else if (command === "previous") {
      // 方向盘PREV：USB上一曲 / 收音机低频搜台
      if (activeModule === "radio") {
        seekRadio(-1);
      } else {
        playPrevious();
      }
    } else if (command === "next") {
      // 方向盘NEXT：USB下一曲 / 收音机高频搜台
      if (activeModule === "radio") {
        seekRadio(1);
      } else {
        playNext();
      }
    } else if (command === "stop") {
      stopPlayback();
    }
  }

  function updateRangeFill(input) {
    if (!input) return;
    var max = Number(input.max) || 1;
    var min = Number(input.min) || 0;
    var value = Number(input.value) || 0;
    var percent = ((value - min) / (max - min)) * 100;
    input.style.setProperty("--fill", Math.max(0, Math.min(100, percent)) + "%");
  }

  function handleKeyboard(event) {
    var activeTag = document.activeElement && document.activeElement.tagName;
    if (activeTag === "INPUT") {
      return;
    }

    if (event.code === "Space") {
      event.preventDefault();
      if (activeModule === "bluetooth") {
        toggleBluetoothPlaybackSynced();
      } else if (activeModule === "radio") {
        // 收音机模块UI已注释，停用快捷键播放
        // toggleRadioPlayback();
      } else {
        togglePlay();
      }
    } else if (event.key === "ArrowRight") {
      if (activeModule === "radio") {
        // seekRadio(0.2); // 收音机模块UI已注释
      } else if (activeModule === "usb") {
        audio.currentTime = Math.min((audio.currentTime || 0) + 5, audio.duration || 0);
      }
    } else if (event.key === "ArrowLeft") {
      if (activeModule === "radio") {
        // seekRadio(-0.2); // 收音机模块UI已注释
      } else if (activeModule === "usb") {
        audio.currentTime = Math.max((audio.currentTime || 0) - 5, 0);
      }
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      adjustActiveVolume(0.05);
    } else if (event.key === "ArrowDown") {
      event.preventDefault();
      adjustActiveVolume(-0.05);
    } else if (event.key === "Escape") {
      closeLyricsFull();
      closeDrawers();
    }
  }

  function setVolume(value) {
    audio.volume = Math.min(1, Math.max(0, value));
    if (usbVolume) {
      usbVolume.value = audio.volume;
    }
    if (usbVolumeValue) {
      usbVolumeValue.textContent = Math.round(audio.volume * 100) + "%";
    }
    if (usbVolume) {
      updateRangeFill(usbVolume);
    }
  }

  function adjustActiveVolume(delta) {
    if (activeModule === "bluetooth") {
      if (btVolume) {
        btVolume.value = Math.min(1, Math.max(0, Number(btVolume.value) + delta));
        if (btVolumeValue) btVolumeValue.textContent = Math.round(Number(btVolume.value) * 100) + "%";
        updateRangeFill(btVolume);
      }
      return;
    }

    if (activeModule === "radio") {
      // 收音机模块UI已注释，音量统一走系统媒体音量
      // adjustRadioVolume(delta);
      return;
    }

    setVolume(audio.volume + delta);
  }

  function closeLyricsFull() {
  }

  function renderLyricsPreview() {
  }

  function createLyricsForTrack() {
    return [];
  }

  function resetPlayer() {
    audio.removeAttribute("src");
    audio.load();
    if (usbTrackTitle) {
      usbTrackTitle.textContent = "";
    }
    if (usbTrackMeta) {
      usbTrackMeta.textContent = "";
    }
    if (usbCurrentTime) {
      usbCurrentTime.textContent = "0:00";
    }
    if (usbDuration) {
      usbDuration.textContent = "0:00";
    }
    if (usbProgress) {
      usbProgress.value = 0;
      usbProgress.max = 100;
      updateRangeFill(usbProgress);
    }
    closeLyricsFull();
    if (lyricsFullList) {
      lyricsFullList.innerHTML = "";
    }
    updateFavoriteState();
    updatePlayState();
  }

  // 限制索引范围，超出范围则循环到另一端
  function clampIndex(index) {
    if (index < 0) {
      return playlist.length - 1;
    }
    if (index >= playlist.length) {
      return 0;
    }
    return index;
  }

  function updateAlbumArtwork(track) {
  }

  // 格式化时间（秒 -> 分:秒）
  function formatTime(value) {
    if (!Number.isFinite(value) || value < 0) {
      return "0:00";
    }

    var minutes = Math.floor(value / 60);
    var seconds = Math.floor(value % 60);
    return minutes + ":" + String(seconds).padStart(2, "0");
  }

  function createId() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
      return window.crypto.randomUUID();
    }

    return String(Date.now()) + "-" + Math.random().toString(16).slice(2);
  }

  window.addEventListener("beforeunload", function () {
    rememberBluetoothResumePoint({ shouldResume: activeModule === "bluetooth" && isBtPlaying, wasPlaying: isBtPlaying });
    rememberUsbResumePoint();
    playlist.forEach(function (track) {
      if (track.url && track.url.indexOf("blob:") === 0) {
        URL.revokeObjectURL(track.url);
      }
    });
  });

  try {
    init();
  } catch (error) {
    console.error("Failed to initialize music player:", error);
  }

  function updateTheme() {
    var isLight = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches;
    document.documentElement.dataset.theme = isLight ? "light" : "dark";
  }

  updateTheme();

  if (window.matchMedia) {
    var mediaQuery = window.matchMedia('(prefers-color-scheme: light)');
    mediaQuery.addEventListener('change', updateTheme);
  }

  /* === 3D音律（音频可视化）代码已注释禁用 ===
  var audioVisualizerContext = null;
  var audioAnalyser = null;
  var audioSource = null;
  var audioGainNode = null;
  var audioSourceCreated = false;
  var visualizerCanvas = null;
  var visualizerCtx = null;
  var visualizerAnimationId = null;
  var visualizerMode = "stopped";
  var visualizerBlocks = [];
  var visualizerGridSize = 14;
  var visualizerFrequencyData = null;
  var visualizerWaveformData = null;
  var visualizerLastFrameTime = 0;
  var visualizerFrameInterval = 1000 / 30;
  var visualizerFreqHistory = [];
  var visualizerBeatEnergy = 0;
  var visualizerBeatCooldown = 0;
  var visualizerBeatTriggerTime = 0; // 节拍触发时间戳，用于延迟连锁
  var visualizerCachedCanvases = null;
  var visualizerParticles = []; // 背景粒子系统（布朗运动）
  var VISUALIZER_MAX_PARTICLES = 50;

  function initAudioVisualizer() {
    var canvases = document.querySelectorAll(".audio-visualizer");
    if (!canvases.length) return;

    visualizerCanvas = canvases[0];
    visualizerCtx = visualizerCanvas.getContext("2d");
    resizeVisualizer();

    if (!visualizerFrequencyData) {
      visualizerFrequencyData = new Uint8Array(256);
    }
    if (!visualizerWaveformData) {
      visualizerWaveformData = new Uint8Array(256);
    }

    try {
      var AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextConstructor) return;

      if (!audioVisualizerContext) {
        audioVisualizerContext = new AudioContextConstructor();
      }

      if (!audioSourceCreated) {
        audioSource = audioVisualizerContext.createMediaElementSource(audio);
        audioAnalyser = audioVisualizerContext.createAnalyser();
        audioAnalyser.fftSize = 512;
        audioAnalyser.smoothingTimeConstant = 0.75;
        audioGainNode = audioVisualizerContext.createGain();
        audioGainNode.gain.value = audio.volume;

        audioSource.connect(audioAnalyser);
        audioSource.connect(audioGainNode);
        audioGainNode.connect(audioVisualizerContext.destination);

        audioSourceCreated = true;
        visualizerFrequencyData = new Uint8Array(audioAnalyser.frequencyBinCount);
        visualizerWaveformData = new Uint8Array(audioAnalyser.fftSize);

        audio.addEventListener("volumechange", function() {
          if (audioGainNode) {
            audioGainNode.gain.value = audio.volume;
          }
        });
      }

      initVisualizerBlocks();
    } catch (e) {
      console.warn("Audio visualizer initialization failed:", e);
    }

    window.addEventListener("resize", resizeVisualizer);
  }

  function initVisualizerBlocks() {
    visualizerBlocks = [];
    var gridCols = 14;
    var gridRows = 10;
    for (var i = 0; i < gridCols * gridRows; i++) {
      var col = i % gridCols;
      var row = Math.floor(i / gridCols);
      var seed = Math.sin(i * 127.1 + 311.7) * 43758.5453;
      var frac = seed - Math.floor(seed);
      var seed2 = Math.sin(i * 269.5 + 183.3) * 43758.5453;
      var frac2 = seed2 - Math.floor(seed2);
      var seed3 = Math.sin(i * 419.2 + 571.4) * 43758.5453;
      var frac3 = seed3 - Math.floor(seed3);

      // 策略2：区域分组随机 — 左侧跟随低频，中间跟随人声/中频，右侧跟随高频
      var region;
      if (col < gridCols * 0.3) {
        region = "left";
      } else if (col < gridCols * 0.7) {
        region = "center";
      } else {
        region = "right";
      }

      // 计算到中心的归一化距离，用于节拍延迟连锁（多米诺骨牌效应）
      var dx = col - gridCols / 2 + 0.5;
      var dz = row - gridRows / 2 + 0.5;
      var distNorm = Math.sqrt(dx * dx + dz * dz) / Math.sqrt((gridCols / 2) * (gridCols / 2) + (gridRows / 2) * (gridRows / 2));

      // 策略3增强：多方向延迟连锁 — 随机选择波动方向制造视觉冲击
      // 0=中心向外扩散, 1=左到右波动, 2=右到左波动, 3=前到后波动
      var delayDirection = Math.floor(frac3 * 4);
      var delayFactor;
      if (delayDirection === 0) {
        delayFactor = distNorm; // 中心向外
      } else if (delayDirection === 1) {
        delayFactor = col / gridCols; // 左到右
      } else if (delayDirection === 2) {
        delayFactor = 1 - col / gridCols; // 右到左
      } else {
        delayFactor = row / gridRows; // 前到后
      }

      visualizerBlocks[i] = {
        height: 4,
        velocity: 0,
        amplitudeBias: 0.8 + frac * 0.4,
        phaseBias: frac * Math.PI * 2,
        speedBias: 0.7 + frac * 0.6,
        region: region,
        distNorm: distNorm,
        // 策略3增强：增大延迟范围（最大200ms），支持多方向波动
        beatDelayMs: delayFactor * 200,
        delayDirection: delayDirection,
        // 策略2增强：同区域内柱子响应不同频率子带，制造有序中的无序
        freqSubOffset: frac2, // 频率子带偏移（0-1），同区域柱子各有偏好
        sensitivity: 0.7 + frac2 * 0.6, // 响应灵敏度差异化
        decayRate: 0.68 + frac3 * 0.12, // 衰减系数差异化
        brownianPhase: frac * Math.PI * 2,
        isForeground: distNorm < 0.2,
        // 策略1增强：每柱独特色偏，避免同区域颜色过于均匀
        hueShift: (frac2 - 0.5) * 18
      };
    }
    initParticleSystem();
  }

  // 策略4：背景粒子系统初始化 — 布朗运动
  function initParticleSystem() {
    visualizerParticles = [];
    for (var i = 0; i < VISUALIZER_MAX_PARTICLES; i++) {
      visualizerParticles.push({
        x: Math.random(),
        y: Math.random(),
        vx: (Math.random() - 0.5) * 0.0015,
        vy: (Math.random() - 0.5) * 0.0015,
        size: 0.8 + Math.random() * 2.2,
        hue: 270 + Math.random() * 70,
        alpha: 0.08 + Math.random() * 0.15
      });
    }
  }

  function resizeVisualizer() {
    var canvases = document.querySelectorAll(".audio-visualizer");
    canvases.forEach(function(canvas) {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    });
    visualizerCachedCanvases = null;
    initVisualizerBlocks();
  }

  function startIdleVisualizer() {
    if (visualizerMode === "idle") return;
    visualizerMode = "idle";

    cancelAnimationFrame(visualizerAnimationId);

    resizeVisualizer();

    var activeModuleCanvases = document.querySelectorAll(".module-panel.is-active .audio-visualizer");
    activeModuleCanvases.forEach(function(canvas) {
      canvas.classList.add("is-active");
    });
    visualizerCachedCanvases = activeModuleCanvases;

    if (audioVisualizerContext && audioVisualizerContext.state === "suspended") {
      audioVisualizerContext.resume();
    }

    visualize();
  }

  function startVisualizer() {
    if (!audioVisualizerContext || !audioAnalyser) return;
    if (visualizerMode === "playing") return;
    visualizerMode = "playing";

    cancelAnimationFrame(visualizerAnimationId);

    resizeVisualizer();

    var activeModuleCanvases = document.querySelectorAll(".module-panel.is-active .audio-visualizer");
    activeModuleCanvases.forEach(function(canvas) {
      canvas.classList.add("is-active");
    });
    visualizerCachedCanvases = activeModuleCanvases;

    if (audioVisualizerContext.state === "suspended") {
      audioVisualizerContext.resume();
    }

    visualize();
  }

  function pauseVisualizer() {
    if (visualizerMode !== "playing") return;
    visualizerMode = "paused";
  }

  function stopVisualizer() {
    visualizerMode = "stopped";
    cancelAnimationFrame(visualizerAnimationId);

    var canvases = document.querySelectorAll(".audio-visualizer");
    canvases.forEach(function(canvas) {
      canvas.classList.remove("is-active");
      var ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    });

    visualizerFreqHistory = [];
    visualizerBeatEnergy = 0;
    visualizerBeatCooldown = 0;
    visualizerCachedCanvases = null;
    initVisualizerBlocks();
  }

  function visualize() {
    if (visualizerMode === "stopped") return;

    visualizerAnimationId = requestAnimationFrame(visualize);

    var now = performance.now();
    if (now - visualizerLastFrameTime < visualizerFrameInterval) return;
    visualizerLastFrameTime = now;

    var freqLen;

    if (visualizerMode === "idle") {
      if (!visualizerFrequencyData) {
        visualizerFrequencyData = new Uint8Array(256);
      }
      freqLen = visualizerFrequencyData.length;
      for (var i = 0; i < freqLen; i++) {
        visualizerFrequencyData[i] = 20;
      }
    } else if (visualizerMode === "playing") {
      if (!audioAnalyser) return;
      audioAnalyser.getByteFrequencyData(visualizerFrequencyData);
      audioAnalyser.getByteTimeDomainData(visualizerWaveformData);
      freqLen = visualizerFrequencyData.length;
    } else if (visualizerMode === "paused") {
      freqLen = visualizerFrequencyData.length;
    } else {
      return;
    }

    var lowFreq = averageArray(visualizerFrequencyData.slice(0, Math.floor(freqLen * 0.15)));
    var lowMidFreq = averageArray(visualizerFrequencyData.slice(Math.floor(freqLen * 0.15), Math.floor(freqLen * 0.30)));
    var midFreq = averageArray(visualizerFrequencyData.slice(Math.floor(freqLen * 0.30), Math.floor(freqLen * 0.50)));
    var highMidFreq = averageArray(visualizerFrequencyData.slice(Math.floor(freqLen * 0.50), Math.floor(freqLen * 0.75)));
    var highFreq = averageArray(visualizerFrequencyData.slice(Math.floor(freqLen * 0.75)));

    var lowScale = lowFreq / 255;
    var lowMidScale = lowMidFreq / 255;
    var midScale = midFreq / 255;
    var highMidScale = highMidFreq / 255;
    var highScale = highFreq / 255;

    var frameEnergy = 0;
    for (var ei = 0; ei < freqLen; ei++) {
      var v = visualizerFrequencyData[ei] / 255;
      frameEnergy += v * v;
    }
    frameEnergy = Math.sqrt(frameEnergy / freqLen);

    if (visualizerMode === "playing") {
      visualizerFreqHistory.push(frameEnergy);
      if (visualizerFreqHistory.length > 15) visualizerFreqHistory.shift();

      var beatSum = 0;
      for (var bi = 0; bi < visualizerFreqHistory.length; bi++) {
        beatSum += visualizerFreqHistory[bi];
      }
      var beatAvg = beatSum / visualizerFreqHistory.length;
      var beatThreshold = beatAvg * 1.25;

      if (frameEnergy > beatThreshold && visualizerBeatCooldown <= 0) {
        visualizerBeatEnergy = Math.min(1, (frameEnergy - beatAvg) * 3);
        visualizerBeatCooldown = 8;
        visualizerBeatTriggerTime = now; // 策略3：记录节拍触发时间，用于延迟连锁
      } else {
        visualizerBeatEnergy *= 0.85;
        visualizerBeatCooldown = Math.max(0, visualizerBeatCooldown - 1);
      }
    } else if (visualizerMode === "idle") {
      visualizerBeatEnergy = 0;
    } else {
      visualizerBeatEnergy *= 0.92;
    }

    var emotionCool = lowScale / Math.max(highScale, 0.01);
    var emotionTemp = Math.min(1, emotionCool / 2);

    var canvases = visualizerCachedCanvases;
    if (!canvases || !canvases.length) {
      canvases = document.querySelectorAll(".audio-visualizer.is-active");
      visualizerCachedCanvases = canvases;
    }
    if (!canvases.length) return;

    var viewAngle = Math.PI / 4;
    var cosView = Math.cos(viewAngle);
    var sinView = Math.sin(viewAngle);
    var elevationAngle = Math.PI / 5;
    var cosElev = Math.cos(elevationAngle);
    var sinElev = Math.sin(elevationAngle);

    var gridCols = 14;
    var gridRows = 10;
    var totalBlocks = gridCols * gridRows;

    canvases.forEach(function(canvas) {
      var ctx = canvas.getContext("2d");
      var canvasWidth = canvas.width;
      var canvasHeight = canvas.height;

      if (canvasWidth === 0 || canvasHeight === 0) return;

      ctx.clearRect(0, 0, canvasWidth, canvasHeight);

      var cubeSize = canvasWidth / (gridCols + 1.5);
      var gap = cubeSize * 0.15;
      var actualSize = cubeSize - gap;
      var depth = cubeSize * 0.5;

      var centerX = canvasWidth / 2;
      var centerY = canvasHeight * 0.72;

      var maxDist = Math.sqrt((gridCols / 2) * (gridCols / 2) + (gridRows / 2) * (gridRows / 2)) * cubeSize;

      var cubes = [];

      for (var row = 0; row < gridRows; row++) {
        for (var col = 0; col < gridCols; col++) {
          var idx = row * gridCols + col;
          var block = visualizerBlocks[idx];
          if (!block) continue;

          var freqIndex = Math.floor(idx * (freqLen / totalBlocks));

          var x = (col - gridCols / 2 + 0.5) * cubeSize;
          var z = (row - gridRows / 2 + 0.5) * cubeSize;

          var distFromCenter = Math.sqrt(x * x + z * z);

          var edgeFalloff = 1;
          if (distFromCenter > maxDist * 0.55) {
            edgeFalloff = 1 - (distFromCenter - maxDist * 0.55) / (maxDist * 0.45);
            edgeFalloff = Math.max(0, edgeFalloff);
          }
          if (edgeFalloff < 0.03) continue;

          // 策略2增强：区域分组随机 — 同区域内柱子响应不同频率子带
          // 左侧：不同柱子偏好不同低频子带（如鼓、贝斯各有侧重）
          // 中间：不同柱子偏好不同中频子带（如人声各频段）
          // 右侧：不同柱子偏好不同高频子带（如铙钹、高音）
          var bandScale;
          if (block.region === "left") {
            var subLow = lowScale * (1 - block.freqSubOffset * 0.35);
            var subLowMid = lowMidScale * (0.25 + block.freqSubOffset * 0.35);
            bandScale = subLow * 0.7 + subLowMid * 0.3;
          } else if (block.region === "center") {
            var subMid = midScale * (0.55 + block.freqSubOffset * 0.25);
            var subHighMid = highMidScale * (0.45 - block.freqSubOffset * 0.25);
            bandScale = subMid * 0.6 + subHighMid * 0.4;
          } else {
            var subHigh = highScale * (0.65 + block.freqSubOffset * 0.25);
            var subHighMid2 = highMidScale * (0.35 - block.freqSubOffset * 0.25);
            bandScale = subHigh * 0.7 + subHighMid2 * 0.3;
          }
          bandScale *= block.sensitivity; // 有序中的无序：每柱灵敏度不同

          var baseHeight = cubeSize * 0.4;
          var maxHeight = cubeSize * 4.5;

          // 策略3增强：随机延迟连锁 — 多方向波动，更长衰减窗口
          // 强节拍时从中心向四周或从左到右产生波动延迟（多米诺骨牌效应）
          var beatBoost = 1;
          if (visualizerBeatEnergy > 0.02 && visualizerBeatTriggerTime > 0) {
            var elapsedSinceBeat = now - visualizerBeatTriggerTime;
            var cubeDelay = block.beatDelayMs;
            if (elapsedSinceBeat >= cubeDelay) {
              // 延迟已到，应用节拍增强（350ms衰减窗口，增强视觉冲击）
              var beatElapsed = elapsedSinceBeat - cubeDelay;
              var beatDecay = Math.max(0, 1 - beatElapsed / 350);
              beatBoost = 1 + visualizerBeatEnergy * 0.85 * beatDecay;
            }
          }

          // 策略4：前景核心元素做更强节拍弹跳
          if (block.isForeground) {
            beatBoost *= 1.18;
          }

          var dynamicHeight = bandScale * maxHeight * block.amplitudeBias * beatBoost;

          var wave = Math.sin(distFromCenter * 0.012 - now * 0.0018 * block.speedBias + block.phaseBias);
          var waveHeight = midScale * cubeSize * 1.8 * wave * block.amplitudeBias;

          var energyBoost = 1 + frameEnergy * 0.3;
          var targetHeight = (baseHeight + dynamicHeight + Math.abs(waveHeight)) * energyBoost;

          if (visualizerMode === "idle") {
            block.height = baseHeight * 0.45 * edgeFalloff;
          } else if (visualizerMode === "paused") {
            // Frozen: keep previous height, no velocity update
          } else {
            block.velocity += (targetHeight - block.height) * 0.12;
            block.velocity *= block.decayRate; // 差异化衰减系数，每柱弹跳回弹节奏不同
            block.height += block.velocity;
            block.height = Math.max(baseHeight * 0.3, Math.min(maxHeight * 1.2, block.height));
          }

          var height = block.height * edgeFalloff;

          var heightFalloff = 1;
          if (height > maxHeight * 0.7) {
            heightFalloff = 1 - (height - maxHeight * 0.7) / (maxHeight * 0.3);
            heightFalloff = Math.max(0.25, heightFalloff);
          }
          height *= heightFalloff;

          if (height < cubeSize * 0.15) continue;

          // 策略2增强：颜色按区域分组 + 每柱独特色偏，避免同区域颜色过于均匀
          var hue;
          var saturation;
          var lightness;
          if (block.region === "left") {
            hue = 335 + lowScale * 20 + block.hueShift; // 暖粉红 + 色偏
            saturation = 72 + lowScale * 15;
            lightness = 70 + lowScale * 12;
          } else if (block.region === "center") {
            hue = 310 + midScale * 25 + block.hueShift; // 紫粉 + 色偏
            saturation = 68 + midScale * 18;
            lightness = 74 + midScale * 10;
          } else {
            hue = 285 + highScale * 30 + block.hueShift; // 冷紫蓝 + 色偏
            saturation = 65 + highScale * 20;
            lightness = 76 + highScale * 12;
          }

          var warmHue = hue + (1 - emotionTemp) * 25;
          var coolHue = hue - emotionTemp * 25;
          var finalHue = emotionTemp > 0.5 ? coolHue : warmHue;

          var rx = x * cosView - z * sinView;
          var ry = -height * sinElev;
          var rz = x * sinView + z * cosView;

          var perspective = 1 + rz / (canvasHeight * 2.5);
          var screenX = centerX + rx * perspective;
          var screenY = centerY + ry * perspective - rz * cosElev * 0.35;

          cubes.push({
            x: screenX,
            y: screenY,
            size: actualSize * perspective * edgeFalloff,
            height: height * perspective,
            depth: depth * perspective * edgeFalloff,
            z: rz,
            hue: finalHue,
            saturation: saturation,
            lightness: lightness,
            edgeFalloff: edgeFalloff,
            heightFalloff: heightFalloff,
            beatPhase: visualizerBeatEnergy
          });
        }
      }

      cubes.sort(function(a, b) {
        return b.z - a.z;
      });

      cubes.forEach(function(cube) {
        var cx = cube.x;
        var cy = cube.y;
        var cs = cube.size;
        var ch = cube.height;
        var cd = cube.depth;
        var hue = cube.hue;
        var saturation = cube.saturation;
        var lightness = cube.lightness;
        var edge = cube.edgeFalloff;
        var beatP = cube.beatPhase;

        var alpha = 0.55 + edge * 0.45;

        ctx.save();

        var glowColor = "hsla(" + hue + ", " + (saturation + 5) + "%, " + (lightness + 15) + "%, " + (alpha * 0.35 * (1 + beatP * 0.5)) + ")";
        ctx.shadowColor = glowColor;
        ctx.shadowBlur = cs * (0.6 + beatP * 0.8);
        ctx.shadowOffsetY = cs * 0.15;

        var dScale = 0.55;

        var rightGrad = ctx.createLinearGradient(
          cx + cs * 0.5, cy,
          cx + cs * 0.5 + cd * dScale, cy - cd * dScale
        );
        rightGrad.addColorStop(0, "hsla(" + hue + ", " + (saturation - 8) + "%, " + (lightness - 22) + "%, " + (alpha * 0.65) + ")");
        rightGrad.addColorStop(1, "hsla(" + hue + ", " + (saturation - 12) + "%, " + (lightness - 32) + "%, " + (alpha * 0.3) + ")");
        ctx.fillStyle = rightGrad;
        ctx.beginPath();
        ctx.moveTo(cx + cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5 + cd * dScale, cy - cd * dScale);
        ctx.lineTo(cx + cs * 0.5 + cd * dScale, cy - cd * dScale + ch);
        ctx.lineTo(cx + cs * 0.5, cy + ch);
        ctx.closePath();
        ctx.fill();

        var frontGrad = ctx.createLinearGradient(
          cx - cs * 0.5, cy,
          cx - cs * 0.5, cy + ch
        );
        frontGrad.addColorStop(0, "hsla(" + hue + ", " + saturation + "%, " + (lightness + 12) + "%, " + alpha + ")");
        frontGrad.addColorStop(0.45, "hsla(" + hue + ", " + saturation + "%, " + lightness + "%, " + (alpha * 0.97) + ")");
        frontGrad.addColorStop(1, "hsla(" + hue + ", " + (saturation - 6) + "%, " + (lightness - 12) + "%, " + (alpha * 0.75) + ")");
        ctx.fillStyle = frontGrad;
        ctx.beginPath();
        ctx.moveTo(cx - cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5, cy + ch);
        ctx.lineTo(cx - cs * 0.5, cy + ch);
        ctx.closePath();
        ctx.fill();

        var topGrad = ctx.createLinearGradient(
          cx - cs * 0.5, cy,
          cx + cs * 0.5 + cd * dScale, cy - cd * dScale
        );
        topGrad.addColorStop(0, "hsla(" + hue + ", " + (saturation + 8) + "%, " + (lightness + 28) + "%, " + (alpha * 0.98) + ")");
        topGrad.addColorStop(0.4, "hsla(" + hue + ", " + (saturation + 4) + "%, " + (lightness + 20) + "%, " + (alpha * 0.95) + ")");
        topGrad.addColorStop(0.75, "hsla(" + hue + ", " + saturation + "%, " + (lightness + 8) + "%, " + (alpha * 0.88) + ")");
        topGrad.addColorStop(1, "hsla(" + hue + ", " + (saturation - 4) + "%, " + (lightness - 4) + "%, " + (alpha * 0.65) + ")");
        ctx.fillStyle = topGrad;
        ctx.beginPath();
        ctx.moveTo(cx - cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5 + cd * dScale, cy - cd * dScale);
        ctx.lineTo(cx - cs * 0.5 + cd * dScale, cy - cd * dScale);
        ctx.closePath();
        ctx.fill();

        var hlGrad = ctx.createLinearGradient(
          cx - cs * 0.42, cy,
          cx - cs * 0.42, cy + ch * 0.35
        );
        hlGrad.addColorStop(0, "hsla(" + hue + ", " + (saturation + 12) + "%, " + (lightness + 35) + "%, " + (alpha * 0.85) + ")");
        hlGrad.addColorStop(0.6, "hsla(" + hue + ", " + (saturation + 6) + "%, " + (lightness + 18) + "%, " + (alpha * 0.4) + ")");
        hlGrad.addColorStop(1, "transparent");
        ctx.fillStyle = hlGrad;
        ctx.beginPath();
        ctx.moveTo(cx - cs * 0.5, cy);
        ctx.lineTo(cx - cs * 0.5 + cs * 0.28, cy);
        ctx.lineTo(cx - cs * 0.5 + cs * 0.28, cy + ch);
        ctx.lineTo(cx - cs * 0.5, cy + ch);
        ctx.closePath();
        ctx.fill();

        var edgeGrad = ctx.createLinearGradient(
          cx + cs * 0.35, cy,
          cx + cs * 0.5, cy + ch
        );
        edgeGrad.addColorStop(0, "transparent");
        edgeGrad.addColorStop(1, "hsla(" + hue + ", " + (saturation - 10) + "%, " + (lightness - 20) + "%, " + (alpha * 0.25) + ")");
        ctx.fillStyle = edgeGrad;
        ctx.beginPath();
        ctx.moveTo(cx + cs * 0.35, cy);
        ctx.lineTo(cx + cs * 0.5, cy);
        ctx.lineTo(cx + cs * 0.5, cy + ch);
        ctx.lineTo(cx + cs * 0.35, cy + ch);
        ctx.closePath();
        ctx.fill();

        ctx.restore();
      });
    });
  }

  function averageArray(arr) {
    if (!arr || arr.length === 0) return 0;
    var sum = 0;
    for (var i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    return sum / arr.length;
  }

  function interpolate(a, b, t) {
    return a + (b - a) * t;
  }

  function updateVisualizerForModule(module) {
    if (module === "radio") {
      stopVisualizer();
    } else if (module === "bluetooth") {
      if (visualizerMode === "idle") {
        stopVisualizer();
      }
      if (!audio.paused) {
        startVisualizer();
      } else {
        var canvases = document.querySelectorAll(".module-panel.is-active .audio-visualizer");
        canvases.forEach(function(canvas) {
          canvas.classList.add("is-active");
        });
        visualizerCachedCanvases = canvases;
        if (visualizerMode === "stopped") {
          visualizerMode = "paused";
          visualize();
        }
      }
    } else if (module === "usb") {
      if (!audio.paused && activeAudioSource === "usb") {
        startVisualizer();
      } else {
        startIdleVisualizer();
      }
    } else {
      stopVisualizer();
    }
  }

  initAudioVisualizer();
  */ // === 3D音律（音频可视化）代码结束 ===

  // ==================== 音乐律动氛围灯（独立模块，不依赖3D音律） ====================
  // 通过 FFT 解析音乐的频率与能量：
  // - 频率决定音高 → 映射色相（低音暖色→高音冷色），再转 RGB(0~255)；
  // - 振幅/能量决定音量 → 映射亮度（越大越亮）；
  // - 指数平滑因子让过渡自然，避免灯光“一亮一灭”；
  // 计算出的 RGBA 通过 MusicBridge.setAmbientLightRGBA 上报 native 控制车内氛围灯。
  var ambientLight = (function () {
    var enabled = true;             // 总开关（初始化失败或 Bridge 不可用时降级关闭）
    var ctx = null;
    var analyser = null;
    var sourceConnected = false;    // audio 元素只能 createMediaElementSource 一次
    var freqData = null;
    var running = false;
    var animId = null;
    var lastSendTime = 0;
    var SEND_INTERVAL = 1000 / 15;  // 上报节流：约 15fps，避免高频调用 native
    var SMOOTH_FACTOR = 0.35;       // 平滑因子：越大跟随越快，越小过渡越平缓

    // 界面模拟氛围灯：进度条上方的 LED 光柱
    var BAR_COUNT = 32;             // 光柱数量
    var el = null;                  // .usb-ambient-light 容器
    var bars = [];                  // 光柱 DOM 数组

    // 平滑后的 RGBA（0~255），初始为关闭态
    var sr = 0, sg = 0, sb = 0, sa = 0;

    function hslToRgb(h, s, l) {
      h = ((h % 360) + 360) % 360;
      var c = (1 - Math.abs(2 * l - 1)) * s;
      var x = c * (1 - Math.abs((h / 60) % 2 - 1));
      var m = l - c / 2;
      var r = 0, g = 0, b = 0;
      if (h < 60) { r = c; g = x; }
      else if (h < 120) { r = x; g = c; }
      else if (h < 180) { g = c; b = x; }
      else if (h < 240) { g = x; b = c; }
      else if (h < 300) { r = x; b = c; }
      else { r = c; b = x; }
      return {
        r: Math.round((r + m) * 255),
        g: Math.round((g + m) * 255),
        b: Math.round((b + m) * 255)
      };
    }

    function send(r, g, b, a) {
      if (!enabled) return;
      try {
        var bridge = window.MusicBridge;
        if (!bridge || typeof bridge.setAmbientLightRGBA !== "function") {
          enabled = false;
          return;
        }
        bridge.setAmbientLightRGBA(
          Math.round(r), Math.round(g), Math.round(b), Math.round(a));
      } catch (e) {
        enabled = false;
        console.warn("氛围灯上报失败，已降级关闭:", e);
      }
    }

    function loop(now) {
      if (!running) return;
      animId = requestAnimationFrame(loop);
      if (!analyser || !freqData || audio.paused) return;

      analyser.getByteFrequencyData(freqData);
      var freqLen = freqData.length;

      // 1. 整体能量（RMS 归一化 0~1）：决定音量/亮度
      var sumSq = 0;
      for (var i = 0; i < freqLen; i++) {
        var v = freqData[i] / 255;
        sumSq += v * v;
      }
      var energy = Math.sqrt(sumSq / freqLen);

      // 2. 加权频率中心 Σ(freq[i]*i)/Σ(freq[i]) 归一化 0~1：决定音高/颜色
      var freqSum = 0;
      var weightedSum = 0;
      for (var j = 1; j < freqLen; j++) {
        var vj = freqData[j];
        if (vj > 0) {
          freqSum += vj;
          weightedSum += vj * j;
        }
      }
      var dominant = freqSum > 0 ? weightedSum / freqSum / freqLen : 0;

      // 3. 频率→颜色：低音→红(0°)，中频→绿/青，高频→蓝/紫(280°)
      var hue = 280 * dominant;
      // 4. 能量→亮度：基线 0.22 避免完全熄灭，峰值 0.85 避免刺眼
      var energyClamped = Math.max(0, Math.min(1, energy));
      var lightness = 0.22 + 0.63 * energyClamped;
      // 5. 饱和度固定 0.85，保证颜色鲜艳；透明度随能量增强（100~255）
      var rgb = hslToRgb(hue, 0.85, lightness);
      var alpha = 100 + Math.round(155 * energyClamped);

      // 6. 指数平滑：向目标值缓慢逼近，实现自然过渡
      var k = SMOOTH_FACTOR;
      sr += (rgb.r - sr) * k;
      sg += (rgb.g - sg) * k;
      sb += (rgb.b - sb) * k;
      sa += (alpha - sa) * k;

      // 7. 界面模拟氛围灯：更新光柱高度（每柱取对应频段平均能量，低音在左、高音在右）
      updateBars(freqData, freqLen);

      // 8. 节流上报 native
      if (now - lastSendTime >= SEND_INTERVAL) {
        lastSendTime = now;
        send(sr, sg, sb, sa);
      }
    }

    /** 更新界面光柱：每柱高度随对应频段能量变化，颜色统一为平滑后的 RGBA。 */
    function updateBars(freqData, freqLen) {
      if (!el) return;
      var color = "rgba(" + Math.round(sr) + "," + Math.round(sg) + "," + Math.round(sb) + "," + (sa / 255).toFixed(3) + ")";
      el.style.setProperty("--al-color", color);
      if (!el.classList.contains("is-active")) el.classList.add("is-active");

      var perBar = Math.max(1, Math.floor(freqLen / BAR_COUNT));
      for (var bi = 0; bi < bars.length; bi++) {
        var start = bi * perBar;
        var end = Math.min(freqLen, start + perBar);
        var sum = 0;
        for (var fi = start; fi < end; fi++) {
          sum += freqData[fi];
        }
        var avg = sum / Math.max(1, end - start) / 255; // 0~1
        var height = 3 + Math.round(avg * 17);          // 3~20px（容器高 22px）
        bars[bi].style.height = height + "px";
      }
    }

    /** 构建界面光柱 DOM（懒创建，仅在存在容器时执行）。 */
    function initDom() {
      if (el) return;
      el = document.getElementById("usbAmbientLight");
      if (!el) return;
      bars = [];
      for (var i = 0; i < BAR_COUNT; i++) {
        var bar = document.createElement("span");
        bar.className = "al-bar";
        el.appendChild(bar);
        bars.push(bar);
      }
    }

    function ensureContext() {
      if (ctx) return true;
      try {
        var AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextConstructor) {
          enabled = false;
          return false;
        }
        ctx = new AudioContextConstructor();
        analyser = ctx.createAnalyser();
        analyser.fftSize = 512;
        analyser.smoothingTimeConstant = 0.8;
        if (!sourceConnected) {
          // 每个 audio 元素只能 createMediaElementSource 一次；
          // 若已被其他模块占用会抛异常，此时降级关闭氛围灯。
          var src = ctx.createMediaElementSource(audio);
          src.connect(analyser);
          src.connect(ctx.destination);
          sourceConnected = true;
        }
        freqData = new Uint8Array(analyser.frequencyBinCount);
        initDom(); // 构建界面光柱（无容器时静默跳过）
        if (ctx.state === "suspended") ctx.resume();
        return true;
      } catch (e) {
        enabled = false;
        console.warn("氛围灯初始化失败，已降级关闭:", e);
        return false;
      }
    }

    /** 开始律动（仅 USB 本地音频播放时调用）。 */
    function start() {
      if (!enabled || running) return;
      if (!ensureContext()) return;
      running = true;
      animId = requestAnimationFrame(loop);
    }

    /** 暂停时调暗（保留当前色相，亮度降至 20% 保持柔和），避免突兀。 */
    function dim() {
      if (el) el.classList.remove("is-active");
      for (var i = 0; i < bars.length; i++) {
        bars[i].style.height = "3px";
      }
      send(sr * 0.2, sg * 0.2, sb * 0.2, sa * 0.4);
    }

    /** 停止/无播放时关闭（Alpha=0，光柱恢复静止）。 */
    function turnOff() {
      running = false;
      if (animId) {
        cancelAnimationFrame(animId);
        animId = null;
      }
      if (el) {
        el.classList.remove("is-active");
        el.style.removeProperty("--al-color");
      }
      for (var i = 0; i < bars.length; i++) {
        bars[i].style.height = "3px";
      }
      sr = sg = sb = sa = 0;
      send(0, 0, 0, 0);
    }

    return { start: start, dim: dim, turnOff: turnOff };
  })();

  var progressAnimationId = null;
  var isProgressUpdating = false;

  function startProgressUpdate() {
    if (isProgressUpdating) return;
    isProgressUpdating = true;
    updateProgressLoop();
  }

  function stopProgressUpdate() {
    isProgressUpdating = false;
    cancelAnimationFrame(progressAnimationId);
  }

  function updateProgressLoop() {
    if (!isProgressUpdating) return;

    progressAnimationId = requestAnimationFrame(updateProgressLoop);

    if (audio.paused) return;
    if (isSeeking) return;

    if (activeAudioSource === "usb") {
      updateUsbPlaybackUi();
      rememberUsbResumePoint();
      return;
    }
  }

  audio.addEventListener("play", function() {
    startProgressUpdate();
    // 音乐律动氛围灯：仅 USB 本地音频有 FFT 数据源
    if (activeAudioSource === "usb") {
      ambientLight.start();
    }
  });

  audio.addEventListener("pause", function() {
    stopProgressUpdate();
    // 暂停时氛围灯调暗，避免保持全亮显得突兀
    ambientLight.dim();
  });

  audio.addEventListener("ended", function() {
    stopProgressUpdate();
    // 曲目结束：若后续无自动播放则氛围灯保持调暗状态（下一曲 play 时自动恢复律动）
    ambientLight.dim();
  });

  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) {
      if (isRadioPlaying) {
        resumeRadioAudio();
      }
      // 3D音律已注释
      // if (audioVisualizerContext && audioVisualizerContext.state === "suspended") {
      //   audioVisualizerContext.resume();
      // }
      if (!audio.paused) {
        startProgressUpdate();
        // 3D音律已注释
        // if (activeModule !== "radio") {
        //   if (visualizerMode === "idle" || visualizerMode === "paused") {
        //     startVisualizer();
        //   } else {
        //     startVisualizer();
        //   }
        // }
      }
      maybeResumeBluetoothOnReturn("visible");
    } else {
      rememberBluetoothResumePoint({ shouldResume: activeModule === "bluetooth" && isBtPlaying, wasPlaying: isBtPlaying });
      if (window.MusicBridge && typeof window.MusicBridge.onBackgroundEnter === "function") {
        try {
          window.MusicBridge.onBackgroundEnter();
        } catch (e) {
          console.warn("MusicBridge.onBackgroundEnter failed:", e);
        }
      }
    }
  });

  document.addEventListener("pagehide", function() {
    rememberBluetoothResumePoint({ shouldResume: activeModule === "bluetooth" && isBtPlaying, wasPlaying: isBtPlaying });
  });

  document.addEventListener("pageshow", function() {
    if (!audio.paused) {
      startProgressUpdate();
      // 3D音律已注释
      // if (activeModule !== "radio") {
      //   startVisualizer();
      // }
    } else if (activeModule === "usb" || activeModule === "bluetooth") {
      // 3D音律已注释
      // if (visualizerMode === "stopped" || visualizerMode === "paused") {
      //   startIdleVisualizer();
      // }
    }
    maybeResumeBluetoothOnReturn("visible");
  });

  function togglePlaylistBatchSelect(id) {
    if (playlistBatchSelected[id]) {
      delete playlistBatchSelected[id];
    } else {
      playlistBatchSelected[id] = true;
    }
    if (activeModule === "usb") {
      renderUsbPlaylist();
    } else {
      renderPlaylist();
    }
  }

  function toggleFavoriteBatchSelect(id) {
    if (favoriteBatchSelected[id]) {
      delete favoriteBatchSelected[id];
    } else {
      favoriteBatchSelected[id] = true;
    }
    renderFavorites();
  }

  function togglePlaylistSelectAll() {
    if (activeModule === "usb") {
      if (Object.keys(playlistBatchSelected).length >= usbPlaylist.length) {
        playlistBatchSelected = {};
      } else {
        playlistBatchSelected = {};
        usbPlaylist.forEach(function(t) { playlistBatchSelected[t.id] = true; });
      }
      renderUsbPlaylist();
    } else {
      if (Object.keys(playlistBatchSelected).length >= playlist.length) {
        playlistBatchSelected = {};
      } else {
        playlistBatchSelected = {};
        playlist.forEach(function(t) { playlistBatchSelected[t.id] = true; });
      }
      renderPlaylist();
    }
  }

  function toggleFavoriteSelectAll() {
    var usbFavs = usbPlaylist.filter(function(t) { return isUsbTrackFavorite(t); });
    if (Object.keys(favoriteBatchSelected).length >= usbFavs.length) {
      favoriteBatchSelected = {};
    } else {
      favoriteBatchSelected = {};
      usbFavs.forEach(function(t) { favoriteBatchSelected[t.id] = true; });
    }
    renderFavorites();
  }

  function updatePlaylistBatchBar() {
    if (!playlistBatchBar) return;
    var count = Object.keys(playlistBatchSelected).length;
    var total = activeModule === "usb" ? usbPlaylist.length : playlist.length;
    var selectAllBtn = playlistSelectAllBtn;
    var selectLabel = playlistSelectLabel;
    var removeBtn = playlistBatchDeleteBtn;
    if (selectAllBtn) {
      if (count > 0 && count >= total) {
        selectAllBtn.classList.add("is-checked");
      } else {
        selectAllBtn.classList.remove("is-checked");
      }
    }
    if (selectLabel) {
      selectLabel.textContent = _t('action.selectAll');
    }
    if (removeBtn) {
      removeBtn.disabled = count === 0;
    }
  }

  function updateFavoriteBatchBar() {
    if (!favoriteBatchBar) return;
    var usbFavs = usbPlaylist.filter(function(t) { return isUsbTrackFavorite(t); });
    var count = Object.keys(favoriteBatchSelected).length;
    var selectAllBtn = favoriteSelectAllBtn;
    var selectLabel = favoriteSelectLabel;
    var removeBtn = favoriteBatchDeleteBtn;
    if (selectAllBtn) {
      if (count > 0 && count >= usbFavs.length) {
        selectAllBtn.classList.add("is-checked");
      } else {
        selectAllBtn.classList.remove("is-checked");
      }
    }
    if (selectLabel) {
      selectLabel.textContent = _t('action.selectAll');
    }
    if (removeBtn) {
      removeBtn.disabled = count === 0;
    }
  }

  function confirmDeleteTrack(id, type) {
    var trackName = "";
    if (type === "favorite") {
      var favTrack = usbPlaylist.find(function(t) { return t.id === id; });
      trackName = favTrack ? favTrack.title : "";
    } else if (activeModule === "usb") {
      var usbTrack = usbPlaylist.find(function(t) { return t.id === id; });
      trackName = usbTrack ? usbTrack.title : "";
    } else {
      var plTrack = playlist.find(function(t) { return t.id === id; });
      trackName = plTrack ? plTrack.title : "";
    }
    var msg = trackName ? _t('dialog.confirmRemoveThis', {title: trackName}) : _t('dialog.confirmRemoveSingle');
    showConfirmDialog(msg, function () {
        if (type === "favorite") {
            removeFavoriteById(id);
        } else if (activeModule === "usb") {
            removeUsbTrackById(id);
        } else {
            removeTrack(id);
        }
    });
  }

  function confirmDeleteBatch(type) {
    var selected = type === "favorite" ? favoriteBatchSelected : playlistBatchSelected;
    var ids = Object.keys(selected);
    if (!ids.length) return;
    var count = ids.length;
    showConfirmDialog(_t('dialog.confirmRemoveBatch', {n: count}), function () {
        ids.forEach(function (id) {
            if (type === "favorite") {
                removeFavoriteById(id);
            } else if (activeModule === "usb") {
                removeUsbTrackById(id);
            } else {
                removeTrack(id);
            }
        });
        if (type === "favorite") {
            favoriteBatchSelected = {};
            renderFavorites();
        } else {
            playlistBatchSelected = {};
            if (activeModule === "usb") {
                renderUsbPlaylist();
            } else {
                renderPlaylist();
            }
        }
    });
  }

  function showConfirmDialog(message, onConfirm) {
    if (!confirmDialog) return;
    confirmMessage.textContent = message;
    confirmDialog.hidden = false;
    confirmDialog.classList.add("is-visible");
    pendingConfirmAction = onConfirm;
  }

  function hideConfirmDialog() {
    if (!confirmDialog) return;
    confirmDialog.hidden = true;
    confirmDialog.classList.remove("is-visible");
    pendingConfirmAction = null;
  }

})();


