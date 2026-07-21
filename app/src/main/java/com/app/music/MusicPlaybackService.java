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

public class MusicPlaybackService extends Service {
    public static final String ACTION_UPDATE = "com.app.music.action.UPDATE_PLAYBACK";
    public static final String ACTION_CONTROL = "com.app.music.action.CONTROL_PLAYBACK";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_PLAYING = "playing";
    public static final String EXTRA_COMMAND = "command";

    private static final String CHANNEL_ID = "sanyi_music_playback";
    private static final int NOTIFICATION_ID = 1001;

    private String title = "三一音乐";
    private String artist = "本地音乐";
    private boolean playing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE.equals(intent.getAction())) {
            title = normalize(intent.getStringExtra(EXTRA_TITLE), "三一音乐");
            artist = normalize(intent.getStringExtra(EXTRA_ARTIST), "本地音乐");
            playing = intent.getBooleanExtra(EXTRA_PLAYING, false);
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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
                .addAction(createAction("上一曲", "previous"))
                .addAction(createAction(playing ? "暂停" : "播放", playing ? "pause" : "play"))
                .addAction(createAction("下一曲", "next"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        return builder.build();
    }

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
                "三一音乐播放",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("本地音乐后台播放控制");
        manager.createNotificationChannel(channel);
    }

    private static int pendingIntentFlags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
