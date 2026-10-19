package com.z7ui.edgepad;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;

/**
 * 动作执行。全部同步、不联网，几十毫秒内完成。
 * 需要无障碍的那几个走 EdgeA11yService，其余走系统 API。
 */
public class Actions {

    private static final String CH_RESULT = "edge_result";

    public static boolean run(Context c, String value) {
        String key = Config.key(value);
        String arg = Config.arg(value);
        try {
            switch (key) {
                case "none":
                    return true;
                case "back":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                case "home":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                case "recents":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
                case "notifications":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                case "quick_settings":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
                case "power_dialog":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                case "lock_screen":
                    return global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                case "screenshot":
                    EdgeA11yService.screenshot(c);
                    return EdgeA11yService.connected();
                case "torch":
                    return torch(c);
                case "volume_up":
                    return volume(c, AudioManager.ADJUST_RAISE);
                case "volume_down":
                    return volume(c, AudioManager.ADJUST_LOWER);
                case "mute":
                    return mute(c);
                case "media_play":
                    return mediaKey(c, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                case "media_next":
                    return mediaKey(c, KeyEvent.KEYCODE_MEDIA_NEXT);
                case "media_prev":
                    return mediaKey(c, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                case "open_app":
                    return openApp(c, arg);
                case "previous_app":
                    return previousApp(c);
                case "split_screen":
                    return split(c);
                case "search":
                    return start(c, new Intent(Intent.ACTION_WEB_SEARCH)
                            .putExtra(SearchManager.QUERY, ""));
                case "assist":
                    return start(c, new Intent(Intent.ACTION_ASSIST));
                case "system_settings":
                    return start(c, new Intent(Settings.ACTION_SETTINGS));
                default:
                    notifyResult(c, "没绑定动作", key);
                    return false;
            }
        } catch (Throwable t) {
            notifyResult(c, "动作失败", key + "：" + t);
            return false;
        }
    }

    private static boolean global(int action) {
        return EdgeA11yService.global(action);
    }

    private static boolean start(Context c, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
        return true;
    }

    private static boolean openApp(Context c, String arg) {
        if (arg.isEmpty()) {
            notifyResult(c, "没选应用", "这个手势绑定的是「打开应用」，但还没选具体应用");
            return false;
        }
        String pkg = ActionCatalog.openAppPackage("open_app:" + arg);
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            notifyResult(c, "打不开", "找不到 " + pkg);
            return false;
        }
        return start(c, i);
    }

    private static boolean previousApp(Context c) {
        String pkg = EdgeA11yService.previousPackage();
        if (pkg.isEmpty()) {
            notifyResult(c, "没有上一个应用", "还没切换过应用");
            return false;
        }
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            notifyResult(c, "打不开", pkg);
            return false;
        }
        return start(c, i);
    }

    private static boolean split(Context c) {
        String pkg = EdgeA11yService.previousPackage();
        if (pkg.isEmpty()) pkg = EdgeA11yService.foreground();
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            notifyResult(c, "分屏失败", "没有可并排的应用");
            return false;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        // 分屏模式常量不在公开 SDK 里，只能靠 LAUNCH_ADJACENT 让系统自己排；
        // 部分 ROM 会退化成普通启动，界面上已经写明「尽力而为」
        c.startActivity(i);
        return true;
    }

    private static boolean volume(Context c, int direction) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction,
                AudioManager.FLAG_SHOW_UI);
        return true;
    }

    private static boolean mute(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (cur > 0) {
            Config.p(c).edit().putInt("saved_volume", cur).apply();
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
        } else {
            int back = Config.p(c).getInt("saved_volume", Math.max(1, max / 2));
            am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.min(back, max),
                    AudioManager.FLAG_SHOW_UI);
        }
        return true;
    }

    private static boolean mediaKey(Context c, int code) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        return true;
    }

    private static boolean torch(Context c) {
        CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) return false;
        try {
            String target = null;
            for (String id : cm.getCameraIdList()) {
                Boolean has = cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (has != null && has) {
                    target = id;
                    break;
                }
            }
            if (target == null) {
                notifyResult(c, "没有闪光灯", "");
                return false;
            }
            boolean on = !Config.torchOn(c);
            cm.setTorchMode(target, on);
            Config.setTorchOn(c, on);
            return true;
        } catch (Throwable t) {
            notifyResult(c, "手电筒失败", String.valueOf(t));
            return false;
        }
    }

    // ---------------- 通知 ----------------

    public static void notifyResult(Context c, String title, String text) {
        try {
            NotificationManager nm = (NotificationManager)
                    c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CH_RESULT, "动作结果",
                        NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
            Intent i = new Intent(c, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder b = new NotificationCompat.Builder(c, CH_RESULT)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW);
            if (Build.VERSION.SDK_INT >= 33) {
                if (c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (Throwable ignored) {
        }
    }
}
