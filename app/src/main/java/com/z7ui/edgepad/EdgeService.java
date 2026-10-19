package com.z7ui.edgepad;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 前台服务：把左右两条手柄挂到窗口上，并跟着配置变化重建。 */
public class EdgeService extends Service {

    public static final String ACTION_RELOAD = "com.z7ui.edgepad.RELOAD";
    private static final String CH = "edge_service";

    private WindowManager wm;
    private final Map<String, HandleView> handles = new LinkedHashMap<>();

    private final Consumer<String> fgListener = pkg -> updateVisibility();

    public static void refresh(Context c) {
        Intent i = new Intent(c, EdgeService.class).setAction(ACTION_RELOAD);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void stop(Context c) {
        c.stopService(new Intent(c, EdgeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForeground(1, notification());
        EdgeA11yService.setForegroundListener(fgListener);
        rebuild();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rebuild();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EdgeA11yService.setForegroundListener(null);
        removeAll();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------- 手柄 ----------------

    private void rebuild() {
        removeAll();
        if (wm == null) return;
        if (!Settings.canDrawOverlays(this)) return;
        add(Config.LEFT);
        add(Config.RIGHT);
        updateVisibility();
        updateNotificationText();
    }

    private void add(String side) {
        if (!Config.sideOn(this, side)) return;
        try {
            HandleView v = new HandleView(this, side);
            wm.addView(v, params(side));
            handles.put(side, v);
        } catch (Throwable ignored) {
        }
    }

    private WindowManager.LayoutParams params(String side) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        int w = Math.max(1, dp(Config.widthDp(this)));
        int h = Math.max(1, m.heightPixels * Config.heightPct(this) / 100);
        int y = (m.heightPixels - h) / 2 + m.heightPixels * Config.offsetPct(this) / 100;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w, h, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        p.gravity = (Config.RIGHT.equals(side) ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP;
        p.x = dp(Config.insetDp(this));
        p.y = y;
        p.setTitle("EdgePad-" + side);
        if (Build.VERSION.SDK_INT >= 30) {
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams
                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        return p;
    }

    private void updateVisibility() {
        boolean hide = Config.excepted(this, EdgeA11yService.foreground());
        for (HandleView v : handles.values()) {
            v.setVisibility(hide ? View.GONE : View.VISIBLE);
        }
    }

    private void removeAll() {
        for (HandleView v : handles.values()) {
            try {
                wm.removeView(v);
            } catch (Throwable ignored) {
            }
        }
        handles.clear();
    }

    // ---------------- 通知 ----------------

    private Notification notification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CH, "侧边手柄",
                        NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("边触在运行")
                .setContentText(text())
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private String text() {
        boolean l = handles.containsKey(Config.LEFT);
        boolean r = handles.containsKey(Config.RIGHT);
        if (l && r) return "左右手柄都开着";
        if (l) return "左手柄开着";
        if (r) return "右手柄开着";
        return "没有启用的手柄";
    }

    private void updateNotificationText() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1, notification());
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
