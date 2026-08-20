package com.app.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * 音乐播放后台服务
 * 功能：
 *  1. 前台服务，显示常驻通知（含上一曲/播放暂停/下一曲按钮）
 *  2. 接收MainActivity发来的播放状态更新（标题、艺术家、播放状态）
 *  3. 通过广播接收通知栏按钮的点击事件，实现远程控制
 *  4. 保证应用在后台时音乐持续播放不被系统回收
 */
public class MusicPlaybackService extends Service {
    // Action常量：MainActivity通过此Action发送播放状态更新
    public static final String ACTION_UPDATE = "com.app.music.action.UPDATE_PLAYBACK";
    // Action常量：通知栏按钮通过此Action发送控制命令
    public static final String ACTION_CONTROL = "com.app.music.action.CONTROL_PLAYBACK";
    // 通知Intent附加字段
    public static final String EXTRA_TITLE = "title";       // 歌曲标题
    public static final String EXTRA_ARTIST = "artist";     // 艺术家名
    public static final String EXTRA_PLAYING = "playing";   // 播放状态
    public static final String EXTRA_COMMAND = "command";    // 控制命令（play/pause/previous/next）

    // 通知渠道ID（Android O+前台服务必须）
    private static final String CHANNEL_ID = "sanyi_music_playback";
    // 通知唯一ID
    private static final int NOTIFICATION_ID = 1001;

    // 当前播放状态
    private String title;
    private String artist;
    private boolean playing = false;

    /** 创建时初始化通知渠道 */
    @Override
    public void onCreate() {
        super.onCreate();
        title = getString(R.string.default_title);
        artist = getString(R.string.default_artist);
        ensureNotificationChannel();
    }

    /**
     * 服务启动入口
     * 接收MainActivity的播放状态更新（ACTION_UPDATE），然后构建前台通知
     * 使用START_STICKY确保服务被系统回收后自动重建
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE.equals(intent.getAction())) {
            title = normalize(intent.getStringExtra(EXTRA_TITLE), getString(R.string.default_title));
            artist = normalize(intent.getStringExtra(EXTRA_ARTIST), getString(R.string.default_artist));
            playing = intent.getBooleanExtra(EXTRA_PLAYING, false);
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    /** 绑定模式不支持，返回null */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 构建前台通知
     * 包含：小图标、歌曲信息、上一曲/播放暂停/下一曲三个操作按钮
     * 点击通知跳转回MainActivity
     */
    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                pendingIntentFlags()
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_sanyi_music)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(contentIntent)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(createAction(getString(R.string.notification_previous), "previous"))
                .addAction(createAction(playing ? getString(R.string.notification_pause) : getString(R.string.notification_play), playing ? "pause" : "play"))
                .addAction(createAction(getString(R.string.notification_next), "next"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        return builder.build();
    }

    /**
     * 创建通知操作按钮
     * @param label 按钮显示文字（如"上一曲"、"暂停"、"下一曲"）
     * @param command 控制命令标识（previous/play/pause/next）
     * @return 构建的Notification.Action
     */
    private Notification.Action createAction(String label, String command) {
        Intent intent = new Intent(ACTION_CONTROL);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_COMMAND, command);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                command.hashCode(),
                intent,
                pendingIntentFlags()
        );
        return new Notification.Action.Builder(R.drawable.ic_sanyi_music, label, pendingIntent).build();
    }

    /**
     * 确保通知渠道已创建（Android O+要求）
     * 仅在首次调用时创建，使用低重要性级别避免打断用户
     */
    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.playback_channel_desc));
        manager.createNotificationChannel(channel);
    }

    /** 适配不同Android版本的PendingIntent标志位 */
    private static int pendingIntentFlags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    /** 字符串工具：若为null或空字符串则返回默认值 */
    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
