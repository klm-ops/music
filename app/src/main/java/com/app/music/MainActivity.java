package com.app.music;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 音乐播放器主Activity
 * 核心功能：通过WebView加载Web音乐播放器页面，实现本地音乐播放、
 * 蓝牙音频设备连接（A2DP输出/A2DP Sink接收）、USB音乐扫描播放、
 * JS-Native桥接通信、电话状态监听等功能。
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MusicPlayback";
    private static final String PLAYER_URL = "file:///android_asset/player/index.html"; // Web播放器本地资源地址
    private static final String USB_NOTIFICATION_CHANNEL_ID = "usb_device_status"; // USB通知渠道ID
    private static final int USB_DISCONNECTED_NOTIFICATION_ID = 9001; // USB断开通知ID
    private static final int PROFILE_A2DP_SINK = 11; // 蓝牙A2DP Sink Profile常量
    private static final int PROFILE_AVRCP_CONTROLLER = 12; // 蓝牙AVRCP Controller Profile常量
    private static final int AVRCP_PASS_THROUGH_STATE_PRESS = 0; // AVRCP按键按下状态
    private static final int AVRCP_PASS_THROUGH_STATE_RELEASE = 1; // AVRCP按键释放状态
    private static final int AVRCP_CMD_ID_REWIND = 0x48; // AVRCP后退命令ID
    private static final int AVRCP_CMD_ID_FAST_FORWARD = 0x49; // AVRCP快进命令ID
    private static final int AVRCP_CMD_ID_FORWARD = 0x4B; // AVRCP下一曲命令ID
    private static final int AVRCP_CMD_ID_BACKWARD = 0x4C; // AVRCP上一曲命令ID
    private static final int AVRCP_CMD_ID_PLAY = 0x44; // AVRCP播放命令ID
    private static final int AVRCP_CMD_ID_PAUSE = 0x46; // AVRCP暂停命令ID
    private static final int A2DP_SINK_STATE_PLAYING = 10; // A2DP Sink播放中状态
    private static final int A2DP_SINK_STATE_NOT_PLAYING = 11; // A2DP Sink未播放状态
    private static final int DEFAULT_STATUS_BAR_TOP_PX = 24; // 默认状态栏顶部像素值
    // 蓝牙A2DP Sink连接状态变化广播Action
    private static final String ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    // 蓝牙A2DP Sink播放状态变化广播Action
    private static final String ACTION_A2DP_SINK_PLAYING_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED";
    // 蓝牙AVRCP Controller播放状态变化广播Action
    private static final String ACTION_AVRCP_CONTROLLER_PLAYBACK_STATE_CHANGED =
            "android.bluetooth.avrcp-controller.profile.action.PLAYBACK_STATE_CHANGED";
    // 蓝牙AVRCP Controller曲目事件广播Action
    private static final String ACTION_AVRCP_CONTROLLER_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    // AVRCP Controller播放状态Extra键
    private static final String EXTRA_AVRCP_CONTROLLER_PLAYBACK =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";
    // AVRCP Controller元数据Extra键
    private static final String EXTRA_AVRCP_CONTROLLER_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    // 蓝牙AVRCP播放状态变化广播Action（旧版）
    private static final String ACTION_AVRCP_PLAYBACK_STATE_CHANGED =
            "android.bluetooth.avrcp.profile.action.PLAYBACK_STATE_CHANGED";
    // 蓝牙AVRCP曲目事件广播Action（旧版）
    private static final String ACTION_AVRCP_TRACK_EVENT =
            "android.bluetooth.avrcp.profile.action.TRACK_EVENT";
    // 系统音量变化广播Action
    private static final String ACTION_VOLUME_CHANGED =
            "android.media.VOLUME_CHANGED_ACTION";
    private static final long USB_MIN_AUDIO_FILE_BYTES = 100L * 1024L; // USB音频文件最小大小（100KB）
    private static final long BLUETOOTH_FAST_CONNECT_TIMEOUT_MS = 3000L; // 蓝牙快速连接超时（3秒）
    private static final long BLUETOOTH_CONNECT_CONFIRM_TIMEOUT_MS = 12000L; // 蓝牙连接确认超时（12秒）
    private static final long BLUETOOTH_FAST_CONNECT_RETRY_MS = 500L; // 蓝牙快速连接重试间隔（500ms）
    private static final long BLUETOOTH_AUTO_RECONNECT_DELAY_MS = 2000L; // 蓝牙自动重连基础延迟（2秒）
    private static final int BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS = 8; // 蓝牙自动重连最大次数
    private static final long BLUETOOTH_STABLE_SESSION_TARGET_MS = 2L * 60L * 60L * 1000L; // 蓝牙稳定会话目标时长（2小时）
    private static final int BLUETOOTH_CONNECT_SAMPLE_LIMIT = 60; // 蓝牙连接采样记录上限
    private static final long BLUETOOTH_DISCOVERY_MIN_DURATION_MS = 8000L; // 蓝牙搜索最短时长（8秒）
    private static final long BLUETOOTH_DISCOVERY_MAX_DURATION_MS = 15000L; // 蓝牙搜索最长时长（15秒）
    private static final long BLUETOOTH_DEVICE_EXPIRY_MS = 60000L; // 蓝牙设备过期时间（60秒）
    private static final long BLUETOOTH_CONNECTION_STATUS_CHECK_INTERVAL_MS = 3000L; // 蓝牙连接状态检查间隔（3秒）
    private static final String BT_STATE_IDLE = "idle"; // 蓝牙状态机：空闲
    private static final String BT_STATE_DISCOVERING = "discovering"; // 蓝牙状态机：搜索中
    private static final String BT_STATE_CONNECTING = "connecting"; // 蓝牙状态机：连接中
    private static final String BT_STATE_CONNECTED = "connected"; // 蓝牙状态机：已连接
    private static final String BT_STATE_PLAYING = "playing"; // 蓝牙状态机：播放中
    private static final String BT_STATE_PAUSED = "paused"; // 蓝牙状态机：已暂停
    private static final String BT_STATE_DISCONNECTED = "disconnected"; // 蓝牙状态机：已断开
    private static final String BT_STATE_ERROR = "error"; // 蓝牙状态机：错误
    private static final long BLUETOOTH_RECONNECT_BACKOFF_BASE_MS = 1000L; // 蓝牙重连退避基础时间（1秒）
    private static final int BLUETOOTH_COVER_MAX_EDGE_PX = 512; // 蓝牙专辑封面最大边长（像素，超过则等比缩放）
    private static final int BLUETOOTH_COVER_JPEG_QUALITY = 90; // 蓝牙专辑封面JPEG压缩质量（0-100）
    private static final String[] USB_AUDIO_EXTENSIONS = { // 支持的USB音频文件扩展名列表
            ".aac", ".mp3", ".flac", ".ape", ".wav", ".wma", ".ogg", ".mpeg", ".mpg", ".mp2", ".mp1",
            ".m4a", ".m4b", ".opus", ".aiff", ".aif", ".dsf", ".dff", ".wv", ".tta", ".tak", ".mid", ".midi"
    };
    private static final int USB_COVER_MAX_EDGE_PX = 512; // USB专辑封面最大边长（像素，超过则采样压缩）
    private static final int USB_COVER_JPEG_QUALITY = 85; // USB专辑封面JPEG压缩质量（0-100）
    private static final long USB_SCAN_PROGRESS_INTERVAL_MS = 200L; // USB扫描进度上报间隔（200ms）
    private static final String USB_FAVORITES_SYNC_DIR = "usb_favorites"; // USB收藏同步目录名
    private static final String USB_FAVORITES_INDEX_FILE = "favorites_index.json"; // USB收藏索引文件名
    private static final long VHAL_KEY_POLL_INTERVAL_MS = 200L; // VHAL物理按键信号轮询间隔（200ms）

    // WebView控件，用于加载Web音乐播放器页面
    private WebView musicWebView;
    @Nullable // 文件选择回调，用于处理Web端文件选择请求
    private ValueCallback<Uri[]> filePathCallback;
    @Nullable // 地理权限请求回调
    private GeolocationPermissions.Callback pendingGeolocationCallback;
    @Nullable // 地理权限请求来源地址
    private String pendingGeolocationOrigin;
    @Nullable // 蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;
    @Nullable // 蓝牙A2DP Profile代理（作为音频输出源）
    private BluetoothA2dp bluetoothA2dp;
    @Nullable // 蓝牙A2DP Sink Profile代理（作为音频接收端）
    private BluetoothProfile bluetoothA2dpSink;
    @Nullable // 蓝牙AVRCP Controller Profile代理（用于媒体控制）
    private BluetoothProfile bluetoothAvrcpController;
    private final Map<String, BluetoothDevice> knownBluetoothDevices = new LinkedHashMap<>(); // 已发现的蓝牙设备列表
    private final Map<String, String> bluetoothDeviceNames = new LinkedHashMap<>(); // 蓝牙设备名称映射
    private final Map<String, Integer> bluetoothDeviceRssi = new LinkedHashMap<>(); // 蓝牙设备信号强度映射
    private final Map<String, Long> bluetoothDeviceLastSeen = new LinkedHashMap<>(); // 蓝牙设备最后发现时间映射
    private final Handler bluetoothHandler = new Handler(Looper.getMainLooper()); // 蓝牙操作Handler
    @Nullable // VHAL物理按键信号读取器（车端方控信号）
    private VhalSignalReader vhalSignalReader;
    private volatile boolean vhalKeyPolling = false; // VHAL物理按键信号轮询是否运行中
    @Nullable // VHAL物理按键信号轮询线程
    private Thread vhalKeyPollThread;
    private int lastVhalKeySignal = VhalSignalReader.SIGNAL_NONE; // 上次VHAL按键信号（用于边沿触发去重）
    private boolean bluetoothReceiverRegistered = false; // 蓝牙广播接收器是否已注册
    private String activeAudioModule = "local"; // 当前活跃的音频模块（local/bluetooth）
    private String pendingBluetoothConnectAddress = ""; // 待连接的蓝牙设备地址
    private String confirmedBluetoothAudioAddress = ""; // 已确认连接的蓝牙音频设备地址
    private String requestedBluetoothControlAddress = ""; // 请求控制的蓝牙设备地址
    private long bluetoothRemoteProgressMs = -1L; // 蓝牙远程播放进度（毫秒）
    private long bluetoothRemoteDurationMs = -1L; // 蓝牙远程播放时长（毫秒）
    private long bluetoothRemoteProgressUpdatedAtMs = 0L; // 蓝牙远程进度更新时间戳
    private boolean bluetoothRemotePlayingKnown = false; // 是否知道蓝牙远程播放状态
    private boolean bluetoothRemotePlaying = false; // 蓝牙远程是否正在播放
    private String bluetoothRemoteTitle = ""; // 蓝牙远程曲目标题
    private String bluetoothRemoteArtist = ""; // 蓝牙远程曲目艺术家
    private String bluetoothRemoteAlbum = ""; // 蓝牙远程曲目专辑
    private String bluetoothRemoteCoverBase64 = ""; // 蓝牙远程专辑封面（Base64 JPEG，用于前端同步显示）
    private int statusBarTopPx = DEFAULT_STATUS_BAR_TOP_PX; // 状态栏顶部像素
    private int navBarBottomPx = 0; // 底部导航栏像素（用于避免按键被系统导航栏遮挡）
    private boolean statusBarLightBackground = false; // 状态栏是否浅色背景
    private boolean playbackControlReceiverRegistered = false; // 播放控制广播接收器是否已注册
    private boolean usbReceiverRegistered = false; // USB广播接收器是否已注册
    private volatile boolean usbScanning = false; // USB是否正在扫描
    private volatile String usbMusicStateJson; // USB音乐状态JSON（在onCreate中初始化）
    private int usbScanToken = 0; // USB扫描令牌（用于取消旧扫描）
    @Nullable
    private StorageManager storageManager; // 存储管理器，用于获取可移动存储设备
    private final Map<String, JSONObject> persistedUsbFavorites = new LinkedHashMap<>(); // 持久化的USB收藏列表
    private volatile boolean favoritesLoaded = false; // 收藏是否已加载
    private boolean localPlaybackPlaying = false; // 本地播放是否正在进行
    private String localPlaybackTitle; // 本地播放曲目标题（在onCreate中初始化）
    private String localPlaybackArtist; // 本地播放艺术家（在onCreate中初始化）
    private int pendingBluetoothConnectAttempts = 0; // 待处理蓝牙连接尝试次数
    private long pendingBluetoothConnectStartedAtMs = 0L; // 待处理蓝牙连接开始时间
    private long lastBluetoothConnectDurationMs = -1L; // 上次蓝牙连接耗时
    private long confirmedBluetoothConnectedAtMs = 0L; // 确认蓝牙连接时间
    private int bluetoothConnectSuccessCount = 0; // 蓝牙连接成功次数
    private int bluetoothConnectFailureCount = 0; // 蓝牙连接失败次数
    private int bluetoothDisconnectCount = 0; // 蓝牙断开次数
    private long bluetoothTotalConnectedDurationMs = 0L; // 蓝牙累计连接时长
    private String lastBluetoothConnectAddress = ""; // 上次蓝牙连接地址
    private String lastBluetoothConnectResult = "idle"; // 上次蓝牙连接结果
    private String bluetoothAutoReconnectAddress = ""; // 蓝牙自动重连地址
    private int bluetoothAutoReconnectAttempts = 0; // 蓝牙自动重连尝试次数
    private boolean userInitiatedBluetoothDisconnect = false; // 是否用户主动断开蓝牙
    private boolean pendingBluetoothDiscoveryAfterPermission = false; // 权限授予后是否待执行蓝牙搜索
    private boolean bluetoothDiscoveryPending = false; // 蓝牙搜索是否待处理
    private long bluetoothDiscoveryStartedAtMs = 0L; // 蓝牙搜索开始时间
    private boolean bluetoothScanningForAudioDevicesOnly = true; // 蓝牙搜索是否仅限音频设备
    private String bluetoothConnectionState = BT_STATE_IDLE; // 蓝牙连接状态机当前状态
    private String bluetoothLastError = ""; // 蓝牙最后错误信息
    private long bluetoothLastErrorAtMs = 0L; // 蓝牙最后错误时间
    private String bluetoothErrorRecoverySuggestion = ""; // 蓝牙错误恢复建议
    private final JSONArray bluetoothConnectSamples = new JSONArray(); // 蓝牙连接采样记录
    private final Runnable bluetoothConnectionStatusCheckRunnable = new Runnable() { // 蓝牙连接状态定时检查
        @Override
        public void run() {
            checkBluetoothConnectionStatus();
            bluetoothHandler.postDelayed(this, BLUETOOTH_CONNECTION_STATUS_CHECK_INTERVAL_MS);
        }
    };
    private final AudioManager.OnAudioFocusChangeListener bluetoothAudioFocusListener = focusChange -> { // 蓝牙音频焦点监听器
        if ("bluetooth".equals(activeAudioModule) && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
        }
    };

    @Nullable // 窗口管理器，用于添加蓝牙返回悬浮窗
    private WindowManager windowManager;
    @Nullable // 蓝牙设置返回按钮悬浮窗
    private View bluetoothBackOverlay;
    @Nullable // 收音机模块悬浮tab栏（覆盖在系统收音机Activity之上，保持tab栏可见可点击）
    private View radioTabOverlay;
    @Nullable // 电话管理器，用于监听来电状态
    private TelephonyManager telephonyManager;
    @Nullable // 电话状态监听器
    private PhoneStateListener phoneStateListener;
    private boolean phoneCallActive = false; // 是否有电话正在通话

    // USB广播接收器：监听USB设备连接/断开、媒体挂载/卸载等事件
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? "" : intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_CHECKING.equals(action)
                    || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)
                    || Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
                cancelUsbDisconnectedNotification();
                if (!usbScanning) {
                    publishUsbEvent("connected", getString(R.string.usb_msg_connected));
                    startUsbScanAsync();
                }
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_REMOVED.equals(action)
                    || Intent.ACTION_MEDIA_EJECT.equals(action)
                    || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)
                    || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                usbScanToken += 1;
                usbScanning = false;
                usbMusicStateJson = createUsbDisconnectedJson(getString(R.string.usb_msg_disconnected));
                publishUsbEvent("disconnected", getString(R.string.usb_msg_disconnected));
                showUsbDisconnectedNotification();
            }
        }
    };

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) {
                    return;
                }
                Uri[] uris = parseFileChooserResult(result.getResultCode(), result.getData());
                filePathCallback.onReceiveValue(uris);
                filePathCallback = null;
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Android file picker can still work without broad media permission via SAF.
            });

    private final ActivityResultLauncher<String> geolocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                completePendingGeolocationRequest(isGranted || hasWebGeolocationPermission());
            });

    private final ActivityResultLauncher<String[]> bluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Bluetooth panel can still show guidance if permission is denied.
                if (!hasBluetoothConnectPermission() || !hasBluetoothScanPermission() || !hasBluetoothAdvertisePermission()) {
                    publishBluetoothEvent("\u84dd\u7259\u6743\u9650\u672a\u5b8c\u5168\u6388\u6743\uff0cAndroid 14 \u9700\u8981\u626b\u63cf/\u8fde\u63a5/\u53ef\u53d1\u73b0\u6743\u9650");
                    pendingBluetoothDiscoveryAfterPermission = false;
                } else {
                    if (pendingBluetoothDiscoveryAfterPermission) {
                        pendingBluetoothDiscoveryAfterPermission = false;
                        startBluetoothDiscoveryFromPermissionGrant();
                    }
                    ensureA2dpProxy();
                    bluetoothHandler.postDelayed(this::checkSystemConnectedBluetoothDevices, 1000);
                }
            });

    private final BluetoothProfile.ServiceListener a2dpServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                bluetoothA2dp = (BluetoothA2dp) proxy;
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                schedulePendingBluetoothConnect(300);
                bluetoothHandler.postDelayed(() -> checkSystemConnectedBluetoothDevices(), 500);
            } else if (profile == PROFILE_A2DP_SINK) {
                bluetoothA2dpSink = proxy;
                publishBluetoothEvent("\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u6a21\u5f0f\u5df2\u5c31\u7eea");
                rememberConnectedA2dpSinkDevices();
                schedulePendingBluetoothConnect(300);
                bluetoothHandler.postDelayed(() -> checkSystemConnectedBluetoothDevices(), 500);
            } else if (profile == PROFILE_AVRCP_CONTROLLER) {
                bluetoothAvrcpController = proxy;
                publishBluetoothEvent("\u84dd\u7259\u5a92\u4f53\u63a7\u5236\u901a\u9053\u5df2\u5c31\u7eea");
                publishBluetoothPlaybackState();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                bluetoothA2dp = null;
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
            } else if (profile == PROFILE_A2DP_SINK) {
                bluetoothA2dpSink = null;
                bluetoothAvrcpController = null;
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
            }
        }
    };

    // 播放控制广播接收器：接收MusicPlaybackService的播放控制命令并分发给WebView
    private final BroadcastReceiver playbackControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !MusicPlaybackService.ACTION_CONTROL.equals(intent.getAction())) {
            }
            String command = intent.getStringExtra(MusicPlaybackService.EXTRA_COMMAND);
            dispatchLocalPlaybackCommand(command == null ? "" : command);
        }
    };

    // 蓝牙广播接收器：监听蓝牙设备发现、配对、连接状态变化、播放状态变化等事件
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothScanningForAudioDevicesOnly && !isAudioBluetoothDevice(device)) {
                    return;
                }
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                rememberBluetoothDevice(
                        device,
                        intent.getStringExtra(BluetoothDevice.EXTRA_NAME),
                        rssi == Short.MIN_VALUE ? null : (int) rssi
                );
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
            } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device, intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                publishBluetoothEvent("\u84dd\u7259\u8bbe\u5907\u540d\u79f0\u5df2\u66f4\u65b0");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (device != null && bondState == BluetoothDevice.BOND_BONDED
                        && device.getAddress().equals(pendingBluetoothConnectAddress)) {
                    publishBluetoothEvent("\u84dd\u7259\u914d\u5bf9\u5b8c\u6210\uff0c\u6b63\u5728\u81ea\u52a8\u8fde\u63a5\u97f3\u9891");
                    schedulePendingBluetoothConnect(500);
                } else if (bondState == BluetoothDevice.BOND_NONE
                        && device != null
                        && device.getAddress().equals(pendingBluetoothConnectAddress)) {
                    completeBluetoothConnectFailure(device, "\u84dd\u7259\u914d\u5bf9\u5931\u8d25\u6216\u5df2\u53d6\u6d88");
                    clearPendingBluetoothConnect();
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                } else {
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device);
                ensureA2dpProxy();
                bluetoothHandler.postDelayed(() -> checkSystemConnectedBluetoothDevices(), 1500);
                publishBluetoothEvent("\u84dd\u7259\u8bbe\u5907\u5df2\u8fde\u63a5");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device);
                if (device != null && device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                    recordBluetoothDisconnect(device);
                    confirmedBluetoothAudioAddress = "";
                    markBluetoothPlaybackDisconnected();
                    publishBluetoothEvent("\u5df2\u65ad\u5f00\u8fde\u63a5");
                } else {
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                }
            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (shouldUseBluetoothReceiverMode()) {
                    if (device != null && device.getAddress().equals(pendingBluetoothConnectAddress)) {
                        schedulePendingBluetoothConnect(500);
                    }
                    publishBluetoothEvent("\u666e\u901a A2DP \u8f93\u51fa\u5df2\u8fde\u63a5\uff0c\u84dd\u7259\u97f3\u4e50\u63a5\u6536\u9700\u8981 A2DP Sink");
                    } else {
                    autoReturnToBluetoothOnConnected();
                    if (device != null && device.getAddress().equals(pendingBluetoothConnectAddress)) {
                        confirmedBluetoothAudioAddress = device.getAddress();
                        rememberBluetoothControlTarget(device);
                        prepareBluetoothMusicRoute();
                        String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                        clearPendingBluetoothConnect();
                        publishBluetoothEvent(message);
                        return;
                    }
                    if (device != null) {
                        confirmedBluetoothAudioAddress = device.getAddress();
                        rememberBluetoothControlTarget(device);
                    }
                    prepareBluetoothMusicRoute();
                    publishBluetoothEvent("\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                    publishBluetoothPlaybackState();
                    }
                } else if (state == BluetoothProfile.STATE_CONNECTING) {   
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    if (device != null && device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                        recordBluetoothDisconnect(device);
                        confirmedBluetoothAudioAddress = "";
                        clearBluetoothControlTarget(device);
                        markBluetoothPlaybackDisconnected();
                        publishBluetoothEvent("\u5df2\u65ad\u5f00\u8fde\u63a5");
                    } else {
                        publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                    }
                }
            } else if (ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rememberBluetoothDevice(device);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (device != null) {
                        confirmedBluetoothAudioAddress = device.getAddress();
                        rememberBluetoothControlTarget(device);
                    }
                    String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u8fde\u63a5");
                    clearPendingBluetoothConnect();
                    prepareBluetoothSpeakerRoute();
                    publishBluetoothEvent(message);
                    publishBluetoothPlaybackState();
                    autoReturnToBluetoothOnConnected();
                } else if (state == BluetoothProfile.STATE_CONNECTING) {
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    if (device != null && device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                        recordBluetoothDisconnect(device);
                        confirmedBluetoothAudioAddress = "";
                        clearBluetoothControlTarget(device);
                        markBluetoothPlaybackDisconnected();
                        publishBluetoothEvent("\u5df2\u65ad\u5f00\u8fde\u63a5");
                    } else {
                        publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                    publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                }
            } else if (ACTION_A2DP_SINK_PLAYING_STATE_CHANGED.equals(action)) {
                updateBluetoothSinkPlayingState(intent);
                publishBluetoothPlaybackState();
                publishBluetoothEvent("\u84dd\u7259\u64ad\u653e\u72b6\u6001\u5df2\u66f4\u65b0");
            } else if (ACTION_AVRCP_CONTROLLER_PLAYBACK_STATE_CHANGED.equals(action)
                    || ACTION_AVRCP_CONTROLLER_TRACK_EVENT.equals(action)
                    || ACTION_AVRCP_PLAYBACK_STATE_CHANGED.equals(action)
                    || ACTION_AVRCP_TRACK_EVENT.equals(action)) {
                try {
                    updateBluetoothRemotePlaybackExtras(intent);
                } catch (RuntimeException ignored) {
                    bluetoothRemoteProgressMs = -1L;
                    bluetoothRemoteDurationMs = -1L;
                    bluetoothRemoteProgressUpdatedAtMs = 0L;
                }
                publishBluetoothPlaybackState();
            } else if (ACTION_VOLUME_CHANGED.equals(action)) {
                publishBluetoothPlaybackState();
                publishSystemVolumeState();
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (btState == BluetoothAdapter.STATE_ON) {
                    publishBluetoothEvent("\u84dd\u7259\u5df2\u5f00\u542f");
                    bluetoothHandler.postDelayed(() -> {
                        ensureA2dpProxy();
                        checkSystemConnectedBluetoothDevices();
                        publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
                    }, 500);
                } else if (btState == BluetoothAdapter.STATE_OFF) {
                    if (confirmedBluetoothAudioAddress.length() > 0) {
                        BluetoothDevice device = getRemoteDevice(confirmedBluetoothAudioAddress);
                        if (device != null) {
                            recordBluetoothDisconnect(device);
                        }
                        confirmedBluetoothAudioAddress = "";
                        markBluetoothPlaybackDisconnected();
                    }
                    stopBluetoothConnectionStatusChecker();
                    clearAllBluetoothAutoReconnect();
                    publishBluetoothEvent("\u84dd\u7259\u5df2\u5173\u95ed");
                } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
                    publishBluetoothEvent("\u84dd\u7259\u6b63\u5728\u5f00\u542f...");
                } else if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                    publishBluetoothEvent("\u84dd\u7259\u6b63\u5728\u5173\u95ed...");
                }
                publishBluetoothPlaybackState();
            }
        }
    };

    /**
     * Activity创建时调用，执行初始化操作：
     * 配置EdgeToEdge沉浸式、设置WebView、蓝牙、USB、电话监听等组件
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // 确保根容器和WebView无padding，全屏填充
        View rootView = findViewById(R.id.main);
        rootView.setPadding(0, 0, 0, 0);
        musicWebView = findViewById(R.id.musicWebView);
        musicWebView.setPadding(0, 0, 0, 0);
        musicWebView.setClipToPadding(false);
        // 初始化本地化播放信息默认值
        localPlaybackTitle = getString(R.string.default_title);
        localPlaybackArtist = getString(R.string.default_artist);
        // 初始化存储管理器，用于后续USB设备检测
        storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        // 初始化USB状态JSON
        usbMusicStateJson = createUsbDisconnectedJson(getString(R.string.usb_msg_not_connected));
        applyStatusBarTheme(false);
        // 监听WindowInsets：
        // 1) 每次insets分发后重置根容器/WebView padding为0，覆盖Material3自动设置的系统栏padding
        // 2) 提取状态栏高度注入CSS变量
        // 3) 返回原始insets，避免返回null导致AndroidX内部NPE
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            statusBarTopPx = systemBars.top;
            // 取系统栏与导航栏底部的较大值，覆盖手势导航与三键导航两种场景
            navBarBottomPx = Math.max(systemBars.bottom, navBars.bottom);
            v.setPadding(0, 0, 0, 0);
            if (musicWebView != null) {
                musicWebView.setPadding(0, 0, 0, 0);
            }
            injectSafeAreaCssVariables();
            return insets;
        });
        configureWebView();
        configureBackNavigation();
        configureBluetooth();
        configurePhoneStateListener();
        registerUsbReceiver();
        registerPlaybackControlReceiver();
        requestAudioPermissionIfNeeded();
        requestPhoneStatePermissionIfNeeded();
        startVhalKeySignalPolling();
        musicWebView.loadUrl(PLAYER_URL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // 处理来自悬浮tab栏的模块切换请求
        String targetModule = intent.getStringExtra("extra_switch_module");
        if (targetModule != null && !targetModule.isEmpty()) {
            // 安全校验：仅允许已知的模块名称，避免脚本注入
            if (!"bluetooth".equals(targetModule)
                    && !"radio".equals(targetModule)
                    && !"usb".equals(targetModule)) {
                return;
            }
            // 隐藏悬浮窗（悬浮tab栏和蓝牙返回按钮）
            hideRadioTabOverlay();
            hideBluetoothBackOverlay();
            // 通知WebView切换到目标模块
            final String module = targetModule;
            evaluatePlayerScript("if(typeof switchModule==='function'){switchModule('" + module + "');}");
            Log.d(TAG, "收到模块切换请求，切换到: " + module);
        }
    }

    @Override
    protected void onPause() {
        evaluatePlayerScript("window.onNativeAppPause&&window.onNativeAppPause();");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBluetoothBackOverlay();
        // 清理可能残留的悬浮tab栏（应用从后台恢复时）
        hideRadioTabOverlay();
        checkSystemConnectedBluetoothDevices();
        if (hasUsbStorageReadPermission() && !findUsbRoots().isEmpty() && !isUsbStateConnected()) {
            startUsbScanAsync();
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            boolean hasConnectedDevice = !getConnectedProfileDevices(bluetoothA2dp).isEmpty()
                    || !getConnectedProfileDevices(bluetoothA2dpSink).isEmpty();
            if (!hasConnectedDevice && confirmedBluetoothAudioAddress.length() == 0) {
                bluetoothHandler.postDelayed(() -> {
                    if (!hasBluetoothConnectPermission() || !hasBluetoothScanPermission()) {
                        return;
                    }
                    checkSystemConnectedBluetoothDevices();
                    if (confirmedBluetoothAudioAddress.length() == 0
                            && getConnectedProfileDevices(bluetoothA2dp).isEmpty()
                            && getConnectedProfileDevices(bluetoothA2dpSink).isEmpty()) {
                        startBluetoothDiscoveryInternal();
                    }
                }, 1500);
            }
        }
        evaluatePlayerScript("window.onNativeAppResume&&window.onNativeAppResume();");
    }

    /**
     * 配置WebView设置：
     * 启用JS、DOM存储、文件访问、媒体播放等设置，注入JS桥接接口和WebChromeClient
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = musicWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setGeolocationEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        musicWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSafeAreaCssVariables();
                applyStatusBarTheme(statusBarLightBackground);
                if (!findUsbRoots().isEmpty()) {
                    startUsbScanAsync();
                }
            }
        });
        musicWebView.addJavascriptInterface(new MusicBridge(), "MusicBridge");
        musicWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "audio/*",
                        "text/*",
                        "application/octet-stream"
                });
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                try {
                    fileChooserLauncher.launch(intent);
                } catch (Exception exception) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback
            ) {
                requestWebGeolocationPermission(origin, callback);
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                completePendingGeolocationRequest(false);
            }
        });
    }

    private void requestWebGeolocationPermission(
            String origin,
            GeolocationPermissions.Callback callback
    ) {
        completePendingGeolocationRequest(false);
        pendingGeolocationOrigin = origin;
        pendingGeolocationCallback = callback;
        if (hasWebGeolocationPermission()) {
            completePendingGeolocationRequest(true);
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            completePendingGeolocationRequest(true);
            return;
        }
        geolocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean hasWebGeolocationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void completePendingGeolocationRequest(boolean allow) {
        if (pendingGeolocationCallback == null || pendingGeolocationOrigin == null) {
            return;
        }
        pendingGeolocationCallback.invoke(pendingGeolocationOrigin, allow, false);
        pendingGeolocationCallback = null;
        pendingGeolocationOrigin = null;
    }

    private void requestAudioPermissionIfNeeded() {
        // 1. 存储访问权限：Android 11+ 直接读取U盘需要"所有文件访问"，Android 6-10 需要 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccess();
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            return;
        }
        // 2. Android 13+：音频媒体读取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            return;
        }
        // 3. Android 13+：通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        // 4. 蓝牙/位置权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothRuntimePermissionsIfNeeded(false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /** Android 11+：跳转"所有文件访问"权限设置页，用于直接读取U盘挂载点。 */
    private void requestAllFilesAccess() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "跳转所有文件访问权限设置失败", e);
        }
    }

    /** 判断当前是否具备直接读取U盘(可移动存储)文件的权限。 */
    private boolean hasUsbStorageReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean requestBluetoothRuntimePermissionsIfNeeded(boolean retryDiscoveryAfterGrant) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        ArrayList<String> bluetoothPermissions = new ArrayList<>();
        if (!hasBluetoothConnectPermission()) {
            bluetoothPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!hasBluetoothScanPermission()) {
            bluetoothPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!hasBluetoothAdvertisePermission()) {
            bluetoothPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (bluetoothPermissions.isEmpty()) {
            return false;
        }
        pendingBluetoothDiscoveryAfterPermission = retryDiscoveryAfterGrant;
        runOnUiThread(() -> {
            try {
                bluetoothPermissionLauncher.launch(bluetoothPermissions.toArray(new String[0]));
            } catch (IllegalStateException exception) {
                pendingBluetoothDiscoveryAfterPermission = false;
                publishBluetoothEvent("\u84dd\u7259\u6743\u9650\u8bf7\u6c42\u6682\u65f6\u65e0\u6cd5\u53d1\u8d77\uff0c\u8bf7\u91cd\u8bd5\u6216\u5728\u7cfb\u7edf\u6743\u9650\u4e2d\u786e\u8ba4");
            }
        });
        return true;
    }

    private void startBluetoothDiscoveryFromPermissionGrant() {
        bluetoothHandler.post(() -> {
            String result = startBluetoothDiscoveryInternal();
            try {
                JSONObject json = new JSONObject(result);
                publishBluetoothEvent(json.optString("message", "\u6b63\u5728\u626b\u63cf\u9644\u8fd1\u84dd\u7259\u8bbe\u5907..."));
            } catch (Exception ignored) {
                publishBluetoothEvent("\u6b63\u5728\u626b\u63cf\u9644\u8fd1\u84dd\u7259\u8bbe\u5907...");
            }
        });
    }

    /**
     * 开始蓝牙设备搜索（内部方法）：
     * 检查蓝牙状态和权限，启动设备发现流程，设置搜索超时自动停止
     */
    private String startBluetoothDiscoveryInternal() {
        if (bluetoothAdapter == null) {
            return statusJson(false, "\u5f53\u524d\u8bbe\u5907\u4e0d\u652f\u6301\u84dd\u7259");
        }
        if (!bluetoothAdapter.isEnabled()) {
            return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f\uff0c\u8bf7\u5148\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5f00\u542f\u84dd\u7259");
        }
        if (!hasBluetoothScanPermission() || !hasBluetoothConnectPermission()) {
            if (requestBluetoothRuntimePermissionsIfNeeded(true)) {
                return statusJson(false, "\u6b63\u5728\u8bf7\u6c42\u84dd\u7259\u626b\u63cf/\u8fde\u63a5\u6743\u9650\uff0c\u6388\u6743\u540e\u5c06\u81ea\u52a8\u91cd\u8bd5\u626b\u63cf");
            }
            return statusJson(false, "\u7f3a\u5c11\u84dd\u7259\u626b\u63cf/\u8fde\u63a5\u6743\u9650\uff0c\u8bf7\u6388\u6743\u540e\u91cd\u8bd5");
        }
        try {
            ensureA2dpProxy();
            cleanupExpiredBluetoothDevices();
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (isAudioBluetoothDevice(device)) {
                    rememberBluetoothDevice(device);
                }
            }
            if (!getConnectedProfileDevices(bluetoothA2dp).isEmpty() || !getConnectedProfileDevices(bluetoothA2dpSink).isEmpty()) {
                checkSystemConnectedBluetoothDevices();
                return statusJson(true, "\u84dd\u7259\u8bbe\u5907\u5df2\u8fde\u63a5\uff0c\u65e0\u9700\u626b\u63cf");
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                bluetoothHandler.postDelayed(this::startBluetoothDiscoveryInternal, 500);
                return statusJson(true, "\u6b63\u5728\u91cd\u65b0\u5f00\u59cb\u626b\u63cf...");
            }
            bluetoothDiscoveryStartedAtMs = System.currentTimeMillis();
            boolean started = bluetoothAdapter.startDiscovery();
            if (started) {
                scheduleBluetoothDiscoveryTimeout();
                return statusJson(true, "\u6b63\u5728\u641c\u7d22\u9644\u8fd1\u84dd\u7259\u97f3\u9891\u8bbe\u5907...");
            } else {
                bluetoothDiscoveryPending = true;
                bluetoothHandler.postDelayed(() -> {
                    if (bluetoothDiscoveryPending) {
                        startBluetoothDiscoveryInternal();
                    }
                }, 2000);
                return statusJson(true, "\u84dd\u7259\u626b\u63cf\u88ab\u5360\u7528\uff0c\u6b63\u5728\u7b49\u5f85...");
            }
        } catch (SecurityException exception) {
            requestBluetoothRuntimePermissionsIfNeeded(true);
            return statusJson(false, "\u84dd\u7259\u641c\u7d22\u6743\u9650\u88ab\u7cfb\u7edf\u62d2\u7edd\uff0c\u5df2\u91cd\u65b0\u53d1\u8d77\u6743\u9650\u68c0\u67e5");
        }
    }

    private boolean isAudioBluetoothDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            return true;
        }
        int majorClass = bluetoothClass.getMajorDeviceClass();
        return majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                || majorClass == BluetoothClass.Device.Major.PHONE
                || majorClass == BluetoothClass.Device.Major.COMPUTER
                || majorClass == BluetoothClass.Device.Major.WEARABLE
                || majorClass == BluetoothClass.Device.Major.PERIPHERAL;
    }

    private void cleanupExpiredBluetoothDevices() {
        long now = System.currentTimeMillis();
        List<String> expiredAddresses = new ArrayList<>();
        for (Map.Entry<String, Long> entry : bluetoothDeviceLastSeen.entrySet()) {
            if (now - entry.getValue() > BLUETOOTH_DEVICE_EXPIRY_MS) {
                expiredAddresses.add(entry.getKey());
            }
        }
        for (String address : expiredAddresses) {
            knownBluetoothDevices.remove(address);
            bluetoothDeviceNames.remove(address);
            bluetoothDeviceRssi.remove(address);
            bluetoothDeviceLastSeen.remove(address);
        }
    }

    private void scheduleBluetoothDiscoveryTimeout() {
        bluetoothHandler.removeCallbacksAndMessages("bluetoothDiscoveryTimeout");
        bluetoothHandler.postDelayed(() -> {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }, BLUETOOTH_DISCOVERY_MAX_DURATION_MS);
    }

    private void registerPlaybackControlReceiver() {
        IntentFilter filter = new IntentFilter(MusicPlaybackService.ACTION_CONTROL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackControlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playbackControlReceiver, filter);
        }
        playbackControlReceiverRegistered = true;
    }

    private void registerUsbReceiver() {
        IntentFilter mediaFilter = new IntentFilter();
        mediaFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        mediaFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mediaFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        mediaFilter.addDataScheme("file");

        IntentFilter usbDeviceFilter = new IntentFilter();
        usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, mediaFilter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(usbReceiver, usbDeviceFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, mediaFilter);
            registerReceiver(usbReceiver, usbDeviceFilter);
        }
        usbReceiverRegistered = true;
    }

    private Uri[] parseFileChooserResult(int resultCode, @Nullable Intent data) {
        Uri[] parsed = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        if (parsed != null && parsed.length > 0) {
            return parsed;
        }
        if (resultCode != RESULT_OK || data == null) {
            return null;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i += 1) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && !uris.contains(uri)) {
                    uris.add(uri);
                }
            }
        }

        Uri singleUri = data.getData();
        if (singleUri != null && !uris.contains(singleUri)) {
            uris.add(singleUri);
        }

        return uris.isEmpty() ? null : uris.toArray(new Uri[0]);
    }

    /**
     * 配置蓝牙广播接收器和Profile代理：
     * 注册蓝牙状态变化、设备发现、配对、连接等广播监听器
     */
    private void configureBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ensureA2dpProxy();
        bluetoothHandler.postDelayed(this::checkSystemConnectedBluetoothDevices, 2000);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_A2DP_SINK_PLAYING_STATE_CHANGED);
        filter.addAction(ACTION_AVRCP_CONTROLLER_PLAYBACK_STATE_CHANGED);
        filter.addAction(ACTION_AVRCP_CONTROLLER_TRACK_EVENT);
        filter.addAction(ACTION_AVRCP_PLAYBACK_STATE_CHANGED);
        filter.addAction(ACTION_AVRCP_TRACK_EVENT);
        filter.addAction(ACTION_VOLUME_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(bluetoothReceiver, filter);
        }
        bluetoothReceiverRegistered = true;
    }

    /**
     * 获取A2DP、A2DP Sink、AVRCP Controller Profile代理：
     * 通过反射方式获取蓝牙Profile代理，用于后续蓝牙音频输出/接收/控制
     */
    private void ensureA2dpProxy() {
        if (bluetoothAdapter == null || !hasBluetoothConnectPermission()) {
            return;
        }
        try {
            if (bluetoothA2dp == null) {
                bluetoothAdapter.getProfileProxy(this, a2dpServiceListener, BluetoothProfile.A2DP);
            }
            if (bluetoothA2dpSink == null) {
                bluetoothAdapter.getProfileProxy(this, a2dpServiceListener, PROFILE_A2DP_SINK);
            }
            if (bluetoothAvrcpController == null) {
                bluetoothAdapter.getProfileProxy(this, a2dpServiceListener, PROFILE_AVRCP_CONTROLLER);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean shouldUseBluetoothReceiverMode() {
        BluetoothDevice pendingDevice = getRemoteDevice(pendingBluetoothConnectAddress);
        if (shouldUseBluetoothReceiverMode(pendingDevice)) {
            return true;
        }
        for (BluetoothDevice device : getConnectedProfileDevices(bluetoothA2dpSink)) {
            if (shouldUseBluetoothReceiverMode(device)) {
                return true;
            }
        }
        return "bluetooth".equals(activeAudioModule) && bluetoothA2dpSink != null;
    }

    private boolean shouldUseBluetoothReceiverMode(BluetoothDevice device) {
        BluetoothClass bluetoothClass = device == null ? null : device.getBluetoothClass();
        int majorClass = bluetoothClass == null ? -1 : bluetoothClass.getMajorDeviceClass();
        return majorClass == BluetoothClass.Device.Major.PHONE
                || majorClass == BluetoothClass.Device.Major.COMPUTER;
    }

    private boolean hasBluetoothConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothAdvertisePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void rememberBluetoothDevice(@Nullable BluetoothDevice device) {
        rememberBluetoothDevice(device, null);
    }

    private void rememberBluetoothDevice(@Nullable BluetoothDevice device, @Nullable String discoveredName) {
        rememberBluetoothDevice(device, discoveredName, null);
    }

    private void rememberBluetoothDevice(
            @Nullable BluetoothDevice device,
            @Nullable String discoveredName,
            @Nullable Integer rssiDbm
    ) {
        if (device == null || !hasBluetoothConnectPermission()) {
            return;
        }
        String address = device.getAddress();
        knownBluetoothDevices.put(address, device);
        String bestName = getBestBluetoothDeviceName(device, discoveredName);
        if (bestName.length() > 0) {
            bluetoothDeviceNames.put(address, bestName);
        }
        if (rssiDbm != null) {
            bluetoothDeviceRssi.put(address, rssiDbm);
        }
        bluetoothDeviceLastSeen.put(address, System.currentTimeMillis());
    }

    private JSONObject createBluetoothDeviceJson(BluetoothDevice device) throws Exception {
        String address = device.getAddress();
        String displayName = getBestBluetoothDeviceName(device, bluetoothDeviceNames.get(address));
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        int typeCode = bluetoothClass == null ? -1 : bluetoothClass.getMajorDeviceClass();
        boolean sinkConnected = isA2dpSinkConnected(device);
        boolean sourceConnected = isA2dpConnected(device);
        boolean connected = sourceConnected || sinkConnected || address.equals(confirmedBluetoothAudioAddress);
        Integer rssiDbm = bluetoothDeviceRssi.get(address);
        JSONObject item = new JSONObject();
        item.put("name", displayName.length() == 0 ? "Unknown Bluetooth Device" : displayName);
        item.put("address", address);
        item.put("paired", device.getBondState() == BluetoothDevice.BOND_BONDED);
        item.put("connected", connected);
        item.put("audioRole", sinkConnected ? "sink" : sourceConnected ? "source" : "none");
        item.put("type", bluetoothMajorClassToLabel(typeCode));
        item.put("typeLabel", bluetoothMajorClassToLabel(typeCode));
        item.put("typeCode", typeCode);
        item.put("rssi", rssiDbm == null ? JSONObject.NULL : rssiDbm);
        item.put("signalLevel", rssiDbm == null ? (connected ? 4 : -1) : bluetoothSignalLevel(rssiDbm));
        item.put("lastSeen", bluetoothDeviceLastSeen.containsKey(address) ? bluetoothDeviceLastSeen.get(address) : 0L);
        return item;
    }

    private String bluetoothMajorClassToLabel(int majorClass) {
        switch (majorClass) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "Audio";
            case BluetoothClass.Device.Major.PHONE:
                return "Phone";
            case BluetoothClass.Device.Major.COMPUTER:
                return "Computer";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "Peripheral";
            case BluetoothClass.Device.Major.WEARABLE:
                return "Wearable";
            case BluetoothClass.Device.Major.NETWORKING:
                return "Network";
            case BluetoothClass.Device.Major.IMAGING:
                return "Imaging";
            default:
                return "Unknown";
        }
    }

    private int bluetoothSignalLevel(int rssiDbm) {
        if (rssiDbm >= -55) {
            return 4;
        }
        if (rssiDbm >= -67) {
            return 3;
        }
        if (rssiDbm >= -80) {
            return 2;
        }
        return 1;
    }
    private String getBestBluetoothDeviceName(BluetoothDevice device, @Nullable String discoveredName) {
        String name = normalizeBluetoothDeviceName(discoveredName);
        if (name.length() > 0) {
            return name;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            name = normalizeBluetoothDeviceName(device.getAlias());
            if (name.length() > 0) {
                return name;
            }
        }
        return normalizeBluetoothDeviceName(device.getName());
    }

    private String normalizeBluetoothDeviceName(@Nullable String name) {
        if (name == null) {
            return "";
        }
        return name.trim();
    }

    private boolean isA2dpConnected(BluetoothDevice device) {
        return bluetoothA2dp != null
                && hasBluetoothConnectPermission()
                && bluetoothA2dp.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED;
    }

    private boolean isA2dpSinkConnected(BluetoothDevice device) {
        return getProfileConnectionState(bluetoothA2dpSink, device) == BluetoothProfile.STATE_CONNECTED;
    }

    private void rememberConnectedA2dpDevices() {
        if (bluetoothA2dp == null || !hasBluetoothConnectPermission()) {
            return;
        }
        try {
            for (BluetoothDevice device : bluetoothA2dp.getConnectedDevices()) {
                rememberBluetoothDevice(device);
            }
        } catch (SecurityException ignored) {
        }
    }

    private void rememberConnectedA2dpSinkDevices() {
        for (BluetoothDevice device : getConnectedProfileDevices(bluetoothA2dpSink)) {
            rememberBluetoothDevice(device);
            confirmedBluetoothAudioAddress = device.getAddress();
        }
    }

    private void refreshSystemBluetoothConnectionState() {
        if (bluetoothA2dp == null && bluetoothA2dpSink == null) {
            return;
        }
        List<BluetoothDevice> a2dpDevices = getConnectedProfileDevices(bluetoothA2dp);
        List<BluetoothDevice> sinkDevices = getConnectedProfileDevices(bluetoothA2dpSink);
        if (!a2dpDevices.isEmpty()) {
            BluetoothDevice device = a2dpDevices.get(0);
            if (!device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothDevice(device);
                rememberBluetoothControlTarget(device);
            }
        } else if (!sinkDevices.isEmpty()) {
            BluetoothDevice device = sinkDevices.get(0);
            if (!device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothDevice(device);
                rememberBluetoothControlTarget(device);
            }
        } else if (confirmedBluetoothAudioAddress.length() > 0) {
            BluetoothDevice device = getRemoteDevice(confirmedBluetoothAudioAddress);
            if (device != null) {
                if (!isA2dpConnected(device) && !isA2dpSinkConnected(device)) {
                    confirmedBluetoothAudioAddress = "";
                }
            } else {
                confirmedBluetoothAudioAddress = "";
            }
        }
    }

    private BluetoothDevice getRemoteDevice(String address) {
        if (bluetoothAdapter == null || address == null || address.length() == 0) {
            return null;
        }
        try {
            return bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String statusJson(boolean ok, String message) {
        JSONObject result = new JSONObject();
        try {
            result.put("ok", ok);
            result.put("message", message);
        } catch (Exception ignored) {
            return "{\"ok\":false,\"message\":\"状态生成失败\"}";
        }
        return result.toString();
    }

    private void beginBluetoothConnectMetrics(BluetoothDevice device) {
        long now = System.currentTimeMillis();
        pendingBluetoothConnectStartedAtMs = now;
        lastBluetoothConnectDurationMs = -1L;
        lastBluetoothConnectAddress = device == null ? "" : device.getAddress();
        lastBluetoothConnectResult = "connecting";
        setBluetoothConnectionState(BT_STATE_CONNECTING);
    }

    private String completeBluetoothConnectSuccess(BluetoothDevice device, String message) {
        long now = System.currentTimeMillis();
        long elapsed = pendingBluetoothConnectStartedAtMs > 0L ? now - pendingBluetoothConnectStartedAtMs : 0L;
        lastBluetoothConnectDurationMs = elapsed;
        lastBluetoothConnectAddress = device == null ? lastBluetoothConnectAddress : device.getAddress();
        lastBluetoothConnectResult = elapsed <= BLUETOOTH_FAST_CONNECT_TIMEOUT_MS ? "success" : "success_slow";
        confirmedBluetoothConnectedAtMs = now;
        bluetoothConnectSuccessCount += 1;
        clearBluetoothAutoReconnect();
        clearBluetoothError();
        setBluetoothConnectionState(BT_STATE_CONNECTED);
        addBluetoothConnectSample(lastBluetoothConnectResult, lastBluetoothConnectAddress, elapsed, message);
        startBluetoothConnectionStatusChecker();
        return elapsed > 0L ? message + "\uff08\u8017\u65f6 " + elapsed + "ms\uff09" : message;
    }

    private void completeBluetoothConnectFailure(@Nullable BluetoothDevice device, String reason) {
        long now = System.currentTimeMillis();
        long elapsed = pendingBluetoothConnectStartedAtMs > 0L ? now - pendingBluetoothConnectStartedAtMs : 0L;
        lastBluetoothConnectDurationMs = elapsed;
        lastBluetoothConnectAddress = device == null ? lastBluetoothConnectAddress : device.getAddress();
        lastBluetoothConnectResult = "failed";
        bluetoothConnectFailureCount += 1;
        setBluetoothConnectionState(BT_STATE_ERROR);
        String suggestion = buildBluetoothRecoverySuggestion(reason);
        recordBluetoothError(reason, suggestion);
        addBluetoothConnectSample("failed", lastBluetoothConnectAddress, elapsed, reason);
        if (device != null
                && !userInitiatedBluetoothDisconnect
                && device.getAddress().equals(bluetoothAutoReconnectAddress)
                && bluetoothAutoReconnectAttempts < BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS) {
            scheduleBluetoothAutoReconnect(device, reason);
        }
    }

    private String buildBluetoothRecoverySuggestion(String reason) {
        if (reason == null || reason.isEmpty()) {
            return getString(R.string.bt_err_retry_hint);
        }
        if (reason.contains("权限") || reason.contains("系统限制")) {
            return getString(R.string.bt_err_grant_permission);
        }
        if (reason.contains("超时") || reason.contains("timeout")) {
            return getString(R.string.bt_err_confirm_bluetooth);
        }
        if (reason.contains("配对") || reason.contains("bond")) {
            return getString(R.string.bt_err_complete_pairing);
        }
        if (reason.contains("未开启") || reason.contains("未连接")) {
            return getString(R.string.bt_err_enable_first);
        }
        if (reason.contains("已断开") || reason.contains("disconnect")) {
            return getString(R.string.bt_err_reconnect_hint);
        }
        if (reason.contains("设备引用") || reason.contains("失效")) {
            return getString(R.string.bt_err_restart_app);
        }
        return getString(R.string.bt_err_retry_hint);
    }

    private void recordBluetoothDisconnect(@Nullable BluetoothDevice device) {
        long now = System.currentTimeMillis();
        if (confirmedBluetoothConnectedAtMs > 0L) {
            bluetoothTotalConnectedDurationMs += Math.max(0L, now - confirmedBluetoothConnectedAtMs);
        }
        confirmedBluetoothConnectedAtMs = 0L;
        bluetoothDisconnectCount += 1;
        String address = device == null ? confirmedBluetoothAudioAddress : device.getAddress();
        lastBluetoothConnectResult = "disconnected";
        setBluetoothConnectionState(BT_STATE_DISCONNECTED);
        stopBluetoothConnectionStatusChecker();
        if (device != null) {
            scheduleBluetoothAutoReconnect(device, "音频链路断开");
        }
        addBluetoothConnectSample("disconnected", address, 0L, "蓝牙音频设备已断开");
    }

    private void addBluetoothConnectSample(String result, String address, long elapsedMs, String message) {
        try {
            JSONObject sample = new JSONObject();
            sample.put("timestamp", System.currentTimeMillis());
            sample.put("result", result);
            sample.put("address", address == null ? "" : address);
            sample.put("elapsedMs", elapsedMs);
            sample.put("fastPass", elapsedMs > 0L && elapsedMs <= BLUETOOTH_FAST_CONNECT_TIMEOUT_MS);
            sample.put("message", message == null ? "" : message);
            bluetoothConnectSamples.put(sample);
            while (bluetoothConnectSamples.length() > BLUETOOTH_CONNECT_SAMPLE_LIMIT) {
                bluetoothConnectSamples.remove(0);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean hasPendingBluetoothConnectTimedOut() {
        return pendingBluetoothConnectStartedAtMs > 0L
                && System.currentTimeMillis() - pendingBluetoothConnectStartedAtMs >= BLUETOOTH_CONNECT_CONFIRM_TIMEOUT_MS;
    }

    private String bluetoothConnectionMetricsJson() {
        JSONObject result = new JSONObject();
        try {
            long currentSessionMs = confirmedBluetoothConnectedAtMs > 0L
                    ? Math.max(0L, System.currentTimeMillis() - confirmedBluetoothConnectedAtMs)
                    : 0L;
            result.put("targetFastConnectMs", BLUETOOTH_FAST_CONNECT_TIMEOUT_MS);
            result.put("connectConfirmTimeoutMs", BLUETOOTH_CONNECT_CONFIRM_TIMEOUT_MS);
            result.put("targetSuccessRate", 0.95);
            result.put("targetStableSessionMs", BLUETOOTH_STABLE_SESSION_TARGET_MS);
            result.put("lastAddress", lastBluetoothConnectAddress);
            result.put("lastResult", lastBluetoothConnectResult);
            result.put("lastElapsedMs", lastBluetoothConnectDurationMs);
            result.put("successCount", bluetoothConnectSuccessCount);
            result.put("failureCount", bluetoothConnectFailureCount);
            result.put("disconnectCount", bluetoothDisconnectCount);
            result.put("currentSessionMs", currentSessionMs);
            result.put("totalConnectedDurationMs", bluetoothTotalConnectedDurationMs + currentSessionMs);
            result.put("samples", bluetoothConnectSamples);
            return result.toString();
        } catch (Exception ignored) {
            return "{\"targetFastConnectMs\":3000,\"targetSuccessRate\":0.95,\"samples\":[]}";
        }
    }

    private String bluetoothStateJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("available", bluetoothAdapter != null);
            result.put("enabled", bluetoothAdapter != null && bluetoothAdapter.isEnabled());
            result.put("discovering", bluetoothAdapter != null && hasBluetoothScanPermission() && bluetoothAdapter.isDiscovering());
            result.put("connectPermission", hasBluetoothConnectPermission());
            result.put("scanPermission", hasBluetoothScanPermission());
            result.put("advertisePermission", hasBluetoothAdvertisePermission());
            result.put("a2dpReady", bluetoothA2dp != null);
            result.put("a2dpSinkReady", bluetoothA2dpSink != null);
            result.put("a2dpSinkConnectedCount", getConnectedProfileDevices(bluetoothA2dpSink).size());
            result.put("connectionState", bluetoothConnectionState);
            result.put("lastError", bluetoothLastError);
            result.put("lastErrorAtMs", bluetoothLastErrorAtMs);
            result.put("errorRecoverySuggestion", bluetoothErrorRecoverySuggestion);
            result.put("autoReconnectAddress", bluetoothAutoReconnectAddress);
            result.put("autoReconnectAttempts", bluetoothAutoReconnectAttempts);
            result.put("autoReconnectMaxAttempts", BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS);
            result.put("pendingConnectAddress", pendingBluetoothConnectAddress);
            result.put("pendingConnectAttempts", pendingBluetoothConnectAttempts);
            result.put("confirmedAudioAddress", confirmedBluetoothAudioAddress);
        } catch (Exception ignored) {
            return "{\"available\":false,\"enabled\":false}";
        }
        return result.toString();
    }

    private void publishBluetoothEvent(String message) {
        if (musicWebView == null) {
            return;
        }
        musicWebView.post(() -> musicWebView.evaluateJavascript(
                "window.onNativeBluetoothEvent && window.onNativeBluetoothEvent(" + JSONObject.quote(message) + ");",
                null
        ));
    }

    private void publishBluetoothPlaybackState() {
        if (musicWebView == null) {
            return;
        }
        String state = bluetoothPlaybackStateJson();
        musicWebView.post(() -> musicWebView.evaluateJavascript(
                "window.onNativeBluetoothPlaybackState && window.onNativeBluetoothPlaybackState(" + JSONObject.quote(state) + ");",
                null
        ));
    }

    private void publishSystemVolumeState() {
        if (musicWebView == null) {
            return;
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager == null ? 0 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager == null ? 0 : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        double volume = maxVolume <= 0 ? 0 : (double) currentVolume / (double) maxVolume;
        musicWebView.post(() -> musicWebView.evaluateJavascript(
                "window.onNativeSystemVolumeChange && window.onNativeSystemVolumeChange(" + volume + ");",
                null
        ));
    }

    private String bluetoothPlaybackStateJson() {
        JSONObject result = new JSONObject();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        try {
            int maxVolume = audioManager == null ? 0 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager == null ? 0 : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int connectedCount = getConnectedA2dpDeviceCount();
            boolean musicActive = audioManager != null && audioManager.isMusicActive();
            boolean sinkConnected = !getConnectedProfileDevices(bluetoothA2dpSink).isEmpty();
            boolean sourceConnected = false;
            try {
                sourceConnected = bluetoothA2dp != null
                        && hasBluetoothConnectPermission()
                        && !bluetoothA2dp.getConnectedDevices().isEmpty();
            } catch (SecurityException ignored) {
                sourceConnected = false;
            }
            result.put("connected", connectedCount > 0);
            boolean playing = connectedCount > 0 && (bluetoothRemotePlayingKnown ? bluetoothRemotePlaying : musicActive);
            result.put("playing", playing);
            result.put("volume", maxVolume <= 0 ? 0 : (double) currentVolume / (double) maxVolume);
            result.put("timestamp", System.currentTimeMillis());
            result.put("audioRole", sinkConnected ? "sink" : sourceConnected ? "source" : "none");
            result.put("connectedCount", connectedCount);
            result.put("musicActive", musicActive);
            result.put("speakerphoneOn", audioManager != null && audioManager.isSpeakerphoneOn());
            result.put("a2dpSinkConnected", sinkConnected);
            result.put("trackTitle", bluetoothRemoteTitle);
            result.put("trackArtist", bluetoothRemoteArtist);
            result.put("trackAlbum", bluetoothRemoteAlbum);
            result.put("trackCover", bluetoothRemoteCoverBase64);
            appendBluetoothRemoteProgress(result, playing);
        } catch (Exception ignored) {
            return "{\"connected\":false,\"playing\":false,\"progressKnown\":false}";
        }
        return result.toString();
    }

    private void appendBluetoothRemoteProgress(JSONObject result, boolean playing) throws Exception {
        if (bluetoothRemoteProgressMs < 0L) {
            result.put("progressKnown", false);
            return;
        }
        long now = System.currentTimeMillis();
        long positionMs = bluetoothRemoteProgressMs;
        if (playing && bluetoothRemoteProgressUpdatedAtMs > 0L) {
            positionMs += Math.max(0L, now - bluetoothRemoteProgressUpdatedAtMs);
        }
        if (bluetoothRemoteDurationMs > 0L) {
            positionMs = Math.min(positionMs, bluetoothRemoteDurationMs);
            result.put("durationSeconds", bluetoothRemoteDurationMs / 1000.0);
        }
        result.put("progressKnown", true);
        result.put("progressSeconds", Math.max(0L, positionMs) / 1000.0);
        result.put("progressTimestamp", now);
    }

    private void markBluetoothPlaybackDisconnected() {
        bluetoothRemotePlayingKnown = true;
        bluetoothRemotePlaying = false;
        bluetoothRemoteProgressUpdatedAtMs = System.currentTimeMillis();
        bluetoothRemoteCoverBase64 = "";
        setBluetoothConnectionState(BT_STATE_DISCONNECTED);
        publishBluetoothPlaybackState();
    }

    private void updateBluetoothSinkPlayingState(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
        if (state == A2DP_SINK_STATE_PLAYING) {
            bluetoothRemotePlayingKnown = true;
            bluetoothRemotePlaying = true;
            prepareBluetoothSpeakerRoute();
            if (BT_STATE_CONNECTED.equals(bluetoothConnectionState)
                    || BT_STATE_PAUSED.equals(bluetoothConnectionState)) {
                setBluetoothConnectionState(BT_STATE_PLAYING);
            }
        } else if (state == A2DP_SINK_STATE_NOT_PLAYING || state == BluetoothProfile.STATE_DISCONNECTED) {
            bluetoothRemotePlayingKnown = true;
            bluetoothRemotePlaying = false;
            if (BT_STATE_PLAYING.equals(bluetoothConnectionState)) {
                setBluetoothConnectionState(BT_STATE_PAUSED);
            }
        }
    }

    private void updateBluetoothRemotePlaybackExtras(@Nullable Intent intent) {
        Bundle extras;
        try {
            extras = intent == null ? null : intent.getExtras();
        } catch (RuntimeException ignored) {
            return;
        }
        if (extras == null) {
            return;
        }
        updateBluetoothPlaybackStateFromExtras(extras);
        updateBluetoothMetadataFromExtras(extras);
        long positionMs = findBluetoothMediaExtraMs(extras, "position", "elapsed", "progress");
        long durationMs = findBluetoothMediaExtraMs(extras, "duration", "length");
        if (positionMs >= 0L) {
            bluetoothRemoteProgressMs = positionMs;
            bluetoothRemoteProgressUpdatedAtMs = System.currentTimeMillis();
        }
        if (durationMs > 0L) {
            bluetoothRemoteDurationMs = durationMs;
        }
    }

    @SuppressWarnings("deprecation")
    private void updateBluetoothPlaybackStateFromExtras(Bundle extras) {
        PlaybackState playbackState = getParcelableExtraSafely(extras, EXTRA_AVRCP_CONTROLLER_PLAYBACK, PlaybackState.class);
        if (playbackState == null) {
            playbackState = findParcelableExtraSafely(extras, PlaybackState.class);
        }
        if (playbackState == null) {
            return;
        }
        int state = playbackState.getState();
        bluetoothRemotePlayingKnown = true;
        bluetoothRemotePlaying = state == PlaybackState.STATE_PLAYING
                || state == PlaybackState.STATE_FAST_FORWARDING
                || state == PlaybackState.STATE_REWINDING
                || state == PlaybackState.STATE_BUFFERING
                || state == PlaybackState.STATE_CONNECTING;
        long position = playbackState.getPosition();
        if (position >= 0L) {
            bluetoothRemoteProgressMs = position;
            bluetoothRemoteProgressUpdatedAtMs = System.currentTimeMillis();
        }
    }

    private void updateBluetoothMetadataFromExtras(Bundle extras) {
        MediaMetadata metadata = getParcelableExtraSafely(extras, EXTRA_AVRCP_CONTROLLER_METADATA, MediaMetadata.class);
        if (metadata == null) {
            metadata = findParcelableExtraSafely(extras, MediaMetadata.class);
        }
        if (metadata == null) {
            return;
        }
        bluetoothRemoteTitle = getMetadataText(metadata, MediaMetadata.METADATA_KEY_TITLE);
        bluetoothRemoteArtist = getMetadataText(metadata, MediaMetadata.METADATA_KEY_ARTIST);
        bluetoothRemoteAlbum = getMetadataText(metadata, MediaMetadata.METADATA_KEY_ALBUM);
        long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        // 新曲目元数据若未携带有效时长，则重置为未知，避免沿用上一曲的旧时长
        bluetoothRemoteDurationMs = duration > 0L ? duration : -1L;
        // 同样重置封面，避免新曲目无封面时沿用上一曲封面
        String cover = extractBluetoothAlbumArtBase64(metadata);
        bluetoothRemoteCoverBase64 = cover == null ? "" : cover;
    }

    /**
     * 从 MediaMetadata 中提取专辑封面并转为 Base64 JPEG。
     * 优先级：ALBUM_ART > ART > DISPLAY_ICON。
     * 为保证与源设备图像质量和比例一致，仅在尺寸过大时等比缩放，避免过度压缩。
     */
    @Nullable
    private String extractBluetoothAlbumArtBase64(MediaMetadata metadata) {
        Bitmap cover = null;
        try {
            cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        } catch (Exception ignored) {
            cover = null;
        }
        if (cover == null) {
            try {
                cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            } catch (Exception ignored) {
                cover = null;
            }
        }
        if (cover == null) {
            try {
                cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            } catch (Exception ignored) {
                cover = null;
            }
        }
        if (cover == null || cover.isRecycled()) {
            return null;
        }
        try {
            int maxEdge = Math.max(cover.getWidth(), cover.getHeight());
            // 等比缩放，避免超大封面导致 JS 桥传输过慢，同时保留原始宽高比例
            if (maxEdge > BLUETOOTH_COVER_MAX_EDGE_PX) {
                float scale = (float) BLUETOOTH_COVER_MAX_EDGE_PX / (float) maxEdge;
                int targetWidth = Math.max(1, Math.round(cover.getWidth() * scale));
                int targetHeight = Math.max(1, Math.round(cover.getHeight() * scale));
                cover = Bitmap.createScaledBitmap(cover, targetWidth, targetHeight, true);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            cover.compress(Bitmap.CompressFormat.JPEG, BLUETOOTH_COVER_JPEG_QUALITY, output);
            return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getMetadataText(MediaMetadata metadata, String key) {
        CharSequence value = metadata.getText(key);
        return value == null ? "" : value.toString();
    }

    @Nullable
    private <T> T getParcelableExtraSafely(Bundle extras, String key, Class<T> type) {
        try {
            Object value = extras.getParcelable(key);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private <T> T findParcelableExtraSafely(Bundle extras, Class<T> type) {
        Set<String> keys;
        try {
            keys = extras.keySet();
        } catch (RuntimeException ignored) {
            return null;
        }
        for (String key : keys) {
            T value = getParcelableExtraSafely(extras, key, type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private long findBluetoothMediaExtraMs(Bundle extras, String... keyHints) {
        Set<String> keys;
        try {
            keys = extras.keySet();
        } catch (RuntimeException ignored) {
            return -1L;
        }
        for (String key : keys) {
            if (key == null || !matchesBluetoothMediaKey(key, keyHints)) {
                continue;
            }
            Object value;
            try {
                value = extras.get(key);
            } catch (RuntimeException ignored) {
                continue;
            }
            Long numericValue = coerceLong(value);
            if (numericValue != null && numericValue >= 0L) {
                return normalizeBluetoothMediaTimeMs(numericValue);
            }
        }
        return -1L;
    }

    private boolean matchesBluetoothMediaKey(String key, String... hints) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("state") || lowerKey.contains("status")) {
            return false;
        }
        for (String hint : hints) {
            if (lowerKey.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Long coerceLong(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private long normalizeBluetoothMediaTimeMs(long value) {
        return value > 0L && value < 10000L ? value * 1000L : value;
    }

    private boolean invokeA2dpMethod(String methodName, BluetoothDevice device) {
        if (bluetoothA2dp == null || device == null || !hasBluetoothConnectPermission()) {
            return false;
        }
        try {
            Method method = BluetoothA2dp.class.getMethod(methodName, BluetoothDevice.class);
            Object result = method.invoke(bluetoothA2dp, device);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean invokeBluetoothProfileMethod(@Nullable BluetoothProfile profile, String methodName, BluetoothDevice device) {
        if (profile == null || device == null || !hasBluetoothConnectPermission()) {
            return false;
        }
        try {
            Method method = findBluetoothMethod(profile, methodName, BluetoothDevice.class);
            if (method == null) {
                return false;
            }
            Object result = method.invoke(profile, device);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int getProfileConnectionState(@Nullable BluetoothProfile profile, BluetoothDevice device) {
        if (profile == null || device == null || !hasBluetoothConnectPermission()) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        try {
            Method method = findBluetoothMethod(profile, "getConnectionState", BluetoothDevice.class);
            if (method == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            Object result = method.invoke(profile, device);
            return result instanceof Integer ? (Integer) result : BluetoothProfile.STATE_DISCONNECTED;
        } catch (Exception ignored) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    @SuppressWarnings("unchecked")
    private List<BluetoothDevice> getConnectedProfileDevices(@Nullable BluetoothProfile profile) {
        if (profile == null || !hasBluetoothConnectPermission()) {
            return java.util.Collections.emptyList();
        }
        try {
            if (profile instanceof BluetoothA2dp) {
                return ((BluetoothA2dp) profile).getConnectedDevices();
            }
            Method method = profile.getClass().getMethod("getConnectedDevices");
            Object result = method.invoke(profile);
            if (result instanceof List<?>) {
                return (List<BluetoothDevice>) result;
            }
        } catch (Exception ignored) {
        }
        return java.util.Collections.emptyList();
    }

    private Method findBluetoothMethod(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    Method method = type.getDeclaredMethod(methodName, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException exception) {
                    type = type.getSuperclass();
                } catch (SecurityException exception) {
                    return null;
                }
            }
            return null;
        }
    }

    private void schedulePendingBluetoothConnect(long delayMillis) {
        if (pendingBluetoothConnectAddress.length() == 0) {
            return;
        }
        long delay = Math.max(0L, Math.min(delayMillis, BLUETOOTH_FAST_CONNECT_RETRY_MS));
        bluetoothHandler.postDelayed(this::attemptPendingBluetoothConnect, delay);
    }

    private void clearPendingBluetoothConnect() {
        pendingBluetoothConnectAddress = "";
        pendingBluetoothConnectAttempts = 0;
        pendingBluetoothConnectStartedAtMs = 0L;
    }

    private void clearBluetoothAutoReconnect() {
        bluetoothAutoReconnectAddress = "";
        bluetoothAutoReconnectAttempts = 0;
        userInitiatedBluetoothDisconnect = false;
    }

    private void clearAllBluetoothAutoReconnect() {
        bluetoothAutoReconnectAddress = "";
        bluetoothAutoReconnectAttempts = 0;
        userInitiatedBluetoothDisconnect = false;
        clearPendingBluetoothConnect();
    }

    private void scheduleBluetoothAutoReconnect(@Nullable BluetoothDevice device, String reason) {
        if (device == null || userInitiatedBluetoothDisconnect || !hasBluetoothConnectPermission()) {
            return;
        }
        bluetoothAutoReconnectAddress = device.getAddress();
        if (bluetoothAutoReconnectAttempts >= BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS) {
            completeBluetoothConnectFailure(device, "\u84dd\u7259\u81ea\u52a8\u91cd\u8fde\u5931\u8d25\uff1a" + reason);
            publishBluetoothEvent("\u84dd\u7259\u8fde\u63a5\u5f02\u5e38\u65ad\u5f00\uff0c\u81ea\u52a8\u91cd\u8fde\u672a\u6210\u529f\uff0c\u8bf7\u9760\u8fd1\u8bbe\u5907\u540e\u624b\u52a8\u91cd\u8bd5");
            clearBluetoothAutoReconnect();
            return;
        }
        bluetoothAutoReconnectAttempts += 1;
        publishBluetoothEvent("\u84dd\u7259\u8fde\u63a5\u5f02\u5e38\u65ad\u5f00\uff0c\u6b63\u5728\u81ea\u52a8\u91cd\u8fde\uff08\u7b2c "
                + bluetoothAutoReconnectAttempts + "/"
                + BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS + " 次）");
        long backoffDelay = BLUETOOTH_RECONNECT_BACKOFF_BASE_MS * (long) Math.pow(2, bluetoothAutoReconnectAttempts - 1);
        backoffDelay = Math.min(backoffDelay, BLUETOOTH_AUTO_RECONNECT_DELAY_MS * 5);
        bluetoothHandler.postDelayed(() -> {
            BluetoothDevice reconnectDevice = getRemoteDevice(bluetoothAutoReconnectAddress);
            if (reconnectDevice == null || bluetoothAutoReconnectAddress.length() == 0) {
                return;
            }
            if (isA2dpConnected(reconnectDevice) || isA2dpSinkConnected(reconnectDevice)) {
                confirmedBluetoothAudioAddress = reconnectDevice.getAddress();
                completeBluetoothConnectSuccess(reconnectDevice, "\u84dd\u7259\u81ea\u52a8\u91cd\u8fde\u6210\u529f");
                publishBluetoothEvent("\u84dd\u7259\u81ea\u52a8\u91cd\u8fde\u6210\u529f");
                publishBluetoothPlaybackState();
                return;
            }
            pendingBluetoothConnectAddress = reconnectDevice.getAddress();
            pendingBluetoothConnectAttempts = 0;
            beginBluetoothConnectMetrics(reconnectDevice);
            schedulePendingBluetoothConnect(0);
        }, backoffDelay);
    }

    private void setBluetoothConnectionState(String state) {
        bluetoothConnectionState = state;
    }

    private void recordBluetoothError(String error, String suggestion) {
        bluetoothLastError = error == null ? "" : error;
        bluetoothLastErrorAtMs = System.currentTimeMillis();
        bluetoothErrorRecoverySuggestion = suggestion == null ? "" : suggestion;
        setBluetoothConnectionState(BT_STATE_ERROR);
    }

    private void clearBluetoothError() {
        bluetoothLastError = "";
        bluetoothErrorRecoverySuggestion = "";
    }

    private void checkBluetoothConnectionStatus() {
        if (confirmedBluetoothAudioAddress.length() == 0) {
            if (!BT_STATE_IDLE.equals(bluetoothConnectionState)
                    && !BT_STATE_DISCOVERING.equals(bluetoothConnectionState)
                    && !BT_STATE_DISCONNECTED.equals(bluetoothConnectionState)
                    && !BT_STATE_ERROR.equals(bluetoothConnectionState)) {
                setBluetoothConnectionState(BT_STATE_DISCONNECTED);
            }
            return;
        }
        BluetoothDevice device = getRemoteDevice(confirmedBluetoothAudioAddress);
        if (device == null) {
            setBluetoothConnectionState(BT_STATE_ERROR);
            recordBluetoothError("设备引用已失效", "请重新启动应用或重置蓝牙");
            return;
        }
        boolean stillConnected = isA2dpConnected(device) || isA2dpSinkConnected(device);
        if (!stillConnected) {
            if (confirmedBluetoothConnectedAtMs > 0L && System.currentTimeMillis() - confirmedBluetoothConnectedAtMs > 3000L) {
                recordBluetoothDisconnect(device);
                setBluetoothConnectionState(BT_STATE_DISCONNECTED);
                if (!userInitiatedBluetoothDisconnect) {
                    scheduleBluetoothAutoReconnect(device, "连接状态检查发现断开");
                }
            }
        } else {
            if (!BT_STATE_CONNECTED.equals(bluetoothConnectionState)
                    && !BT_STATE_PLAYING.equals(bluetoothConnectionState)
                    && !BT_STATE_PAUSED.equals(bluetoothConnectionState)) {
                setBluetoothConnectionState(BT_STATE_CONNECTED);
                clearBluetoothError();
            }
        }
    }

    private void checkSystemConnectedBluetoothDevices() {
        if (!hasBluetoothConnectPermission()) {
            return;
        }
        ensureA2dpProxy();
        bluetoothHandler.postDelayed(() -> {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null && audioManager.isBluetoothA2dpOn()) {
                List<BluetoothDevice> a2dpDevices = getConnectedProfileDevices(bluetoothA2dp);
                List<BluetoothDevice> sinkDevices = getConnectedProfileDevices(bluetoothA2dpSink);
                if (!a2dpDevices.isEmpty()) {
                    BluetoothDevice device = a2dpDevices.get(0);
                    if (!device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                        confirmedBluetoothAudioAddress = device.getAddress();
                        rememberBluetoothDevice(device);
                        rememberBluetoothControlTarget(device);
                        prepareBluetoothMusicRoute();
                        String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                        publishBluetoothEvent(message);
                        publishBluetoothPlaybackState();
                        return;
                    }
                }
                if (!sinkDevices.isEmpty()) {
                    BluetoothDevice device = sinkDevices.get(0);
                    if (!device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                        confirmedBluetoothAudioAddress = device.getAddress();
                        rememberBluetoothDevice(device);
                        rememberBluetoothControlTarget(device);
                        prepareBluetoothSpeakerRoute();
                        String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u8fde\u63a5");
                        publishBluetoothEvent(message);
                        publishBluetoothPlaybackState();
                        return;
                    }
                }
                if (confirmedBluetoothAudioAddress.length() == 0) {
                    for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            int state = bluetoothA2dp != null ? bluetoothA2dp.getConnectionState(device) : BluetoothProfile.STATE_DISCONNECTED;
                            if (state == BluetoothProfile.STATE_CONNECTED) {
                                confirmedBluetoothAudioAddress = device.getAddress();
                                rememberBluetoothDevice(device);
                                rememberBluetoothControlTarget(device);
                                prepareBluetoothMusicRoute();
                                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                                publishBluetoothEvent(message);
                                publishBluetoothPlaybackState();
                                return;
                            }
                        }
                    }
                }
            }
        }, 500);
    }

    private void startBluetoothConnectionStatusChecker() {
        stopBluetoothConnectionStatusChecker();
        bluetoothHandler.post(bluetoothConnectionStatusCheckRunnable);
    }

    private void stopBluetoothConnectionStatusChecker() {
        bluetoothHandler.removeCallbacks(bluetoothConnectionStatusCheckRunnable);
    }

    private void rememberBluetoothControlTarget(BluetoothDevice device) {
        if (device != null) {
            requestedBluetoothControlAddress = device.getAddress();
        }
    }

    private void clearBluetoothControlTarget(@Nullable BluetoothDevice device) {
        if (device == null || device.getAddress().equals(requestedBluetoothControlAddress)) {
            requestedBluetoothControlAddress = "";
        }
    }

    private void attemptPendingBluetoothConnect() {
        if (pendingBluetoothConnectAddress.length() == 0) {
            return;
        }
        BluetoothDevice device = getRemoteDevice(pendingBluetoothConnectAddress);
        if (device == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || !hasBluetoothConnectPermission()) {
            completeBluetoothConnectFailure(device, "\u84dd\u7259\u8fde\u63a5\u5df2\u53d6\u6d88\uff0c\u8bf7\u68c0\u67e5\u8bbe\u5907\u548c\u6743\u9650");
            clearPendingBluetoothConnect();
            publishBluetoothEvent("\u84dd\u7259\u8fde\u63a5\u5df2\u53d6\u6d88\uff0c\u8bf7\u68c0\u67e5\u8bbe\u5907\u548c\u6743\u9650");
            return;
        }
        try {
            rememberBluetoothDevice(device);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                if (!hasPendingBluetoothConnectTimedOut()) {
                    schedulePendingBluetoothConnect(BLUETOOTH_FAST_CONNECT_RETRY_MS);
                } else {
                    completeBluetoothConnectFailure(device, "\u84dd\u7259\u914d\u5bf9\u786e\u8ba4\u8d85\u65f6");
                    clearPendingBluetoothConnect();
                    publishBluetoothEvent("\u84dd\u7259\u914d\u5bf9\u786e\u8ba4\u8d85\u65f6\uff0c\u8bf7\u91cd\u65b0\u641c\u7d22\u5e76\u8fde\u63a5");
                }
                return;
            }
            if (isA2dpSinkConnected(device)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothControlTarget(device);
                prepareBluetoothSpeakerRoute();
                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u8fde\u63a5");
                clearPendingBluetoothConnect();
                publishBluetoothEvent(message);
                publishBluetoothPlaybackState();
                return;
            }
            if (isA2dpConnected(device)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothControlTarget(device);
                prepareBluetoothMusicRoute();
                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                clearPendingBluetoothConnect();
                publishBluetoothEvent(message);
                publishBluetoothPlaybackState();
                return;
            }
            if (hasPendingBluetoothConnectTimedOut()) {
                completeBluetoothConnectFailure(device, "\u84dd\u7259\u8fde\u63a5\u786e\u8ba4\u8d85\u65f6");
                clearPendingBluetoothConnect();
                publishBluetoothEvent("\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u70b9\u51fb\u8bbe\u5907\u5f00\u542f\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
                return;
            }
            pendingBluetoothConnectAttempts += 1;
            publishBluetoothEvent("\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u70b9\u51fb\u8bbe\u5907\u5f00\u542f\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
            schedulePendingBluetoothConnect(BLUETOOTH_FAST_CONNECT_RETRY_MS);
        } catch (SecurityException exception) {
            completeBluetoothConnectFailure(device, "\u84dd\u7259\u8fde\u63a5\u6743\u9650\u88ab\u7cfb\u7edf\u62d2\u7edd");
            clearPendingBluetoothConnect();
            publishBluetoothEvent("\u84dd\u7259\u8fde\u63a5\u6743\u9650\u88ab\u7cfb\u7edf\u62d2\u7edd");
        }
    }

    /**
     * 请求蓝牙A2DP连接：
     * 发起蓝牙设备配对（如未配对），然后请求A2DP Profile连接
     */
    private String requestBluetoothA2dpConnection(BluetoothDevice device) {
        try {
            userInitiatedBluetoothDisconnect = false;
            ensureA2dpProxy();
            rememberBluetoothDevice(device);
            rememberBluetoothControlTarget(device);
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering() && hasBluetoothScanPermission()) {
                bluetoothAdapter.cancelDiscovery();
            }
            pendingBluetoothConnectAddress = device.getAddress();
            pendingBluetoothConnectAttempts = 0;
            beginBluetoothConnectMetrics(device);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                boolean started = device.getBondState() == BluetoothDevice.BOND_BONDING || device.createBond();
                if (!started) {
                    completeBluetoothConnectFailure(device, "\u914d\u5bf9\u53d1\u8d77\u5931\u8d25");
                    clearPendingBluetoothConnect();
                } else {
                    schedulePendingBluetoothConnect(BLUETOOTH_FAST_CONNECT_RETRY_MS);
                }
                return statusJson(started, started ? "\u5df2\u53d1\u8d77\u7cfb\u7edf\u914d\u5bf9\uff0c\u914d\u5bf9\u5b8c\u6210\u540e\u8bf7\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5f00\u542f\"\u5a92\u4f53\u97f3\u9891\"" : "\u914d\u5bf9\u53d1\u8d77\u5931\u8d25");
            }
            if (isA2dpSinkConnected(device)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothControlTarget(device);
                prepareBluetoothSpeakerRoute();
                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u7bb1\u63a5\u6536\u8bbe\u5907\u5df2\u8fde\u63a5");
                clearPendingBluetoothConnect();
                publishBluetoothPlaybackState();
                return statusJson(true, message);
            }
            if (isA2dpConnected(device)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothControlTarget(device);
                prepareBluetoothMusicRoute();
                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                clearPendingBluetoothConnect();
                publishBluetoothPlaybackState();
                return statusJson(true, message);
            }
            schedulePendingBluetoothConnect(100);
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u70b9\u51fb\u8bbe\u5907\u5f00\u542f\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        } catch (SecurityException exception) {
            clearPendingBluetoothConnect();
            return statusJson(false, "\u84dd\u7259\u8fde\u63a5\u6743\u9650\u88ab\u7cfb\u7edf\u62d2\u7edd");
        }
    }

    private int disconnectAllA2dpDevices() {
        if (bluetoothA2dp == null && bluetoothA2dpSink == null || !hasBluetoothConnectPermission()) {
            return 0;
        }
        int requested = 0;
        try {
            if (bluetoothA2dp != null) {
                for (BluetoothDevice device : bluetoothA2dp.getConnectedDevices()) {
                    rememberBluetoothDevice(device);
                    if (invokeA2dpMethod("disconnect", device)) {
                        requested += 1;
                    }
                }
            }
            for (BluetoothDevice device : getConnectedProfileDevices(bluetoothA2dpSink)) {
                rememberBluetoothDevice(device);
                if (invokeBluetoothProfileMethod(bluetoothA2dpSink, "disconnect", device)) {
                    requested += 1;
                }
            }
        } catch (SecurityException ignored) {
            return requested;
        }
        return requested;
    }

    private int disconnectOtherA2dpDevices(BluetoothDevice targetDevice) {
        if (bluetoothA2dp == null || targetDevice == null || !hasBluetoothConnectPermission()) {
            return 0;
        }
        int requested = 0;
        try {
            for (BluetoothDevice device : bluetoothA2dp.getConnectedDevices()) {
                rememberBluetoothDevice(device);
                if (!targetDevice.getAddress().equals(device.getAddress()) && invokeA2dpMethod("disconnect", device)) {
                    requested += 1;
                }
            }
        } catch (SecurityException ignored) {
            return requested;
        }
        return requested;
    }

    private int disconnectOtherA2dpSinkDevices(BluetoothDevice targetDevice) {
        if (bluetoothA2dpSink == null || targetDevice == null || !hasBluetoothConnectPermission()) {
            return 0;
        }
        int requested = 0;
        for (BluetoothDevice device : getConnectedProfileDevices(bluetoothA2dpSink)) {
            rememberBluetoothDevice(device);
            if (!targetDevice.getAddress().equals(device.getAddress())
                    && invokeBluetoothProfileMethod(bluetoothA2dpSink, "disconnect", device)) {
                requested += 1;
            }
        }
        return requested;
    }

    private int getConnectedA2dpDeviceCount() {
        if (!hasBluetoothConnectPermission()) {
            return 0;
        }
        int count = getConnectedProfileDevices(bluetoothA2dpSink).size();
        try {
            return count + (bluetoothA2dp == null ? 0 : bluetoothA2dp.getConnectedDevices().size());
        } catch (SecurityException ignored) {
            return count;
        }
    }

    /**
     * 准备蓝牙音频路由（作为音频输出源）：
     * 将音频输出路由切换到蓝牙A2DP设备，设置音频焦点和音频管理器参数
     */
    private void prepareBluetoothMusicRoute() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        if (!audioManager.isBluetoothA2dpOn() && getConnectedProfileDevices(bluetoothA2dp).isEmpty()) {
            return;
        }
        if (!getConnectedProfileDevices(bluetoothA2dpSink).isEmpty()) {
            prepareBluetoothSpeakerRoute();
            return;
        }
        activeAudioModule = "bluetooth";
        try {
            configureBluetoothAudioManager(audioManager, false);
        } catch (SecurityException ignored) {
            publishBluetoothEvent("\u84dd\u7259\u97f3\u9891\u8def\u7531\u6743\u9650\u4e0d\u8db3");
        }
    }

    /**
     * 准备蓝牙扬声器路由（作为音频接收端）：
     * 通过A2DP Sink接收蓝牙音频播放，并启用扬声器输出
     */
    private void prepareBluetoothSpeakerRoute() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        if (!getConnectedProfileDevices(bluetoothA2dpSink).isEmpty() || !getConnectedProfileDevices(bluetoothA2dp).isEmpty()) {
            activeAudioModule = "bluetooth";
            try {
                configureBluetoothAudioManager(audioManager, true);
            } catch (SecurityException ignored) {
                publishBluetoothEvent("\u84dd\u7259\u97f3\u7bb1\u626c\u58f0\u5668\u8def\u7531\u6743\u9650\u4e0d\u8db3");
            }
        }
    }

    private void configureBluetoothAudioManager(AudioManager audioManager, boolean forceSpeaker) {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        if (audioManager.isBluetoothA2dpOn()) {
            try {
                audioManager.setParameters("A2dpSuspended=false");
            } catch (Exception ignored) {
            }
        }
        try {
            audioManager.setParameters("BT_SCO=off");
        } catch (Exception ignored) {
        }
        audioManager.setSpeakerphoneOn(forceSpeaker);
        if (forceSpeaker) {
            selectBuiltInSpeaker(audioManager);
        }
        ensureMediaVolumeAudible(audioManager);
        audioManager.requestAudioFocus(
                bluetoothAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
    }

    private void selectBuiltInSpeaker(AudioManager audioManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        for (AudioDeviceInfo device : audioManager.getAvailableCommunicationDevices()) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                audioManager.setCommunicationDevice(device);
                return;
            }
        }
    }

    private void ensureMediaVolumeAudible(AudioManager audioManager) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume <= 0 || audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
            return;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(1, maxVolume / 3), 0);
    }
    private boolean sendBluetoothAvrcpCommandForKey(int keyCode) {
        ensureA2dpProxy();
        int avrcpCommand = avrcpCommandForKeyCode(keyCode);
        if (avrcpCommand < 0 || !hasBluetoothConnectPermission()) {
            return false;
        }
        List<BluetoothDevice> targetDevices = getBluetoothMediaControlTargets();
        boolean sent = false;
        for (BluetoothDevice device : targetDevices) {
            sent = sendBluetoothAvrcpPassThrough(device, avrcpCommand) || sent;
        }
        if (sent) {
            updateOptimisticBluetoothPlaybackState(keyCode);
        }
        return sent;
    }

    private List<BluetoothDevice> getBluetoothMediaControlTargets() {
        List<BluetoothDevice> targets = new ArrayList<>();
        appendUniqueBluetoothDevices(targets, getConnectedProfileDevices(bluetoothA2dpSink));
        appendUniqueBluetoothDevices(targets, getConnectedProfileDevices(bluetoothAvrcpController));
        if (confirmedBluetoothAudioAddress.length() > 0) {
            BluetoothDevice device = getRemoteDevice(confirmedBluetoothAudioAddress);
            if (device != null) {
                appendUniqueBluetoothDevice(targets, device);
            }
        }
        if (pendingBluetoothConnectAddress.length() > 0) {
            BluetoothDevice device = getRemoteDevice(pendingBluetoothConnectAddress);
            if (device != null) {
                appendUniqueBluetoothDevice(targets, device);
            }
        }
        if (requestedBluetoothControlAddress.length() > 0) {
            BluetoothDevice device = getRemoteDevice(requestedBluetoothControlAddress);
            if (device != null) {
                appendUniqueBluetoothDevice(targets, device);
            }
        }
        return targets;
    }

    private void appendUniqueBluetoothDevices(List<BluetoothDevice> targets, List<BluetoothDevice> devices) {
        for (BluetoothDevice device : devices) {
            appendUniqueBluetoothDevice(targets, device);
        }
    }

    private void appendUniqueBluetoothDevice(List<BluetoothDevice> targets, BluetoothDevice device) {
        if (device == null) {
            return;
        }
        String address = device.getAddress();
        for (BluetoothDevice existing : targets) {
            if (existing != null && existing.getAddress().equals(address)) {
                return;
            }
        }
        targets.add(device);
    }

    private int avrcpCommandForKeyCode(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return AVRCP_CMD_ID_PLAY;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            return AVRCP_CMD_ID_PAUSE;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            return AVRCP_CMD_ID_FORWARD;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            return AVRCP_CMD_ID_BACKWARD;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            return AVRCP_CMD_ID_FAST_FORWARD;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            return AVRCP_CMD_ID_REWIND;
        }
        return -1;
    }

    private boolean sendBluetoothAvrcpPassThrough(BluetoothDevice device, int command) {
        if (device == null || bluetoothAvrcpController == null) {
            return false;
        }
        try {
            Method method = findBluetoothMethod(
                    bluetoothAvrcpController,
                    "sendPassThroughCmd",
                    BluetoothDevice.class,
                    int.class,
                    int.class
            );
            if (method == null) {
                return false;
            }
            Object pressResult = method.invoke(bluetoothAvrcpController, device, command, AVRCP_PASS_THROUGH_STATE_PRESS);
            Object releaseResult = method.invoke(bluetoothAvrcpController, device, command, AVRCP_PASS_THROUGH_STATE_RELEASE);
            return isSuccessfulHiddenApiResult(pressResult) && isSuccessfulHiddenApiResult(releaseResult);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isSuccessfulHiddenApiResult(@Nullable Object result) {
        return result == null || !(result instanceof Boolean) || (Boolean) result;
    }

    private void updateOptimisticBluetoothPlaybackState(int keyCode) {
        bluetoothRemotePlayingKnown = true;
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            bluetoothRemotePlaying = true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            bluetoothRemotePlaying = false;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            bluetoothRemoteProgressMs = 0L;
            bluetoothRemoteProgressUpdatedAtMs = System.currentTimeMillis();
        }
    }

    /**
     * 发送媒体控制按键：
     * 优先通过AVRCP协议发送媒体命令到蓝牙设备，失败则回退到系统AudioManager分发
     */
    private boolean sendMediaKey(int keyCode) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return false;
        }
        boolean avrcpSent = false;
        boolean bluetoothMode = "bluetooth".equals(activeAudioModule);
        if ("bluetooth".equals(activeAudioModule)) {
            prepareBluetoothMusicRoute();
            avrcpSent = sendBluetoothAvrcpCommandForKey(keyCode);
        }
        boolean mediaKeyDispatched = false;
        if (!avrcpSent) {
            long now = System.currentTimeMillis();
            audioManager.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            mediaKeyDispatched = true;
            if (bluetoothMode && hasBluetoothAudioConnectionHint()) {
                updateOptimisticBluetoothPlaybackState(keyCode);
            }
        }
        publishBluetoothPlaybackState();
        bluetoothHandler.postDelayed(this::publishBluetoothPlaybackState, 600);
        return !bluetoothMode || avrcpSent || (mediaKeyDispatched && hasBluetoothAudioConnectionHint());
    }

    private boolean hasBluetoothAudioConnectionHint() {
        if (!hasBluetoothConnectPermission()) {
            return false;
        }
        if (getConnectedA2dpDeviceCount() > 0) {
            return true;
        }
        return hasUsableBluetoothAddress(confirmedBluetoothAudioAddress)
                || hasUsableBluetoothAddress(pendingBluetoothConnectAddress)
                || hasUsableBluetoothAddress(requestedBluetoothControlAddress);
    }

    private boolean hasUsableBluetoothAddress(String address) {
        return address != null && address.length() > 0 && getRemoteDevice(address) != null;
    }

    private String setMediaStreamVolume(float volume) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return statusJson(false, "\u97f3\u9891\u670d\u52a1\u4e0d\u53ef\u7528");
        }
        if ("bluetooth".equals(activeAudioModule)) {
            prepareBluetoothMusicRoute();
        }
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int target = Math.max(0, Math.min(max, Math.round(max * volume)));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        publishBluetoothPlaybackState();
        int connectedCount = getConnectedA2dpDeviceCount();
        return statusJson(true, connectedCount > 0
                ? "\u84dd\u7259\u5a92\u4f53\u97f3\u91cf\u5df2\u540c\u6b65"
                : "\u5a92\u4f53\u97f3\u91cf\u5df2\u8c03\u6574");
    }
    private void openBluetoothSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
        showBluetoothBackOverlay();
    }

    /**
     * 通过标准Android显式Intent启动车载系统收音机界面
     * 目标组件：com.android.car.radio/.RadioActivity
     * 遵循Android应用组件交互最佳实践，包含异常处理和多版本兼容性降级策略
     *
     * 支持嵌入式启动模式：当传入有效的bounds矩形时，目标Activity会显示在指定区域内，
     * 不会遮挡上方的tab切换栏；bounds无效时降级为全屏启动。
     *
     * @param left   目标显示区域左边界（屏幕像素坐标，0表示全屏）
     * @param top    目标显示区域上边界（屏幕像素坐标，通常为tab栏底部Y坐标）
     * @param right  目标显示区域右边界（屏幕像素坐标）
     * @param bottom 目标显示区域下边界（屏幕像素坐标，通常为屏幕高度）
     * @return JSON格式的启动结果，包含success、message、launchMode、bounds等字段
     */
    private String launchCarRadioActivity(int left, int top, int right, int bottom) {
        JSONObject result = new JSONObject();
        try {
            // 判断是否使用嵌入式bounds模式
            final boolean hasBounds = right > left && bottom > top;
            Rect bounds = null;
            if (hasBounds) {
                try {
                    bounds = new Rect(left, top, right, bottom);
                } catch (Exception ignored) {
                    bounds = null;
                }
            }
            final Rect finalBounds = bounds;

            // 1. 首选：使用显式ComponentName + 可选bounds启动目标收音机Activity
            Intent explicitIntent = new Intent();
            explicitIntent.setClassName(
                    "com.android.car.radio",
                    "com.android.car.radio.RadioActivity"
            );
            explicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // 构建ActivityOptions，设置启动bounds（API 24+），使目标Activity显示在tab栏下方区域
            ActivityOptions options = ActivityOptions.makeBasic();
            if (finalBounds != null) {
                try {
                    options.setLaunchBounds(finalBounds);
                } catch (Exception boundsEx) {
                    Log.w(TAG, "setLaunchBounds失败，降级为全屏启动: " + boundsEx.getMessage());
                }
            }

            try {
                startActivity(explicitIntent, options.toBundle());
                result.put("success", true);
                result.put("message", finalBounds != null
                        ? "已通过显式Intent嵌入式启动车载收音机界面（bounds区域显示，不遮挡tab栏）"
                        : "已通过显式Intent启动车载收音机界面（全屏模式）");
                result.put("launchMode", finalBounds != null ? "explicit_embedded" : "explicit");
                result.put("bounds", finalBounds != null ? finalBounds.toString() : "fullscreen");
                result.put("fallbackUsed", false);
                Log.d(TAG, "成功启动车载收音机：com.android.car.radio/.RadioActivity，bounds=" + finalBounds);
                // 注意：此处不得显示"返回"悬浮按钮（showBluetoothBackOverlay），
                // 否则会遮挡顶部界面切换栏，破坏切换栏与Radio界面的无缝衔接
                return result.toString();
            } catch (Exception explicitException) {
                Log.w(TAG, "显式Intent启动收音机失败，尝试降级方案: " + explicitException.getMessage());
            }

            // 2. 降级方案1：使用PackageManager查询并解析隐式Intent（ACTION_MAIN + CATEGORY_LAUNCHER）
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.android.car.radio");
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    ActivityOptions pmOptions = ActivityOptions.makeBasic();
                    if (finalBounds != null) {
                        try {
                            pmOptions.setLaunchBounds(finalBounds);
                        } catch (Exception ignored) {
                        }
                    }
                    startActivity(launchIntent, pmOptions.toBundle());
                    result.put("success", true);
                    result.put("message", "已通过PackageManager启动车载收音机应用");
                    result.put("launchMode", "packageManager");
                    result.put("bounds", finalBounds != null ? finalBounds.toString() : "fullscreen");
                    result.put("fallbackUsed", true);
                    Log.d(TAG, "通过PackageManager降级启动收音机成功");
                    return result.toString();
                }
            } catch (Exception pmException) {
                Log.w(TAG, "PackageManager降级方案也失败: " + pmException.getMessage());
            }

            // 3. 降级方案2：尝试使用通用收音机Intent（适配不同厂商定制的ROM）
            try {
                Intent genericRadioIntent = new Intent(Intent.ACTION_MAIN);
                genericRadioIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                genericRadioIntent.setPackage("com.android.car.radio");
                genericRadioIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ActivityOptions genericOptions = ActivityOptions.makeBasic();
                if (finalBounds != null) {
                    try {
                        genericOptions.setLaunchBounds(finalBounds);
                    } catch (Exception ignored) {
                    }
                }
                startActivity(genericRadioIntent, genericOptions.toBundle());
                result.put("success", true);
                result.put("message", "已通过通用Intent启动车载收音机");
                result.put("launchMode", "generic");
                result.put("bounds", finalBounds != null ? finalBounds.toString() : "fullscreen");
                result.put("fallbackUsed", true);
                Log.d(TAG, "通过通用Intent降级启动收音机成功");
                return result.toString();
            } catch (Exception genericException) {
                Log.w(TAG, "通用Intent降级方案也失败: " + genericException.getMessage());
            }

            // 4. 所有方案都失败，返回错误信息
            result.put("success", false);
            result.put("message", "当前系统未检测到标准车载收音机应用（com.android.car.radio），请确认ROM是否包含收音机组件");
            result.put("launchMode", "none");
            result.put("fallbackUsed", true);
            result.put("errorDetail", "所有启动方案均失败：显式Intent→PackageManager→通用Intent");
            return result.toString();

        } catch (Exception outerException) {
            try {
                result.put("success", false);
                result.put("message", "启动收音机界面时发生异常：" + outerException.getMessage());
                result.put("launchMode", "error");
                result.put("errorDetail", outerException.getClass().getSimpleName());
            } catch (Exception ignored) {
            }
            Log.e(TAG, "启动收音机界面总异常", outerException);
            return result.toString();
        }
    }

    /**
     * 将当前应用MainActivity重新拉到前台，覆盖其他Activity（如收音机Activity）。
     * 用于切换离开收音机模块时，将RadioActivity遮挡在自身窗口下方，恢复应用界面。
     *
     * @return JSON格式的操作结果
     */
    private String bringMainActivityToFront() {
        JSONObject result = new JSONObject();
        try {
            Intent bringToFront = new Intent(this, MainActivity.class);
            bringToFront.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(bringToFront);
            result.put("success", true);
            result.put("message", "应用已回到前台");
            Log.d(TAG, "MainActivity已重新拉到前台，覆盖收音机Activity");
        } catch (Exception e) {
            try {
                result.put("success", false);
                result.put("message", "回到前台失败：" + e.getMessage());
            } catch (Exception ignored) {
            }
            Log.e(TAG, "MainActivity回到前台失败", e);
        }
        return result.toString();
    }

    private void showBluetoothBackOverlay() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        if (windowManager == null) {
            return;
        }
        if (bluetoothBackOverlay != null) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
            return;
        }

        int horizontalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 18f, getResources().getDisplayMetrics());
        int verticalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12f, getResources().getDisplayMetrics());
        int cornerRadius = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());

        TextView backButton = new TextView(this);
        backButton.setText("\u8fd4\u56de");
        backButton.setTextColor(Color.WHITE);
        backButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        backButton.setGravity(Gravity.CENTER);
        backButton.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        backButton.setOnClickListener(v -> {
            hideBluetoothBackOverlay();
            Intent backIntent = new Intent(this, MainActivity.class);
            backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(backIntent);
            } catch (Exception ignored) {
            }
        });

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(cornerRadius);
        background.setColor(0xE62181FF);
        backButton.setBackground(background);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        int marginX = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20f, getResources().getDisplayMetrics());
        int marginY = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
        params.x = marginX;
        params.y = marginY;

        try {
            windowManager.addView(backButton, params);
            bluetoothBackOverlay = backButton;
        } catch (Exception ignored) {
        }
    }

    private void hideBluetoothBackOverlay() {
        if (windowManager != null && bluetoothBackOverlay != null) {
            try {
                windowManager.removeView(bluetoothBackOverlay);
            } catch (Exception ignored) {
            }
            bluetoothBackOverlay = null;
        }
    }

    /**
     * 蓝牙设备连接成功后，若当前正处于系统蓝牙设置界面（悬浮返回按钮显示中），
     * 则自动收起返回按钮并将应用拉回前台，回退到蓝牙音乐界面。
     */
    private void autoReturnToBluetoothOnConnected() {
        if (bluetoothBackOverlay != null && "bluetooth".equals(activeAudioModule)) {
            hideBluetoothBackOverlay();
            bringMainActivityToFront();
            Log.d(TAG, "蓝牙设备已连接，自动返回蓝牙音乐界面");
        }
    }

    /**
     * 显示悬浮tab栏，覆盖在系统收音机Activity之上
     * 当设备不支持setLaunchBounds嵌入式启动（RadioActivity全屏显示）时，
     * 通过此悬浮窗保持上方tab切换栏可见可点击，用户可点击tab切换回其他模块
     *
     * @param left       tab栏左边界的屏幕像素坐标
     * @param top        tab栏上边界的屏幕像素坐标（通常为状态栏高度）
     * @param right      tab栏右边界的屏幕像素坐标
     * @param bottom     tab栏下边界的屏幕像素坐标
     * @param lightTheme 是否为浅色主题（影响tab栏颜色）
     * @param labels     三个tab按钮的文本（蓝牙/收音机/U盘）
     */
    private void showRadioTabOverlay(int left, int top, int right, int bottom, boolean lightTheme, String[] labels) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        if (windowManager == null) {
            return;
        }
        // 先移除已有的悬浮tab栏
        hideRadioTabOverlay();
        if (!Settings.canDrawOverlays(this)) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
            return;
        }

        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) {
            return;
        }

        // 颜色配置（与Web端CSS变量一致）
        // 深色模式：白色透明叠加；浅色模式：黑色透明叠加
        int inactiveTextColor = lightTheme ? 0x94000000 : 0x94FFFFFF;   // rgba(0,0,0,0.58) / rgba(255,255,255,0.58)
        int activeTextColor = lightTheme ? 0xFF333333 : 0xFFF1F4FF;     // #333333 / #f1f4ff
        int inactiveBgColor = lightTheme ? 0x08000000 : 0x08FFFFFF;     // rgba(0,0,0,0.03) / rgba(255,255,255,0.03)
        int activeBgColor = lightTheme ? 0x12000000 : 0x12FFFFFF;       // rgba(0,0,0,0.07) / rgba(255,255,255,0.07)

        float density = getResources().getDisplayMetrics().density;
        int cornerRadius = (int) (14f * density); // 14dp 圆角
        int tabGap = (int) (8f * density);        // 8dp 间距

        // 创建tab栏容器：水平三等分布局
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        container.setWeightSum(3);

        // tab配置：目标模块、是否激活
        String[] modules = {"bluetooth", "radio", "usb"};
        if (labels == null || labels.length < 3) {
            labels = new String[]{"蓝牙音乐", "收音机", "U盘音乐"};
        }

        for (int i = 0; i < 3; i++) {
            TextView tabButton = new TextView(this);
            android.widget.LinearLayout.LayoutParams tabParams = new android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
            );
            // 设置tab间距（左右各留gap/2）
            if (i == 0) {
                tabParams.setMarginStart(0);
                tabParams.setMarginEnd(tabGap / 2);
            } else if (i == 2) {
                tabParams.setMarginStart(tabGap / 2);
                tabParams.setMarginEnd(0);
            } else {
                tabParams.setMarginStart(tabGap / 2);
                tabParams.setMarginEnd(tabGap / 2);
            }
            tabButton.setLayoutParams(tabParams);
            tabButton.setText(labels[i]);
            tabButton.setGravity(Gravity.CENTER);
            tabButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tabButton.setIncludeFontPadding(false);
            try {
                tabButton.setTypeface(tabButton.getTypeface(), android.graphics.Typeface.BOLD);
            } catch (Exception ignored) {
            }

            final boolean isActive = "radio".equals(modules[i]);
            // 设置背景（圆角矩形）
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(cornerRadius);
            bg.setColor(isActive ? activeBgColor : inactiveBgColor);
            tabButton.setBackground(bg);
            tabButton.setTextColor(isActive ? activeTextColor : inactiveTextColor);

            final String targetModule = modules[i];
            tabButton.setOnClickListener(v -> {
                // 点击tab按钮：启动MainActivity并传递目标模块参数
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("extra_switch_module", targetModule);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            });

            container.addView(tabButton);
        }

        // 配置WindowManager参数
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = left;
        params.y = top;

        try {
            windowManager.addView(container, params);
            radioTabOverlay = container;
            Log.d(TAG, "悬浮tab栏已显示: bounds=[" + left + "," + top + "," + right + "," + bottom + "], lightTheme=" + lightTheme);
        } catch (Exception e) {
            Log.e(TAG, "显示悬浮tab栏失败", e);
        }
    }

    /**
     * 隐藏悬浮tab栏
     */
    private void hideRadioTabOverlay() {
        if (windowManager != null && radioTabOverlay != null) {
            try {
                windowManager.removeView(radioTabOverlay);
            } catch (Exception ignored) {
            }
            radioTabOverlay = null;
            Log.d(TAG, "悬浮tab栏已隐藏");
        }
    }

    private void showUsbDisconnectedNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(USB_NOTIFICATION_CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                        USB_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.usb_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(getString(R.string.usb_channel_desc));
                channel.setShowBadge(false);
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, USB_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(getString(R.string.usb_disconnected_title))
                .setContentText(getString(R.string.usb_disconnected_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setOngoing(false);
        try {
            notificationManager.notify(USB_DISCONNECTED_NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {
        }
    }

    private void cancelUsbDisconnectedNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                notificationManager.cancel(USB_DISCONNECTED_NOTIFICATION_ID);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();
            if ("bluetooth".equals(activeAudioModule)) {
                // 蓝牙模块：播放/暂停及上一曲/下一曲均通过AVRCP发送到蓝牙设备
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    publishBluetoothEvent("\u84dd\u7259\u97f3\u4e50\u7269\u7406\u6309\u952e\u64ad\u653e/\u6682\u505c\u5df2\u89e6\u53d1");
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    sendMediaKey(keyCode);
                    publishBluetoothEvent("\u84dd\u7259\u97f3\u4e50\u7269\u7406\u6309\u952e\u63a7\u5236\u5df2\u89e6\u53d1");
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    handlePrevNext(true);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    handlePrevNext(false);
                    return true;
                }
            } else {
                // 收音机：上一曲=低频搜台，下一曲=高频搜台；USB/本地：上一曲/下一曲
                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    handlePrevNext(true);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    handlePrevNext(false);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 处理上一曲/下一曲（物理按键与VHAL信号共用）：
     * 蓝牙模块走AVRCP下发，其余模块（收音机/U盘/本地）通过JS播放控制。
     */
    private void handlePrevNext(boolean next) {
        if ("bluetooth".equals(activeAudioModule)) {
            int keyCode = next ? KeyEvent.KEYCODE_MEDIA_NEXT : KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            sendMediaKey(keyCode);
            publishBluetoothEvent(next
                    ? "\u84dd\u7259\u97f3\u4e50\u4e0b\u4e00\u66f2\u5df2\u89e6\u53d1"
                    : "\u84dd\u7259\u97f3\u4e50\u4e0a\u4e00\u66f2\u5df2\u89e6\u53d1");
        } else {
            evaluatePlayerScript("window.onNativePlaybackControl&&window.onNativePlaybackControl('"
                    + (next ? "next" : "previous") + "');");
        }
    }

    /** 启动VHAL物理按键信号轮询（后台线程，连接失败会安全退出）。 */
    private void startVhalKeySignalPolling() {
        if (vhalKeyPolling) {
            return;
        }
        vhalKeyPolling = true;
        vhalKeyPollThread = new Thread(() -> {
            VhalSignalReader reader = VhalSignalReader.connect(getApplicationContext());
            if (reader == null) {
                Log.w(TAG, "VHAL物理按键信号轮询未启动: 连接失败");
                vhalKeyPolling = false;
                return;
            }
            vhalSignalReader = reader;
            while (vhalKeyPolling) {
                handleVhalKeySignal(reader.readKeySignal());
                try {
                    Thread.sleep(VHAL_KEY_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            reader.close();
            vhalSignalReader = null;
        }, "VhalKeySignalPoll");
        vhalKeyPollThread.start();
    }

    /** 停止VHAL物理按键信号轮询。 */
    private void stopVhalKeySignalPolling() {
        vhalKeyPolling = false;
        if (vhalKeyPollThread != null) {
            vhalKeyPollThread.interrupt();
            vhalKeyPollThread = null;
        }
    }

    /** 处理读取到的VHAL按键信号，仅在同一信号持续期间去重（边沿触发）。 */
    private void handleVhalKeySignal(int signal) {
        if (signal == VhalSignalReader.SIGNAL_NONE) {
            lastVhalKeySignal = signal;
            return;
        }
        if (signal == lastVhalKeySignal) {
            return;
        }
        lastVhalKeySignal = signal;
        boolean next = signal == VhalSignalReader.SIGNAL_NEXT;
        bluetoothHandler.post(() -> handlePrevNext(next));
    }

    private void updateLocalPlaybackNotification(String title, String artist, boolean playing) {
        boolean hasTrack = title != null && title.trim().length() > 0;
        if (!hasTrack && !playing) {
            localPlaybackPlaying = false;
            stopService(new Intent(this, MusicPlaybackService.class));
            return;
        }
        localPlaybackTitle = normalizePlaybackText(title, getString(R.string.default_title));
        localPlaybackArtist = normalizePlaybackText(artist, getString(R.string.default_artist));
        localPlaybackPlaying = playing;

        Intent intent = new Intent(this, MusicPlaybackService.class);
        intent.setAction(MusicPlaybackService.ACTION_UPDATE);
        intent.putExtra(MusicPlaybackService.EXTRA_TITLE, localPlaybackTitle);
        intent.putExtra(MusicPlaybackService.EXTRA_ARTIST, localPlaybackArtist);
        intent.putExtra(MusicPlaybackService.EXTRA_PLAYING, localPlaybackPlaying);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void dispatchLocalPlaybackCommand(String command) {
        if (musicWebView == null || command.length() == 0) {
            return;
        }
        String script = "window.onNativePlaybackControl&&window.onNativePlaybackControl("
                + JSONObject.quote(command)
                + ");";
        evaluatePlayerScript(script);
    }

    private void evaluatePlayerScript(String script) {
        if (musicWebView == null || script == null || script.length() == 0) {
            return;
        }
        musicWebView.post(() -> musicWebView.evaluateJavascript(script, null));
    }

    private String normalizePlaybackText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void injectSafeAreaCssVariables() {
        if (musicWebView == null) {
            return;
        }
        int safeTop = Math.max(0, statusBarTopPx);
        int safeBottom = Math.max(0, navBarBottomPx);
        // 同时注入顶部状态栏与底部导航栏真实高度，CSS端用 max(env(), native) 取较大值，
        // 确保 WebView 中 env(safe-area-inset-*) 不可靠时底部按键仍不会被系统导航栏遮挡
        String script = "document.documentElement.style.setProperty('--native-status-bar-top','"
                + safeTop
                + "px');document.documentElement.style.setProperty('--native-nav-bar-bottom','"
                + safeBottom
                + "px');";
        musicWebView.post(() -> musicWebView.evaluateJavascript(script, null));
    }

    private void applyStatusBarTheme(boolean lightBackground) {
        statusBarLightBackground = lightBackground;
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        // true means dark system-bar icons for light backgrounds; false keeps icons light.
        controller.setAppearanceLightStatusBars(lightBackground);
    }

    private void startUsbScanAsync() {
        if (usbScanning) {
            return;
        }
        final int token = ++usbScanToken;
        usbScanning = true;
        usbMusicStateJson = createUsbScanningJson();
        publishUsbEvent("scan_started", getString(R.string.usb_msg_reading));
        new Thread(() -> {
            String state = scanUsbMusicNow();
            if (token != usbScanToken) {
                return;
            }
            usbScanning = false;
            usbMusicStateJson = state;
            boolean hasTracks = getUsbTrackCount(state) > 0;
            publishUsbEvent("scan_completed", hasTracks
                    ? getString(R.string.usb_msg_scan_complete)
                    : getString(R.string.usb_msg_no_music));
            syncFavoritesAfterScan(state);
        }, "UsbMusicScanner").start();
    }

    private String scanUsbMusicNow() {
        // 缺少存储访问权限时，File.listFiles() 在Android 11+ 会返回null/空，导致无法读取U盘文件
        if (!hasUsbStorageReadPermission()) {
            return createUsbErrorJson(getString(R.string.usb_msg_no_permission));
        }
        List<File> roots = findUsbRoots();
        if (roots.isEmpty()) {
            return createUsbDisconnectedJson(getString(R.string.usb_msg_not_connected));
        }
        File primaryRoot = roots.get(0);
        Map<String, UsbFolderBucket> folders = new LinkedHashMap<>();
        int[] fileCounters = new int[]{0, 0};
        long[] lastProgressTime = new long[]{0};
        List<File> discoveredFiles = new ArrayList<>();

        for (File root : roots) {
            discoverUsbAudioFiles(root, discoveredFiles, fileCounters, lastProgressTime);
        }

        Map<String, List<JSONObject>> enrichedTracks = new LinkedHashMap<>();
        int totalFiles = discoveredFiles.size();
        int processedFiles = 0;
        long lastMetadataProgress = System.currentTimeMillis();

        for (File file : discoveredFiles) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                addUsbTrackWithMetadata(file, folders, discoveredFiles.indexOf(file));
            } catch (Exception ignored) {
                try {
                    addUsbTrackBasic(file, folders);
                } catch (Exception e) {
                    // Skip malformed files
                }
            }
            processedFiles++;
            long now = System.currentTimeMillis();
            if (now - lastMetadataProgress > USB_SCAN_PROGRESS_INTERVAL_MS) {
                publishUsbScanProgress(processedFiles, totalFiles);
                lastMetadataProgress = now;
            }
        }

        try {
            JSONArray folderArray = new JSONArray();
            JSONArray trackArray = new JSONArray();
            for (UsbFolderBucket bucket : folders.values()) {
                if (bucket.tracks.length() == 0) {
                    continue;
                }
                JSONObject folder = new JSONObject();
                folder.put("path", bucket.path);
                folder.put("name", bucket.name);
                folder.put("thumbnail", "");
                folder.put("tracks", bucket.tracks);
                folderArray.put(folder);
                for (int i = 0; i < bucket.tracks.length(); i += 1) {
                    trackArray.put(bucket.tracks.getJSONObject(i));
                }
            }
            JSONObject result = new JSONObject();
            String label = primaryRoot.getName();
            result.put("connected", true);
            result.put("scanning", false);
            result.put("label", label);
            result.put("uuid", label);
            result.put("id", label + ":" + label);
            result.put("message", folderArray.length() > 0 ? getString(R.string.usb_msg_scan_complete) : getString(R.string.usb_msg_no_music));
            result.put("folders", folderArray);
            result.put("tracks", trackArray);
            return result.toString();
        } catch (Exception exception) {
            return createUsbErrorJson(getString(R.string.usb_msg_cannot_identify));
        }
    }

    private void discoverUsbAudioFiles(File root, List<File> discovered, int[] counters, long[] lastTime) {
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child == null || child.isHidden()) {
                    continue;
                }
                if (child.isDirectory()) {
                    stack.push(child);
                } else if (isSupportedUsbAudioFile(child)) {
                    discovered.add(child);
                    counters[0]++;
                    counters[1] = discovered.size();
                    long now = System.currentTimeMillis();
                    if (now - lastTime[0] > USB_SCAN_PROGRESS_INTERVAL_MS) {
                        publishUsbScanProgress(counters[1], counters[1]);
                        lastTime[0] = now;
                    }
                }
            }
        }
    }

    private void scanUsbRoot(File root, Map<String, UsbFolderBucket> folders) {
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child == null || child.isHidden()) {
                    continue;
                }
                if (child.isDirectory()) {
                    stack.push(child);
                } else if (isSupportedUsbAudioFile(child)) {
                    addUsbTrack(child, folders);
                }
            }
        }
    }

    private void scanUsbRootWithProgress(File root, Map<String, UsbFolderBucket> folders, int[] counters, long[] lastTime) {
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            File current = stack.pop();
            counters[0]++;
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child == null || child.isHidden()) {
                    continue;
                }
                if (child.isDirectory()) {
                    stack.push(child);
                } else if (isSupportedUsbAudioFile(child)) {
                    addUsbTrack(child, folders);
                    counters[1]++;
                    long now = System.currentTimeMillis();
                    if (now - lastTime[0] > 300) {
                        publishUsbScanProgress(counters[0], counters[1]);
                        lastTime[0] = now;
                    }
                }
            }
        }
    }

    private void publishUsbScanProgress(int scanned, int found) {
        try {
            JSONObject progress = new JSONObject();
            progress.put("type", "scan_progress");
            progress.put("scanned", scanned);
            progress.put("found", found);
            progress.put("total", Math.max(scanned, found));
            String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                    + JSONObject.quote(progress.toString())
                    + ");";
            musicWebView.post(() -> evaluatePlayerScript(script));
        } catch (Exception ignored) {
        }
    }

    private void publishUsbScanProgress(int scanned, int found, int total) {
        try {
            JSONObject progress = new JSONObject();
            progress.put("type", "scan_progress");
            progress.put("scanned", scanned);
            progress.put("found", found);
            progress.put("total", total);
            String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                    + JSONObject.quote(progress.toString())
                    + ");";
            musicWebView.post(() -> evaluatePlayerScript(script));
        } catch (Exception ignored) {
        }
    }

    private void addUsbTrack(File file, Map<String, UsbFolderBucket> folders) {
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        String folderPath = parent.getAbsolutePath();
        UsbFolderBucket bucket = folders.get(folderPath);
        if (bucket == null) {
            bucket = new UsbFolderBucket(folderPath, parent.getName());
            folders.put(folderPath, bucket);
        }
        try {
            JSONObject track = createUsbTrackJson(file, folderPath, bucket.name, bucket.tracks.length());
            bucket.tracks.put(track);
        } catch (Exception ignored) {
        }
    }

    private void addUsbTrackWithMetadata(File file, Map<String, UsbFolderBucket> folders, int index) {
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        String folderPath = parent.getAbsolutePath();
        UsbFolderBucket bucket = folders.get(folderPath);
        if (bucket == null) {
            bucket = new UsbFolderBucket(folderPath, parent.getName());
            folders.put(folderPath, bucket);
        }
        JSONObject track;
        try {
            track = createUsbTrackJson(file, folderPath, bucket.name, bucket.tracks.length());
        } catch (Exception e) {
            track = createBasicUsbTrackJson(file, folderPath, bucket.name, bucket.tracks.length());
        }
        bucket.tracks.put(track);
    }

    private void addUsbTrackBasic(File file, Map<String, UsbFolderBucket> folders) {
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        String folderPath = parent.getAbsolutePath();
        UsbFolderBucket bucket = folders.get(folderPath);
        if (bucket == null) {
            bucket = new UsbFolderBucket(folderPath, parent.getName());
            folders.put(folderPath, bucket);
        }
        JSONObject track = createBasicUsbTrackJson(file, folderPath, bucket.name, bucket.tracks.length());
        bucket.tracks.put(track);
    }

    private JSONObject createBasicUsbTrackJson(File file, String folderPath, String folderName, int index) {
        JSONObject track = new JSONObject();
        try {
            track.put("id", folderPath + "::" + index);
            track.put("title", stripExtension(file.getName()));
            track.put("artist", getString(R.string.usb_artist));
            track.put("album", "");
            track.put("fileName", file.getName());
            track.put("fileSize", file.length());
            track.put("path", file.getAbsolutePath());
            track.put("url", Uri.fromFile(file).toString());
            track.put("folderPath", folderPath);
            track.put("folderName", folderName);
            track.put("durationLabel", "--:--");
            track.put("coverUrl", "");
        } catch (Exception ignored) {
        }
        return track;
    }

    private JSONObject createUsbTrackJson(File file, String folderPath, String folderName, int index) throws Exception {
        JSONObject track = new JSONObject();
        String title = stripExtension(file.getName());
        String artist = getString(R.string.usb_artist);
        String album = "";
        String durationLabel = "--:--";
        String coverUrl = "";
        artist = getString(R.string.usb_artist);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String metadataTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String metadataArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String metadataAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String metadataDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (metadataTitle != null && metadataTitle.trim().length() > 0) {
                title = metadataTitle.trim();
            }
            if (metadataArtist != null && metadataArtist.trim().length() > 0) {
                artist = metadataArtist.trim();
            }
            if (metadataAlbum != null && metadataAlbum.trim().length() > 0) {
                album = metadataAlbum.trim();
            }
            if (metadataDuration != null && metadataDuration.trim().length() > 0) {
                try {
                    long durationMs = Long.parseLong(metadataDuration.trim());
                    durationLabel = formatDurationLabel(durationMs);
                } catch (NumberFormatException ignored) {
                }
            }
            byte[] embeddedPicture = retriever.getEmbeddedPicture();
            if (embeddedPicture != null && embeddedPicture.length > 0) {
                coverUrl = saveUsbCoverCache(file, embeddedPicture);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        track.put("id", folderPath + "::" + index);
        track.put("title", title);
        track.put("artist", artist);
        track.put("album", album);
        track.put("fileName", file.getName());
        track.put("fileSize", file.length());
        track.put("path", file.getAbsolutePath());
        track.put("url", Uri.fromFile(file).toString());
        track.put("folderPath", folderPath);
        track.put("folderName", folderName);
        track.put("durationLabel", durationLabel);
        track.put("coverUrl", coverUrl);
        return track;
    }

    private String saveUsbCoverCache(File sourceFile, byte[] pictureData) {
        try {
            File coverDir = new File(getCacheDir(), "usb_covers");
            if (!coverDir.exists() && !coverDir.mkdirs()) {
                return "";
            }
            String hash = Integer.toHexString(sourceFile.getAbsolutePath().hashCode());
            File coverFile = new File(coverDir, hash + ".jpg");
            if (coverFile.exists() && coverFile.length() > 0) {
                return Uri.fromFile(coverFile).toString();
            }
            // 先读取图片尺寸，计算采样率，避免直接解码超大封面导致内存占用过高
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, bounds);
            int maxEdge = Math.max(bounds.outWidth, bounds.outHeight);
            int inSampleSize = 1;
            while (maxEdge / inSampleSize > USB_COVER_MAX_EDGE_PX) {
                inSampleSize *= 2;
            }
            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = inSampleSize;
            Bitmap cover = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, decodeOpts);
            if (cover == null) {
                return "";
            }
            FileOutputStream fos = new FileOutputStream(coverFile);
            try {
                // 统一转成 JPEG 并压缩，减少缓存体积与加载时间
                cover.compress(Bitmap.CompressFormat.JPEG, USB_COVER_JPEG_QUALITY, fos);
                fos.flush();
            } finally {
                fos.close();
                cover.recycle();
            }
            return Uri.fromFile(coverFile).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String formatDurationLabel(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    private List<File> findUsbRoots() {
        List<File> roots = new ArrayList<>();
        List<String> seenPaths = new ArrayList<>();

        // 方式一：使用 StorageManager API 获取系统识别的可移动存储卷（主要方式）
        if (storageManager != null) {
            try {
                List<StorageVolume> volumes = storageManager.getStorageVolumes();
                if (volumes != null) {
                    for (StorageVolume volume : volumes) {
                        try {
                            // 跳过内置存储（emulated），只关注物理可移动存储
                            if (volume.isEmulated()) {
                                continue;
                            }
                            String state = volume.getState();
                            // 只处理已挂载或可移除的卷
                            if (!Environment.MEDIA_MOUNTED.equals(state)
                                    && !Environment.MEDIA_REMOVED.equals(state)) {
                                continue;
                            }
                            File directory = getStorageVolumeDirectory(volume);
                            if (directory != null && directory.isDirectory() && directory.canRead()) {
                                String path = directory.getAbsolutePath();
                                if (!seenPaths.contains(path)) {
                                    seenPaths.add(path);
                                    roots.add(directory);
                                    Log.d(TAG, "USB检测[StorageManager]: volume=" + volume.getDescription(this)
                                            + " path=" + path + " state=" + state);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "USB检测[StorageManager] volume遍历异常: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "USB检测[StorageManager] 获取存储卷失败: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "USB检测[StorageManager] storageManager为null，回退到路径扫描");
        }

        // 方式二：回退方案，扫描硬编码路径候选列表
        if (roots.isEmpty()) {
            String[] usbPathCandidates = {
                    "/storage", "/mnt/usb", "/storage/usb", "/storage/usbdisk0",
                    "/storage/usbdisk1", "/storage/usb0", "/storage/usb1",
                    "/media/usb0", "/media/usb1", "/mnt/usb0", "/mnt/usb1",
                    "/mnt/media_rw/usb0", "/mnt/media_rw/usb1",
                    "/mnt/runtime/default/usb"
            };
            for (String candidatePath : usbPathCandidates) {
                File dir = new File(candidatePath);
                if (!dir.isDirectory() || !dir.canRead()) {
                    continue;
                }
                String path = dir.getAbsolutePath();
                if (seenPaths.contains(path)) {
                    continue;
                }
                seenPaths.add(path);
                if ("/storage".equals(candidatePath)) {
                    File[] children = dir.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            if (child == null || !child.isDirectory() || !child.canRead()) {
                                continue;
                            }
                            String name = child.getName();
                            if ("emulated".equals(name) || "self".equals(name)) {
                                continue;
                            }
                            String childPath = child.getAbsolutePath();
                            if (!seenPaths.contains(childPath)) {
                                seenPaths.add(childPath);
                                roots.add(child);
                            }
                        }
                    }
                } else {
                    roots.add(dir);
                }
            }
            // 二次扫描 /storage 目录，确保无遗漏
            File storage = new File("/storage");
            File[] storageChildren = storage.listFiles();
            if (storageChildren != null) {
                for (File candidate : storageChildren) {
                    if (candidate == null || !candidate.isDirectory() || !candidate.canRead()) {
                        continue;
                    }
                    String name = candidate.getName();
                    if ("emulated".equals(name) || "self".equals(name)) {
                        continue;
                    }
                    String path = candidate.getAbsolutePath();
                    if (!seenPaths.contains(path)) {
                        seenPaths.add(path);
                        roots.add(candidate);
                    }
                }
            }
            if (!roots.isEmpty()) {
                Log.d(TAG, "USB检测[路径回退]: 找到 " + roots.size() + " 个路径");
            }
        }

        return roots;
    }

    /**
     * 通过反射获取 StorageVolume 的目录路径
     * API 30+ 可直接使用 getDirectory()，低版本通过反射获取挂载点
     */
    private File getStorageVolumeDirectory(StorageVolume volume) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 (API 30+)：StorageVolume.getDirectory() 直接返回目录
                return volume.getDirectory();
            }
            // 低版本：通过反射获取挂载点路径
            Method getPathMethod = StorageVolume.class.getMethod("getPath");
            String path = (String) getPathMethod.invoke(volume);
            if (path != null && !path.isEmpty()) {
                return new File(path);
            }
        } catch (Exception e) {
            Log.w(TAG, "USB检测 StorageVolume路径获取失败: " + e.getMessage());
        }
        return null;
    }

    private boolean isSupportedUsbAudioFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        long length = file.length();
        if (length <= USB_MIN_AUDIO_FILE_BYTES) {
            return false;
        }
        String name = file.getName().toLowerCase();
        boolean extensionMatch = false;
        for (String extension : USB_AUDIO_EXTENSIONS) {
            if (name.endsWith(extension)) {
                extensionMatch = true;
                break;
            }
        }
        if (!extensionMatch) {
            return false;
        }
        return hasValidAudioSignature(file);
    }

    private boolean hasValidAudioSignature(File file) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] header = new byte[16];
            int read = fis.read(header);
            if (read < 4) {
                return false;
            }
            String fileName = file.getName().toLowerCase();

            // MP3（含 ID3v1/v2 标签）
            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                return true;
            }
            // WAV / AIFF / OGG（RIFF 变体）
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
                if (read >= 12) {
                    String format = new String(header, 8, 4, "US-ASCII");
                    return "WAVE".equals(format) || "AIFF".equals(format) || "OGGS".equals(format);
                }
                return true;
            }
            // FLAC
            if (header[0] == 'f' && header[1] == 'L' && header[2] == 'a' && header[3] == 'C') {
                return true;
            }
            // MP4 容器（M4A/M4B/AAC-LC/Opus）：ftyp
            if (read >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
                return true;
            }
            // OGG（Vorbis/Opus）
            if (header[0] == 'O' && header[1] == 'g' && header[2] == 'g') {
                return true;
            }
            // APE（Monkey's Audio）：魔数 "MAC "
            if (header[0] == 'M' && header[1] == 'A' && header[2] == 'C' && header[3] == ' ') {
                return true;
            }
            // WMA（ASF 容器）GUID 头：30 26 B2 75 8E 66 CF 11 A6 D9 00 AA 00 62 CE 6C
            if (read >= 8
                    && (header[0] & 0xFF) == 0x30 && (header[1] & 0xFF) == 0x26
                    && (header[2] & 0xFF) == 0xB2 && (header[3] & 0xFF) == 0x75
                    && (header[4] & 0xFF) == 0x8E && (header[5] & 0xFF) == 0x66
                    && (header[6] & 0xFF) == 0xCF && (header[7] & 0xFF) == 0x11) {
                return true;
            }
            // 压缩包（ZIP/JAR 等）明确拒绝
            if (header[0] == 'P' && header[1] == 'K') {
                return false;
            }
            // AAC 裸流（ADTS）：12 位同步字 0xFFF
            if (fileName.endsWith(".aac")) {
                return (header[0] & 0xFF) == 0xFF && (header[1] & 0xF0) == 0xF0;
            }
            // MPEG 音频 L1/L2/L3（MP3/MP2/MP1/MPEG/MPG）：11 位同步字 0xFFE
            if (fileName.endsWith(".mp3") || fileName.endsWith(".mp2") || fileName.endsWith(".mp1")
                    || fileName.endsWith(".mpeg") || fileName.endsWith(".mpg")) {
                return (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
            }
            // 其余白名单扩展名（DSF/DFF/TTA/TAK/WV/MID 等）：仅长度兜底
            return read >= 4;
        } catch (Exception e) {
            return false;
        }
    }

    private String stripExtension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : normalizePlaybackText(name, "未知歌曲");
    }

    private String createUsbScanningJson() {
        try {
            JSONObject result = new JSONObject();
            result.put("connected", true);
            result.put("scanning", true);
            result.put("message", getString(R.string.usb_msg_reading));
            result.put("folders", new JSONArray());
            result.put("tracks", new JSONArray());
            return result.toString();
        } catch (Exception ignored) {
            return "{\"connected\":true,\"scanning\":true,\"message\":\"USB reading\",\"folders\":[],\"tracks\":[]}";
        }
    }

    private String createUsbDisconnectedJson(String message) {
        try {
            JSONObject result = new JSONObject();
            result.put("connected", false);
            result.put("scanning", false);
            result.put("message", message);
            result.put("folders", new JSONArray());
            result.put("tracks", new JSONArray());
            return result.toString();
        } catch (Exception ignored) {
            return "{\"connected\":false,\"scanning\":false,\"message\":\"USB device not connected\",\"folders\":[],\"tracks\":[]}";
        }
    }

    private String createUsbErrorJson(String message) {
        try {
            JSONObject result = new JSONObject();
            result.put("connected", false);
            result.put("scanning", false);
            result.put("message", message);
            result.put("folders", new JSONArray());
            result.put("tracks", new JSONArray());
            return result.toString();
        } catch (Exception ignored) {
            return createUsbDisconnectedJson(getString(R.string.usb_msg_cannot_identify));
        }
    }

    private int getUsbTrackCount(String state) {
        try {
            JSONArray tracks = new JSONObject(state).optJSONArray("tracks");
            return tracks == null ? 0 : tracks.length();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean isUsbStateConnected() {
        try {
            return new JSONObject(usbMusicStateJson).optBoolean("connected", false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void publishUsbEvent(String type, String message) {
        if (musicWebView == null) {
            return;
        }
        try {
            JSONObject event = new JSONObject();
            event.put("type", type);
            event.put("message", message);
            String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                    + JSONObject.quote(event.toString())
                    + ");";
            musicWebView.post(() -> musicWebView.evaluateJavascript(script, null));
        } catch (Exception ignored) {
        }
    }

    private void loadPersistedFavorites() {
        if (favoritesLoaded) {
            return;
        }
        favoritesLoaded = true;
        File indexFile = getFavoritesIndexFile();
        if (indexFile == null || !indexFile.exists()) {
            return;
        }
        try (java.io.FileInputStream fis = new java.io.FileInputStream(indexFile);
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject fav = array.getJSONObject(i);
                String key = fav.optString("key", "");
                if (!key.isEmpty()) {
                    persistedUsbFavorites.put(key, fav);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void persistFavorites() {
        try {
            File dir = getOrCreateFavoritesDir();
            if (dir == null) {
                return;
            }
            File indexFile = new File(dir, USB_FAVORITES_INDEX_FILE);
            JSONArray array = new JSONArray();
            for (JSONObject fav : persistedUsbFavorites.values()) {
                array.put(fav);
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(indexFile)) {
                fos.write(array.toString().getBytes("UTF-8"));
                fos.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private File getOrCreateFavoritesDir() {
        File dir = new File(getExternalFilesDir(null), USB_FAVORITES_SYNC_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            dir = new File(getFilesDir(), USB_FAVORITES_SYNC_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
        }
        return dir;
    }

    private File getFavoritesIndexFile() {
        File dir = getOrCreateFavoritesDir();
        return dir == null ? null : new File(dir, USB_FAVORITES_INDEX_FILE);
    }

    private void syncFavoritesAfterScan(String stateJson) {
        loadPersistedFavorites();
        if (persistedUsbFavorites.isEmpty()) {
            return;
        }
        try {
            JSONObject state = new JSONObject(stateJson);
            JSONArray tracks = state.optJSONArray("tracks");
            if (tracks == null) {
                return;
            }
            JSONArray matchedFavorites = new JSONArray();
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject track = tracks.getJSONObject(i);
                String key = buildFavoriteKey(
                        track.optString("artist", ""),
                        track.optString("title", "")
                );
                if (persistedUsbFavorites.containsKey(key)) {
                    matchedFavorites.put(track.put("favoriteKey", key));
                }
            }
            JSONObject syncResult = new JSONObject();
            syncResult.put("type", "favorites_synced");
            syncResult.put("matchedCount", matchedFavorites.length());
            syncResult.put("favorites", matchedFavorites);
            String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                    + JSONObject.quote(syncResult.toString())
                    + ");";
            musicWebView.post(() -> musicWebView.evaluateJavascript(script, null));
        } catch (Exception ignored) {
        }
    }

    private String buildFavoriteKey(String artist, String title) {
        return (artist == null ? "" : artist) + "::" + (title == null ? "" : title);
    }

    private synchronized void addFavoriteToStorage(
            String trackPath, String artist, String title,
            String album, String fileSize, String durationLabel, String coverUrl
    ) {
        loadPersistedFavorites();
        String key = buildFavoriteKey(artist, title);
        JSONObject fav = new JSONObject();
        try {
            fav.put("key", key);
            fav.put("trackPath", trackPath == null ? "" : trackPath);
            fav.put("artist", artist == null ? "" : artist);
            fav.put("title", title == null ? "" : title);
            fav.put("album", album == null ? "" : album);
            fav.put("fileSize", fileSize == null ? "0" : fileSize);
            fav.put("durationLabel", durationLabel == null ? "--:--" : durationLabel);
            fav.put("coverUrl", coverUrl == null ? "" : coverUrl);
            fav.put("addedAt", System.currentTimeMillis());
            persistedUsbFavorites.put(key, fav);
            persistFavorites();
            copyFileToFavoritesDir(trackPath, key);
        } catch (Exception ignored) {
        }
    }

    private synchronized boolean removeFavoriteFromStorage(String artist, String title) {
        loadPersistedFavorites();
        String key = buildFavoriteKey(artist, title);
        JSONObject removed = persistedUsbFavorites.remove(key);
        if (removed != null) {
            persistFavorites();
            String trackPath = removed.optString("trackPath", "");
            deleteFileFromFavoritesDir(key);
            if (!trackPath.isEmpty()) {
                File src = new File(trackPath);
                if (src.exists()) {
                    File dest = getFavoriteCopyFile(key);
                    if (dest.exists()) {
                        dest.delete();
                    }
                }
            }
            return true;
        }
        return false;
    }

    private JSONArray getPersistedFavoritesAsJson() {
        loadPersistedFavorites();
        JSONArray result = new JSONArray();
        for (JSONObject fav : persistedUsbFavorites.values()) {
            result.put(fav);
        }
        return result;
    }

    private void copyFileToFavoritesDir(String trackPath, String key) {
        if (trackPath == null || trackPath.isEmpty()) {
            return;
        }
        File src = new File(trackPath);
        if (!src.isFile() || !src.canRead()) {
            return;
        }
        try {
            File dest = getFavoriteCopyFile(key);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (dest.exists() && dest.length() == src.length()) {
                return;
            }
            try (java.io.FileInputStream fis = new java.io.FileInputStream(src);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteFileFromFavoritesDir(String key) {
        try {
            File dest = getFavoriteCopyFile(key);
            if (dest.exists()) {
                dest.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private File getFavoriteCopyFile(String key) {
        File dir = getOrCreateFavoritesDir();
        if (dir == null) {
            return new File(getCacheDir(), "usb_fav_" + key.hashCode() + ".tmp");
        }
        String safeKey = key.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return new File(dir, safeKey + ".audio");
    }

    private class UsbFolderBucket {
        final String path;
        final String name;
        final JSONArray tracks = new JSONArray();

        UsbFolderBucket(String path, String name) {
            this.path = path;
            this.name = name == null || name.trim().isEmpty() ? getString(R.string.usb_root_dir_name) : name;
        }
    }

    /**
     * JS-Native桥接内部类，暴露给WebView中JavaScript调用的原生接口
     * 所有@JavascriptInterface方法均可在Web页面中通过window.MusicBridge.xxx()调用
     */
    private class MusicBridge {
        @JavascriptInterface // 设置状态栏主题（深色/浅色）
        public void setStatusBarTheme(String theme) {
            boolean lightBackground = "light".equals(theme) || "light-background".equals(theme);
            runOnUiThread(() -> applyStatusBarTheme(lightBackground));
        }

        @JavascriptInterface // 获取USB音乐当前状态JSON
        public String getUsbMusicState() {
            return usbMusicStateJson;
        }

        @JavascriptInterface // 触发USB音乐扫描
        public String scanUsbMusic() {
            startUsbScanAsync();
            return usbMusicStateJson;
        }

        @JavascriptInterface // 获取蓝牙当前状态JSON
        public String getBluetoothState() {
            return bluetoothStateJson();
        }

        @JavascriptInterface // 刷新蓝牙状态（重新获取Profile代理、检查已连接设备）
        public String refreshBluetoothState() {
            ensureA2dpProxy();
            checkSystemConnectedBluetoothDevices();
            publishBluetoothPlaybackState();
            return bluetoothStateJson();
        }

        @JavascriptInterface // 获取蓝牙状态详情（含连接指标和播放状态）
        public String getBluetoothStatusDetail() {
            JSONObject result = new JSONObject();
            try {
                result.put("state", bluetoothStateJson());
                result.put("metrics", bluetoothConnectionMetricsJson());
                result.put("playback", bluetoothPlaybackStateJson());
            } catch (Exception ignored) {
                return "{\"error\":\"状态详情获取失败\"}";
            }
            return result.toString();
        }

        @JavascriptInterface // 清除蓝牙错误状态
        public String clearBluetoothErrorState() {
            clearBluetoothError();
            setBluetoothConnectionState(BT_STATE_IDLE);
            return statusJson(true, "错误状态已清除");
        }

        @JavascriptInterface // 取消蓝牙自动重连
        public String cancelBluetoothReconnect() {
            clearAllBluetoothAutoReconnect();
            return statusJson(true, "已取消自动重连");
        }

        @JavascriptInterface // 获取蓝牙连接指标（成功率、耗时等）
        public String getBluetoothConnectionMetrics() {
            return bluetoothConnectionMetricsJson();
        }

        @JavascriptInterface // 获取所有已知蓝牙设备列表
        public String getBluetoothDevices() {
            JSONArray result = new JSONArray();
            try {
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || !hasBluetoothConnectPermission()) {
                    return result.toString();
                }

                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    rememberBluetoothDevice(device);
                }
                refreshSystemBluetoothConnectionState();
                rememberConnectedA2dpDevices();
                rememberConnectedA2dpSinkDevices();
                for (BluetoothDevice device : knownBluetoothDevices.values()) {
                    result.put(createBluetoothDeviceJson(device));
                }
            } catch (Exception ignored) {
                return "[]";
            }
            return result.toString();
        }

        @JavascriptInterface // 获取已配对蓝牙设备列表
        public String getPairedBluetoothDevices() {
            return getBluetoothDevices();
        }

        @JavascriptInterface // 开始蓝牙设备搜索
        public String startBluetoothDiscovery() {
            return startBluetoothDiscoveryInternal();
        }

        @JavascriptInterface // 配对指定蓝牙设备
        public String pairBluetoothDevice(String address) {
            BluetoothDevice device = getRemoteDevice(address);
            if (device == null) {
                return statusJson(false, "\u8bbe\u5907\u5730\u5740\u65e0\u6548");
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f\uff0c\u8bf7\u5148\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5f00\u542f\u84dd\u7259");
            }
            if (!hasBluetoothConnectPermission()) {
                return statusJson(false, "\u7f3a\u5c11\u84dd\u7259\u8fde\u63a5\u6743\u9650");
            }
            try {
                rememberBluetoothDevice(device);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    return statusJson(true, "\u8bbe\u5907\u5df2\u5b8c\u6210\u914d\u5bf9");
                }
                boolean started = device.createBond();
                return statusJson(started, started ? "\u5df2\u53d1\u8d77\u7cfb\u7edf\u914d\u5bf9\uff0c\u8bf7\u5728\u5f39\u7a97\u4e2d\u786e\u8ba4" : "\u914d\u5bf9\u53d1\u8d77\u5931\u8d25");
            } catch (SecurityException exception) {
                return statusJson(false, "\u84dd\u7259\u914d\u5bf9\u6743\u9650\u88ab\u7cfb\u7edf\u62d2\u7edd");
            }
        }

        @JavascriptInterface // 连接指定蓝牙设备
        public String connectBluetoothDevice(String address) {
            BluetoothDevice device = getRemoteDevice(address);
            if (device == null) {
                return statusJson(false, "\u8bbe\u5907\u5730\u5740\u65e0\u6548");
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f\uff0c\u8bf7\u5148\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5f00\u542f\u84dd\u7259");
            }
            if (!hasBluetoothConnectPermission()) {
                return statusJson(false, "\u7f3a\u5c11\u84dd\u7259\u8fde\u63a5\u6743\u9650");
            }
            if (isA2dpConnected(device) || isA2dpSinkConnected(device)) {
                confirmedBluetoothAudioAddress = device.getAddress();
                rememberBluetoothControlTarget(device);
                prepareBluetoothMusicRoute();
                String message = completeBluetoothConnectSuccess(device, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5df2\u8fde\u63a5");
                publishBluetoothEvent(message);
                publishBluetoothPlaybackState();
                return statusJson(true, message);
            }
            openBluetoothSettings();
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u70b9\u51fb\u8bbe\u5907\u5f00\u542f\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        }

        @JavascriptInterface // 断开指定蓝牙设备
        public String disconnectBluetoothDevice(String address) {
            BluetoothDevice device = getRemoteDevice(address);
            if (device == null) {
                return statusJson(false, "\u8bbe\u5907\u5730\u5740\u65e0\u6548");
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f");
            }
            userInitiatedBluetoothDisconnect = true;
            clearAllBluetoothAutoReconnect();
            clearBluetoothControlTarget(device);
            if (device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                confirmedBluetoothAudioAddress = "";
                markBluetoothPlaybackDisconnected();
            }
            setBluetoothConnectionState(BT_STATE_DISCONNECTED);
            openBluetoothSettings();
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u5173\u95ed\u8bbe\u7f6e\u7684\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        }

        @JavascriptInterface // 断开所有蓝牙设备
        public String disconnectAllBluetoothDevices() {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f");
            }
            if (!hasBluetoothConnectPermission()) {
                return statusJson(false, "\u7f3a\u5c11\u84dd\u7259\u8fde\u63a5\u6743\u9650");
            }
            userInitiatedBluetoothDisconnect = true;
            clearAllBluetoothAutoReconnect();
            requestedBluetoothControlAddress = "";
            confirmedBluetoothAudioAddress = "";
            markBluetoothPlaybackDisconnected();
            setBluetoothConnectionState(BT_STATE_DISCONNECTED);
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u5173\u95ed\u8bbe\u5907\u7684\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        }

        @JavascriptInterface // 发送蓝牙媒体控制命令（play/pause/next/prev等）
        public String sendBluetoothMediaCommand(String command) {
            if (getConnectedA2dpDeviceCount() <= 0) {
                return statusJson(false, "\u84dd\u7259\u97f3\u9891\u8bbe\u5907\u5c1a\u672a\u7a33\u5b9a\u8fde\u63a5\uff0c\u8bf7\u5148\u5b8c\u6210\u8fde\u63a5\u540e\u518d\u64ad\u653e");
            }
            boolean sent = false;
            if ("play".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            } else if ("pause".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            } else if ("next".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            } else if ("previous".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            } else if ("fastForward".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
            } else if ("rewind".equals(command)) {
                sent = sendMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND);
            } else {
                return statusJson(false, "未知蓝牙媒体控制指令");
            }
            if (!sent) {
                return statusJson(false, "\u84dd\u7259\u5a92\u4f53\u63a7\u5236\u901a\u9053\u4e0d\u53ef\u7528\uff0c\u8bf7\u786e\u8ba4\u624b\u673a\u5df2\u8fde\u63a5\u5a92\u4f53\u97f3\u9891\u5e76\u91cd\u8fde");
            }
            return statusJson(true, "已发送蓝牙媒体控制指令：" + command);
        }

        @JavascriptInterface // 获取蓝牙播放状态（播放/暂停/曲目信息等）
        public String getBluetoothPlaybackState() {
            return bluetoothPlaybackStateJson();
        }

        @JavascriptInterface // 设置蓝牙媒体音量
        public String setBluetoothVolume(float volume) {
            return setMediaStreamVolume(volume);
        }

        @JavascriptInterface // 设置媒体音量
        public String setMediaVolume(float volume) {
            return setMediaStreamVolume(volume);
        }

        @JavascriptInterface // 更新本地播放通知（曲目信息和播放状态）
        public void updateLocalPlaybackState(String title, String artist, boolean playing) {
            updateLocalPlaybackNotification(title, artist, playing);
        }

        @JavascriptInterface // 准备蓝牙音频路由
        public String prepareBluetoothAudioRoute() {
            prepareBluetoothMusicRoute();
            int connectedCount = getConnectedA2dpDeviceCount();
            return statusJson(connectedCount > 0,
                    connectedCount > 0
                            ? "\u84dd\u7259\u626c\u58f0\u5668\u8def\u7531\u5df2\u51c6\u5907"
                            : "\u672a\u68c0\u6d4b\u5230\u5df2\u8fde\u63a5\u7684\u84dd\u7259\u97f3\u9891\u8bbe\u5907");
        }
        @JavascriptInterface // 设置当前活跃的音频模块（local/bluetooth）
        public void setActiveAudioModule(String module) {
            activeAudioModule = module == null ? "" : module;
            if ("bluetooth".equals(module)) {
                prepareBluetoothMusicRoute();
            } else {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            }
        }

        @JavascriptInterface // 获取离线收音机能力状态检测结果
        public String getOfflineRadioState() {
            JSONObject result = new JSONObject();
            try {
                boolean possibleFmFeature =
                        getPackageManager().hasSystemFeature("android.hardware.radio.fm")
                                || getPackageManager().hasSystemFeature("android.hardware.broadcastradio");
                result.put("available", false);
                result.put("hardwareFeatureHint", possibleFmFeature);
                result.put("message",
                        possibleFmFeature
                                ? "\u68c0\u6d4b\u5230\u53ef\u80fd\u7684FM\u786c\u4ef6\u6807\u8bb0\uff0c\u4f46\u5f53\u524dROM\u672a\u5411\u5e94\u7528\u5f00\u653e\u6807\u51c6\u79bb\u7ebfFM\u63a7\u5236\u63a5\u53e3"
                                : "\u5f53\u524d\u8bbe\u5907\u672a\u5411\u5e94\u7528\u5f00\u653e\u79bb\u7ebfFM\u6536\u97f3\u673a\u786c\u4ef6\u63a5\u53e3");
            } catch (Exception ignored) {
                return "{\"available\":false,\"message\":\"\u79bb\u7ebfFM\u80fd\u529b\u68c0\u6d4b\u5931\u8d25\"}";
            }
            return result.toString();
        }

        @JavascriptInterface // 扫描离线FM广播电台（当前系统不支持，返回空列表）
        public String scanOfflineRadioStations(String band) {
            JSONObject result = new JSONObject();
            try {
                result.put("ok", false);
                result.put("stations", new JSONArray());
                result.put("message", "\u5f53\u524dAndroid\u7cfb\u7edf\u672a\u63d0\u4f9b\u53ef\u7528\u7684\u79bb\u7ebfFM\u626b\u53f0API\uff0c\u65e0\u6cd5\u5728\u65e0\u7f51\u7edc\u65f6\u641c\u7d22\u771f\u5b9e\u8c03\u9891\u9891\u9053");
            } catch (Exception ignored) {
                return "{\"ok\":false,\"stations\":[],\"message\":\"\u79bb\u7ebfFM\u626b\u63cf\u5931\u8d25\"}";
            }
            return result.toString();
        }

        @JavascriptInterface // 启动离线FM播放（当前系统不支持）
        public String startOfflineRadio(String band, double frequency, float volume) {
            JSONObject result = new JSONObject();
            try {
                result.put("ok", false);
                result.put("message", "\u5f53\u524d\u8bbe\u5907/ROM\u672a\u5411\u5e94\u7528\u5f00\u653e\u79bb\u7ebfFM\u64ad\u653e\u63a5\u53e3\uff0c\u5df2\u963b\u6b62\u6a21\u62df\u566a\u58f0\u64ad\u653e");
            } catch (Exception ignored) {
                return "{\"ok\":false,\"message\":\"\u79bb\u7ebfFM\u542f\u52a8\u5931\u8d25\"}";
            }
            return result.toString();
        }

        @JavascriptInterface // 停止离线FM播放（预留接口）
        public void stopOfflineRadio() {
            // Reserved for devices or ROM builds that expose a real FM HAL bridge.
        }

        @JavascriptInterface // 设置离线FM音量（预留接口）
        public void setOfflineRadioVolume(float volume) {
            // Reserved for devices or ROM builds that expose a real FM HAL bridge.
        }

        @JavascriptInterface // 打开系统蓝牙设置页面
        public void openBluetoothSettings() {
            MainActivity.this.openBluetoothSettings();
        }

        @JavascriptInterface // 获取本地持久化的USB收藏列表
        public String getPersistedFavorites() {
            loadPersistedFavorites();
            return getPersistedFavoritesAsJson().toString();
        }

        @JavascriptInterface // 添加收藏到本地存储
        public String addFavoriteTrack(
                String trackPath, String artist, String title,
                String album, String fileSize, String durationLabel, String coverUrl
        ) {
            try {
                addFavoriteToStorage(trackPath, artist, title, album, fileSize, durationLabel, coverUrl);
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("key", buildFavoriteKey(artist, title));
                return result.toString();
            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface // 从本地存储中移除收藏
        public String removeFavoriteTrack(String artist, String title) {
            try {
                boolean removed = removeFavoriteFromStorage(artist, title);
                JSONObject result = new JSONObject();
                result.put("success", removed);
                return result.toString();
            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface // 同步USB扫描结果与本地收藏
        public String syncFavoritesWithUsb() {
            String state = usbMusicStateJson;
            new Thread(() -> syncFavoritesAfterScan(state), "UsbFavSync").start();
            return "{\"success\":true}";
        }

        @JavascriptInterface // 启动系统车载收音机Activity（通过标准显式Intent，支持嵌入式bounds）
        public String launchSystemRadio(int left, int top, int right, int bottom) {
            return MainActivity.this.launchCarRadioActivity(left, top, right, bottom);
        }

        @JavascriptInterface // 将应用MainActivity拉回前台（用于切换离开收音机模块时覆盖收音机Activity）
        public String bringAppToFront() {
            return MainActivity.this.bringMainActivityToFront();
        }

        @JavascriptInterface // 显示悬浮tab栏（覆盖在系统收音机Activity之上，保持tab栏可见可点击）
        public void showRadioTabOverlay(int left, int top, int right, int bottom,
                                        int lightTheme, String labelBt, String labelRadio, String labelUsb) {
            final String[] labels = {labelBt, labelRadio, labelUsb};
            runOnUiThread(() -> MainActivity.this.showRadioTabOverlay(
                    left, top, right, bottom, lightTheme == 1, labels));
        }

        @JavascriptInterface // 隐藏悬浮tab栏
        public void hideRadioTabOverlay() {
            runOnUiThread(MainActivity.this::hideRadioTabOverlay);
        }

        // =============================================
        // USB 扫描调试接口（开发测试用，发布时可移除）
        // =============================================

        @JavascriptInterface // 测试USB扫描：直接在后台线程中模拟扫描流程，返回JSON结果
        public String testUsbScan() {
            Log.d(TAG, "[USB测试] 开始模拟USB扫描...");

            // 检查是否存在可用于测试的真实文件目录
            File testRoot = null;
            String[] candidates = {
                    "/sdcard/Music",
                    "/mnt/sdcard/Music",
                    "/storage/emulated/0/Music",
                    "/sdcard/Download"
            };
            for (String path : candidates) {
                File dir = new File(path);
                if (dir.isDirectory() && dir.canRead() && dir.listFiles() != null && dir.listFiles().length > 0) {
                    // 检查是否有音频文件
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && isSupportedUsbAudioFile(f)) {
                                testRoot = dir;
                                break;
                            }
                        }
                    }
                    if (testRoot != null) break;
                }
            }

            if (testRoot != null) {
                // 真实文件扫描：找到测试目录，使用真实文件扫描
                Log.d(TAG, "[USB测试] 发现测试目录: " + testRoot.getAbsolutePath() + "，使用真实文件扫描");
                final File scanRoot = testRoot;
                final int scanToken = ++usbScanToken;
                usbScanning = true;

                // 异步执行真实扫描
                new Thread(() -> {
                    String state = scanUsbMusicNow();
                    if (scanToken != usbScanToken) return;
                    usbScanning = false;
                    usbMusicStateJson = state;

                    // 推送结果给前端
                    try {
                        JSONObject event = new JSONObject();
                        event.put("type", "scan_completed");
                        event.put("message", getString(R.string.usb_msg_scan_complete));
                        String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                                + JSONObject.quote(event.toString()) + ");";
                        musicWebView.post(() -> evaluatePlayerScript(script));
                    } catch (Exception e) {
                        Log.e(TAG, "[USB测试] 推送结果失败", e);
                    }
                    Log.d(TAG, "[USB测试] 真实扫描完成，结果已推送");
                }, "UsbTestScanner").start();

                return "{\"success\":true, \"message\":\"已在 " + testRoot.getAbsolutePath() + " 目录启动真实扫描\"}";
            } else {
                // 使用模拟数据：构造完整的扫描结果JSON
                Log.d(TAG, "[USB测试] 未找到测试目录，使用模拟数据");
                try {
                    JSONObject result = new JSONObject();
                    result.put("connected", true);
                    result.put("scanning", false);
                    result.put("label", "TEST_USB");
                    result.put("uuid", "TEST_USB");
                    result.put("id", "TEST_USB:TEST_USB");
                    result.put("message", "扫描完成（模拟数据），共发现 6 首音乐");

                    // 构造文件夹数据
                    JSONArray folders = new JSONArray();

                    // Folder 1: Rock
                    JSONObject rockFolder = new JSONObject();
                    rockFolder.put("path", "/mnt/usb/TEST_USB/Rock");
                    rockFolder.put("name", "Rock");
                    rockFolder.put("thumbnail", "🎸");
                    JSONArray rockTracks = new JSONArray();
                    rockTracks.put(createMockTrack("track_001", "Bohemian Rhapsody", "Queen",
                            "A Night at the Opera", "mp3", 5242880, 354000, "5:54",
                            "/mnt/usb/TEST_USB/Rock"));
                    rockTracks.put(createMockTrack("track_002", "Hotel California", "Eagles",
                            "Hotel California", "flac", 31457280, 391000, "6:31",
                            "/mnt/usb/TEST_USB/Rock"));
                    rockFolder.put("tracks", rockTracks);
                    folders.put(rockFolder);

                    // Folder 2: Pop
                    JSONObject popFolder = new JSONObject();
                    popFolder.put("path", "/mnt/usb/TEST_USB/Pop");
                    popFolder.put("name", "Pop Hits 2024");
                    popFolder.put("thumbnail", "🎵");
                    JSONArray popTracks = new JSONArray();
                    popTracks.put(createMockTrack("track_003", "Blinding Lights", "The Weeknd",
                            "After Hours", "aac", 3145728, 200000, "3:20",
                            "/mnt/usb/TEST_USB/Pop"));
                    popTracks.put(createMockTrack("track_004", "Shape of You", "Ed Sheeran",
                            "Divide", "m4a", 4194304, 233000, "3:53",
                            "/mnt/usb/TEST_USB/Pop"));
                    popFolder.put("tracks", popTracks);
                    folders.put(popFolder);

                    // Folder 3: Jazz
                    JSONObject jazzFolder = new JSONObject();
                    jazzFolder.put("path", "/mnt/usb/TEST_USB/Jazz");
                    jazzFolder.put("name", "Jazz Classics");
                    jazzFolder.put("thumbnail", "🎷");
                    JSONArray jazzTracks = new JSONArray();
                    jazzTracks.put(createMockTrack("track_005", "Take Five", "Dave Brubeck",
                            "Time Out", "wav", 47185920, 324000, "5:24",
                            "/mnt/usb/TEST_USB/Jazz"));
                    jazzTracks.put(createMockTrack("track_006", "So What", "Miles Davis",
                            "Kind of Blue", "ogg", 5242880, 481000, "8:01",
                            "/mnt/usb/TEST_USB/Jazz"));
                    jazzFolder.put("tracks", jazzTracks);
                    folders.put(jazzFolder);

                    result.put("folders", folders);

                    // 构造完整tracks列表
                    JSONArray allTracks = new JSONArray();
                    for (int i = 0; i < folders.length(); i++) {
                        JSONObject folder = folders.getJSONObject(i);
                        JSONArray tracks = folder.getJSONArray("tracks");
                        for (int j = 0; j < tracks.length(); j++) {
                            allTracks.put(tracks.getJSONObject(j));
                        }
                    }
                    result.put("tracks", allTracks);

                    // 保存结果并推送到前端
                    usbMusicStateJson = result.toString();
                    usbScanning = false;

                    // 构造scan_completed事件推送到前端
                    JSONObject event = new JSONObject();
                    event.put("type", "scan_completed");
                    event.put("message", "扫描完成（模拟数据），共发现 " + allTracks.length() + " 首音乐");
                    final String eventJson = event.toString();
                    musicWebView.post(() -> {
                        String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                                + JSONObject.quote(eventJson) + ");";
                        evaluatePlayerScript(script);
                    });

                    Log.d(TAG, "[USB测试] 模拟数据已生成并推送到前端，共 " + allTracks.length() + " 首歌曲");
                    return "{\"success\":true, \"message\":\"模拟USB扫描完成，已推送 " + allTracks.length() + " 首歌曲到前端\"}";

                } catch (Exception e) {
                    Log.e(TAG, "[USB测试] 构造模拟数据失败", e);
                    return "{\"success\":false, \"message\":\"构造模拟数据失败: " + e.getMessage() + "\"}";
                }
            }
        }

        @JavascriptInterface // 清除测试USB数据
        public String clearTestUsbData() {
            // 发送disconnected事件给前端
            try {
                JSONObject event = new JSONObject();
                event.put("type", "disconnected");
                event.put("message", "模拟U盘已拔出");
                String script = "window.onNativeUsbEvent&&window.onNativeUsbEvent("
                        + JSONObject.quote(event.toString()) + ");";
                musicWebView.post(() -> evaluatePlayerScript(script));
            } catch (Exception ignored) {
            }
            return "已清除测试USB数据";
        }

        @JavascriptInterface // 音乐律动氛围灯RGBA上报（Web端FFT分析后调用，频率→颜色，能量→亮度）
        public void setAmbientLightRGBA(int r, int g, int b, int a) {
            try {
                // 钳制到合法范围 0~255，避免越界值导致硬件异常
                int cr = Math.max(0, Math.min(255, r));
                int cg = Math.max(0, Math.min(255, g));
                int cb = Math.max(0, Math.min(255, b));
                int ca = Math.max(0, Math.min(255, a));
                Log.d(TAG, "音乐律动氛围灯RGBA: (" + cr + "," + cg + "," + cb + "," + ca + ")");
                // TODO: 在此接入车端氛围灯硬件控制（如 VHAL 厂商扩展信号 / 系统氛围灯 Service），
                //       将 RGBA 写入实际硬件。当前仅保留日志便于联调验证，调用失败不抛异常。
            } catch (Throwable t) {
                Log.e(TAG, "设置氛围灯RGBA异常", t);
            }
        }

        /**
         * 辅助方法：创建模拟音轨JSON对象
         */
        private JSONObject createMockTrack(String id, String title, String artist,
                                            String album, String ext, long fileSize,
                                            int durationMs, String durationLabel,
                                            String folderPath) {
            try {
                JSONObject track = new JSONObject();
                track.put("id", id);
                track.put("title", title);
                track.put("artist", artist);
                track.put("album", album);
                track.put("path", folderPath + "/" + title + "." + ext);
                track.put("fileName", title + "." + ext);
                track.put("fileSize", fileSize);
                track.put("duration", durationMs);
                track.put("durationLabel", durationLabel);
                track.put("folderPath", folderPath);
                track.put("folderName", new File(folderPath).getName());
                return track;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (musicWebView != null && musicWebView.canGoBack()) {
                    musicWebView.goBack();
                    return;
                }
                if (localPlaybackPlaying) {
                    moveTaskToBack(true);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * 监听电话状态变化：
     * 来电/通话时通知WebView暂停音乐，通话结束后通知恢复
     */
    private void configurePhoneStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return;
        }
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String number) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (!phoneCallActive) {
                            phoneCallActive = true;
                            evaluatePlayerScript("window.onNativePhoneCallStart&&window.onNativePhoneCallStart();");
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (phoneCallActive) {
                            phoneCallActive = false;
                            evaluatePlayerScript("window.onNativePhoneCallEnd&&window.onNativePhoneCallEnd();");
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (SecurityException ignored) {
        }
    }

    private void requestPhoneStatePermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        } catch (Exception ignored) {
        }
    }

    /**
     * 释放所有资源：
     * 取消蓝牙发现、关闭Profile代理、注销所有广播接收器、销毁WebView、释放电话监听
     */
    @Override
    protected void onDestroy() {
        hideBluetoothBackOverlay();
        hideRadioTabOverlay(); // 清理悬浮tab栏
        if (telephonyManager != null && phoneStateListener != null) {
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception ignored) {
            }
            phoneStateListener = null;
            telephonyManager = null;
        }
        if (bluetoothAdapter != null) {
            try {
                if (hasBluetoothScanPermission() && bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException ignored) {
            }
            if (bluetoothA2dp != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
                bluetoothA2dp = null;
            }
            if (bluetoothA2dpSink != null) {
                bluetoothAdapter.closeProfileProxy(PROFILE_A2DP_SINK, bluetoothA2dpSink);
                bluetoothA2dpSink = null;
            }
            if (bluetoothAvrcpController != null) {
                bluetoothAdapter.closeProfileProxy(PROFILE_AVRCP_CONTROLLER, bluetoothAvrcpController);
                bluetoothAvrcpController = null;
            }
        }
        stopBluetoothConnectionStatusChecker();
        stopVhalKeySignalPolling();
        if (bluetoothReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothReceiverRegistered = false;
        }
        if (playbackControlReceiverRegistered) {
            unregisterReceiver(playbackControlReceiver);
            playbackControlReceiverRegistered = false;
        }
        if (usbReceiverRegistered) {
            unregisterReceiver(usbReceiver);
            usbReceiverRegistered = false;
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (musicWebView != null) {
            musicWebView.destroy();
        }
        super.onDestroy();
    }
}
