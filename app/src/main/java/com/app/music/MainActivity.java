package com.app.music;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.graphics.Color;
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
import android.provider.Settings;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String PLAYER_URL = "file:///android_asset/player/index.html";
    private static final int PROFILE_A2DP_SINK = 11;
    private static final int PROFILE_AVRCP_CONTROLLER = 12;
    private static final int AVRCP_PASS_THROUGH_STATE_PRESS = 0;
    private static final int AVRCP_PASS_THROUGH_STATE_RELEASE = 1;
    private static final int AVRCP_CMD_ID_REWIND = 0x48;
    private static final int AVRCP_CMD_ID_FAST_FORWARD = 0x49;
    private static final int AVRCP_CMD_ID_FORWARD = 0x4B;
    private static final int AVRCP_CMD_ID_BACKWARD = 0x4C;
    private static final int AVRCP_CMD_ID_PLAY = 0x44;
    private static final int AVRCP_CMD_ID_PAUSE = 0x46;
    private static final int A2DP_SINK_STATE_PLAYING = 10;
    private static final int A2DP_SINK_STATE_NOT_PLAYING = 11;
    private static final int DEFAULT_STATUS_BAR_TOP_PX = 24;
    private static final String ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    private static final String ACTION_A2DP_SINK_PLAYING_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED";
    private static final String ACTION_AVRCP_CONTROLLER_PLAYBACK_STATE_CHANGED =
            "android.bluetooth.avrcp-controller.profile.action.PLAYBACK_STATE_CHANGED";
    private static final String ACTION_AVRCP_CONTROLLER_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    private static final String EXTRA_AVRCP_CONTROLLER_PLAYBACK =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";
    private static final String EXTRA_AVRCP_CONTROLLER_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    private static final String ACTION_AVRCP_PLAYBACK_STATE_CHANGED =
            "android.bluetooth.avrcp.profile.action.PLAYBACK_STATE_CHANGED";
    private static final String ACTION_AVRCP_TRACK_EVENT =
            "android.bluetooth.avrcp.profile.action.TRACK_EVENT";
    private static final String ACTION_VOLUME_CHANGED =
            "android.media.VOLUME_CHANGED_ACTION";
    private static final long USB_MIN_AUDIO_FILE_BYTES = 100L * 1024L;
    private static final long BLUETOOTH_FAST_CONNECT_TIMEOUT_MS = 3000L;
    private static final long BLUETOOTH_CONNECT_CONFIRM_TIMEOUT_MS = 12000L;
    private static final long BLUETOOTH_FAST_CONNECT_RETRY_MS = 500L;
    private static final long BLUETOOTH_AUTO_RECONNECT_DELAY_MS = 2000L;
    private static final int BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS = 8;
    private static final long BLUETOOTH_STABLE_SESSION_TARGET_MS = 2L * 60L * 60L * 1000L;
    private static final int BLUETOOTH_CONNECT_SAMPLE_LIMIT = 60;
    private static final long BLUETOOTH_DISCOVERY_MIN_DURATION_MS = 8000L;
    private static final long BLUETOOTH_DISCOVERY_MAX_DURATION_MS = 15000L;
    private static final long BLUETOOTH_DEVICE_EXPIRY_MS = 60000L;
    private static final long BLUETOOTH_CONNECTION_STATUS_CHECK_INTERVAL_MS = 3000L;
    private static final long BLUETOOTH_RECONNECT_BACKOFF_BASE_MS = 1000L;
    private static final String[] USB_AUDIO_EXTENSIONS = {
            ".aac", ".mp3", ".flac", ".ape", ".wav", ".wma", ".ogg", ".mpeg", ".mpg", ".mp2", ".mp1", ".m4a"
    };

    private WebView musicWebView;
    @Nullable
    private ValueCallback<Uri[]> filePathCallback;
    @Nullable
    private GeolocationPermissions.Callback pendingGeolocationCallback;
    @Nullable
    private String pendingGeolocationOrigin;
    @Nullable
    private BluetoothAdapter bluetoothAdapter;
    @Nullable
    private BluetoothA2dp bluetoothA2dp;
    @Nullable
    private BluetoothProfile bluetoothA2dpSink;
    @Nullable
    private BluetoothProfile bluetoothAvrcpController;
    private final Map<String, BluetoothDevice> knownBluetoothDevices = new LinkedHashMap<>();
    private final Map<String, String> bluetoothDeviceNames = new LinkedHashMap<>();
    private final Map<String, Integer> bluetoothDeviceRssi = new LinkedHashMap<>();
    private final Map<String, Long> bluetoothDeviceLastSeen = new LinkedHashMap<>();
    private final Handler bluetoothHandler = new Handler(Looper.getMainLooper());
    private boolean bluetoothReceiverRegistered = false;
    private String activeAudioModule = "local";
    private String pendingBluetoothConnectAddress = "";
    private String confirmedBluetoothAudioAddress = "";
    private String requestedBluetoothControlAddress = "";
    private long bluetoothRemoteProgressMs = -1L;
    private long bluetoothRemoteDurationMs = -1L;
    private long bluetoothRemoteProgressUpdatedAtMs = 0L;
    private boolean bluetoothRemotePlayingKnown = false;
    private boolean bluetoothRemotePlaying = false;
    private String bluetoothRemoteTitle = "";
    private String bluetoothRemoteArtist = "";
    private String bluetoothRemoteAlbum = "";
    private int statusBarTopPx = DEFAULT_STATUS_BAR_TOP_PX;
    private boolean statusBarLightBackground = false;
    private boolean playbackControlReceiverRegistered = false;
    private boolean usbReceiverRegistered = false;
    private volatile boolean usbScanning = false;
    private volatile String usbMusicStateJson = "{\"connected\":false,\"scanning\":false,\"message\":\"USB设备未连接\",\"folders\":[],\"tracks\":[]}";
    private int usbScanToken = 0;
    private boolean localPlaybackPlaying = false;
    private String localPlaybackTitle = "三一音乐";
    private String localPlaybackArtist = "本地音乐";
    private int pendingBluetoothConnectAttempts = 0;
    private long pendingBluetoothConnectStartedAtMs = 0L;
    private long lastBluetoothConnectDurationMs = -1L;
    private long confirmedBluetoothConnectedAtMs = 0L;
    private int bluetoothConnectSuccessCount = 0;
    private int bluetoothConnectFailureCount = 0;
    private int bluetoothDisconnectCount = 0;
    private long bluetoothTotalConnectedDurationMs = 0L;
    private String lastBluetoothConnectAddress = "";
    private String lastBluetoothConnectResult = "idle";
    private String bluetoothAutoReconnectAddress = "";
    private int bluetoothAutoReconnectAttempts = 0;
    private boolean userInitiatedBluetoothDisconnect = false;
    private boolean pendingBluetoothDiscoveryAfterPermission = false;
    private boolean bluetoothDiscoveryPending = false;
    private long bluetoothDiscoveryStartedAtMs = 0L;
    private boolean bluetoothScanningForAudioDevicesOnly = true;
    private final JSONArray bluetoothConnectSamples = new JSONArray();
    private final Runnable bluetoothConnectionStatusCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkBluetoothConnectionStatus();
            bluetoothHandler.postDelayed(this, BLUETOOTH_CONNECTION_STATUS_CHECK_INTERVAL_MS);
        }
    };
    private final AudioManager.OnAudioFocusChangeListener bluetoothAudioFocusListener = focusChange -> {
        if ("bluetooth".equals(activeAudioModule) && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            publishBluetoothEvent("\u84dd\u7259\u72b6\u6001\u5df2\u66f4\u65b0");
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? "" : intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_CHECKING.equals(action)
                    || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)
                    || Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
                if (!usbScanning) {
                    publishUsbEvent("connected", "USB\u8bbe\u5907\u5df2\u8fde\u63a5");
                    startUsbScanAsync();
                }
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_REMOVED.equals(action)
                    || Intent.ACTION_MEDIA_EJECT.equals(action)
                    || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)
                    || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                usbScanToken += 1;
                usbScanning = false;
                usbMusicStateJson = createUsbDisconnectedJson("USB \u8bbe\u5907\u5df2\u65ad\u5f00");
                publishUsbEvent("disconnected", "USB \u8bbe\u5907\u5df2\u65ad\u5f00");
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

    private final BroadcastReceiver playbackControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !MusicPlaybackService.ACTION_CONTROL.equals(intent.getAction())) {
            }
            String command = intent.getStringExtra(MusicPlaybackService.EXTRA_COMMAND);
            dispatchLocalPlaybackCommand(command == null ? "" : command);
        }
    };

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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        musicWebView = findViewById(R.id.musicWebView);
        applyStatusBarTheme(false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarTopPx = systemBars.top;
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            injectSafeAreaCssVariables();
            return insets;
        });
        configureWebView();
        configureBackNavigation();
        configureBluetooth();
        registerUsbReceiver();
        registerPlaybackControlReceiver();
        requestAudioPermissionIfNeeded();
        musicWebView.loadUrl(PLAYER_URL);
        if (!findUsbRoots().isEmpty()) {
            startUsbScanAsync();
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
        checkSystemConnectedBluetoothDevices();
        evaluatePlayerScript("window.onNativeAppResume&&window.onNativeAppResume();");
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothRuntimePermissionsIfNeeded(false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
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

    private void configureBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ensureA2dpProxy();
        bluetoothHandler.postDelayed(this::checkSystemConnectedBluetoothDevices, 2000);

        IntentFilter filter = new IntentFilter();
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
        addBluetoothConnectSample("failed", lastBluetoothConnectAddress, elapsed, reason);
        if (device != null
                && !userInitiatedBluetoothDisconnect
                && device.getAddress().equals(bluetoothAutoReconnectAddress)
                && bluetoothAutoReconnectAttempts < BLUETOOTH_AUTO_RECONNECT_MAX_ATTEMPTS) {
            scheduleBluetoothAutoReconnect(device, reason);
        }
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
        } else if (state == A2DP_SINK_STATE_NOT_PLAYING || state == BluetoothProfile.STATE_DISCONNECTED) {
            bluetoothRemotePlayingKnown = true;
            bluetoothRemotePlaying = false;
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
        if (duration > 0L) {
            bluetoothRemoteDurationMs = duration;
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

    private void checkBluetoothConnectionStatus() {
        if (confirmedBluetoothAudioAddress.length() == 0) {
            return;
        }
        BluetoothDevice device = getRemoteDevice(confirmedBluetoothAudioAddress);
        if (device == null) {
            return;
        }
        boolean stillConnected = isA2dpConnected(device) || isA2dpSinkConnected(device);
        if (!stillConnected) {
            if (confirmedBluetoothConnectedAtMs > 0L && System.currentTimeMillis() - confirmedBluetoothConnectedAtMs > 3000L) {
                recordBluetoothDisconnect(device);
                if (!userInitiatedBluetoothDisconnect) {
                    scheduleBluetoothAutoReconnect(device, "连接状态检查发现断开");
                }
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
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && "bluetooth".equals(activeAudioModule)) {
            int keyCode = event.getKeyCode();
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
        }
        return super.dispatchKeyEvent(event);
    }

    private void updateLocalPlaybackNotification(String title, String artist, boolean playing) {
        boolean hasTrack = title != null && title.trim().length() > 0;
        if (!hasTrack && !playing) {
            localPlaybackPlaying = false;
            stopService(new Intent(this, MusicPlaybackService.class));
            return;
        }
        localPlaybackTitle = normalizePlaybackText(title, "三一音乐");
        localPlaybackArtist = normalizePlaybackText(artist, "本地音乐");
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
        String script = "document.documentElement.style.setProperty('--native-status-bar-top','"
                + safeTop
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
        publishUsbEvent("scan_started", "USB \u8bfb\u53d6\u4e2d");
        new Thread(() -> {
            String state = scanUsbMusicNow();
            if (token != usbScanToken) {
                return;
            }
            usbScanning = false;
            usbMusicStateJson = state;
            publishUsbEvent("scan_complete", getUsbTrackCount(state) > 0
                    ? "USB音乐扫描完成"
                    : "USB设备中无音乐文件...");
            publishUsbEvent("scan_complete", getUsbTrackCount(state) > 0
                    ? "USB\u97f3\u4e50\u626b\u63cf\u5b8c\u6210"
                    : "USB\u8bbe\u5907\u4e2d\u65e0\u97f3\u4e50\u6587\u4ef6");
        }, "UsbMusicScanner").start();
    }

    private String scanUsbMusicNow() {
        List<File> roots = findUsbRoots();
        if (roots.isEmpty()) {
            return createUsbDisconnectedJson("USB\u8bbe\u5907\u672a\u8fde\u63a5");
        }
        File primaryRoot = roots.get(0);
        Map<String, UsbFolderBucket> folders = new LinkedHashMap<>();
        for (File root : roots) {
            scanUsbRootWithProgress(root, folders, new int[]{0, 0}, new long[]{0});
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
            result.put("message", folderArray.length() > 0 ? "USB扫描完成" : "USB设备中无音乐文件...");
            result.put("message", folderArray.length() > 0 ? "USB\u626b\u63cf\u5b8c\u6210" : "USB\u8bbe\u5907\u4e2d\u65e0\u97f3\u4e50\u6587\u4ef6");
            result.put("folders", folderArray);
            result.put("tracks", trackArray);
            return result.toString();
        } catch (Exception exception) {
            return createUsbErrorJson("\u65e0\u6cd5\u8bc6\u522b\u6b64\u8bbe\u5907");
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
            String script = "window.onUsbEvent&&window.onUsbEvent(" + progress.toString() + ");";
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
            // Ignore malformed files so one bad item does not block the whole USB scan.
        }
    }

    private JSONObject createUsbTrackJson(File file, String folderPath, String folderName, int index) throws Exception {
        JSONObject track = new JSONObject();
        String title = stripExtension(file.getName());
        String artist = "USB音乐";
        String album = "";
        artist = "USB\u97f3\u4e50";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String metadataTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String metadataArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String metadataAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (metadataTitle != null && metadataTitle.trim().length() > 0) {
                title = metadataTitle.trim();
            }
            if (metadataArtist != null && metadataArtist.trim().length() > 0) {
                artist = metadataArtist.trim();
            }
            if (metadataAlbum != null && metadataAlbum.trim().length() > 0) {
                album = metadataAlbum.trim();
            }
        } catch (Exception ignored) {
            // Filename fallback keeps large scans fast and resilient.
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
        return track;
    }

    private List<File> findUsbRoots() {
        List<File> roots = new ArrayList<>();
        File storage = new File("/storage");
        File[] candidates = storage.listFiles();
        if (candidates == null) {
            return roots;
        }
        for (File candidate : candidates) {
            if (candidate == null || !candidate.isDirectory() || !candidate.canRead()) {
                continue;
            }
            String name = candidate.getName();
            if ("emulated".equals(name) || "self".equals(name)) {
                continue;
            }
            roots.add(candidate);
        }
        return roots;
    }

    private boolean isSupportedUsbAudioFile(File file) {
        if (file.length() <= USB_MIN_AUDIO_FILE_BYTES) {
            return false;
        }
        String name = file.getName().toLowerCase();
        for (String extension : USB_AUDIO_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
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
            result.put("message", "USB \u8bfb\u53d6\u4e2d");
            result.put("folders", new JSONArray());
            result.put("tracks", new JSONArray());
            return result.toString();
        } catch (Exception ignored) {
            return "{\"connected\":true,\"scanning\":true,\"message\":\"USB 读取中\",\"folders\":[],\"tracks\":[]}";
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
            return "{\"connected\":false,\"scanning\":false,\"message\":\"USB设备未连接\",\"folders\":[],\"tracks\":[]}";
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
            return createUsbDisconnectedJson("\u65e0\u6cd5\u8bc6\u522b\u6b64\u8bbe\u5907");
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

    private static class UsbFolderBucket {
        final String path;
        final String name;
        final JSONArray tracks = new JSONArray();

        UsbFolderBucket(String path, String name) {
            this.path = path;
            this.name = name == null || name.trim().isEmpty() ? "\u6839\u76ee\u5f55" : name;
        }
    }

    private class MusicBridge {
        @JavascriptInterface
        public void setStatusBarTheme(String theme) {
            boolean lightBackground = "light".equals(theme) || "light-background".equals(theme);
            runOnUiThread(() -> applyStatusBarTheme(lightBackground));
        }

        @JavascriptInterface
        public String getUsbMusicState() {
            return usbMusicStateJson;
        }

        @JavascriptInterface
        public String scanUsbMusic() {
            startUsbScanAsync();
            return usbMusicStateJson;
        }

        @JavascriptInterface
        public String getBluetoothState() {
            return bluetoothStateJson();
        }

        @JavascriptInterface
        public String getBluetoothConnectionMetrics() {
            return bluetoothConnectionMetricsJson();
        }

        @JavascriptInterface
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

        @JavascriptInterface
        public String getPairedBluetoothDevices() {
            return getBluetoothDevices();
        }

        @JavascriptInterface
        public String startBluetoothDiscovery() {
            return startBluetoothDiscoveryInternal();
        }

        @JavascriptInterface
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

        @JavascriptInterface
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

        @JavascriptInterface
        public String disconnectBluetoothDevice(String address) {
            BluetoothDevice device = getRemoteDevice(address);
            if (device == null) {
                return statusJson(false, "\u8bbe\u5907\u5730\u5740\u65e0\u6548");
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f");
            }
            userInitiatedBluetoothDisconnect = true;
            bluetoothAutoReconnectAddress = "";
            bluetoothAutoReconnectAttempts = 0;
            clearBluetoothControlTarget(device);
            if (device.getAddress().equals(confirmedBluetoothAudioAddress)) {
                confirmedBluetoothAudioAddress = "";
                markBluetoothPlaybackDisconnected();
            }
            openBluetoothSettings();
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u5173\u95ed\u8bbe\u7f6e\u7684\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        }

        @JavascriptInterface
        public String disconnectAllBluetoothDevices() {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return statusJson(false, "\u84dd\u7259\u672a\u5f00\u542f");
            }
            if (!hasBluetoothConnectPermission()) {
                return statusJson(false, "\u7f3a\u5c11\u84dd\u7259\u8fde\u63a5\u6743\u9650");
            }
            userInitiatedBluetoothDisconnect = true;
            bluetoothAutoReconnectAddress = "";
            bluetoothAutoReconnectAttempts = 0;
            requestedBluetoothControlAddress = "";
            return statusJson(true, "\u8bf7\u5728\u7cfb\u7edf\u84dd\u7259\u8bbe\u7f6e\u4e2d\u5173\u95ed\u8bbe\u5907\u7684\"\u5a92\u4f53\u97f3\u9891\"\u5f00\u5173");
        }

        @JavascriptInterface
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

        @JavascriptInterface
        public String getBluetoothPlaybackState() {
            return bluetoothPlaybackStateJson();
        }

        @JavascriptInterface
        public String setBluetoothVolume(float volume) {
            return setMediaStreamVolume(volume);
        }

        @JavascriptInterface
        public String setMediaVolume(float volume) {
            return setMediaStreamVolume(volume);
        }

        @JavascriptInterface
        public void updateLocalPlaybackState(String title, String artist, boolean playing) {
            updateLocalPlaybackNotification(title, artist, playing);
        }

        @JavascriptInterface
        public String prepareBluetoothAudioRoute() {
            prepareBluetoothMusicRoute();
            int connectedCount = getConnectedA2dpDeviceCount();
            return statusJson(connectedCount > 0,
                    connectedCount > 0
                            ? "\u84dd\u7259\u626c\u58f0\u5668\u8def\u7531\u5df2\u51c6\u5907"
                            : "\u672a\u68c0\u6d4b\u5230\u5df2\u8fde\u63a5\u7684\u84dd\u7259\u97f3\u9891\u8bbe\u5907");
        }
        @JavascriptInterface
        public void setActiveAudioModule(String module) {
            activeAudioModule = module == null ? "" : module;
            if ("bluetooth".equals(module)) {
                prepareBluetoothMusicRoute();
            } else {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            }
        }

        @JavascriptInterface
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

        @JavascriptInterface
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

        @JavascriptInterface
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

        @JavascriptInterface
        public void stopOfflineRadio() {
            // Reserved for devices or ROM builds that expose a real FM HAL bridge.
        }

        @JavascriptInterface
        public void setOfflineRadioVolume(float volume) {
            // Reserved for devices or ROM builds that expose a real FM HAL bridge.
        }

        @JavascriptInterface
        public void openBluetoothSettings() {
            MainActivity.this.openBluetoothSettings();
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

    @Override
    protected void onDestroy() {
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
