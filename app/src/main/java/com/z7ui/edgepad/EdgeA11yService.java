package com.z7ui.edgepad;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 无障碍服务：执行全局动作、截屏、记住前台应用包名。
 * 只订阅窗口切换事件，不申请读取窗口内容的权限。
 */
public class EdgeA11yService extends AccessibilityService {

    private static volatile EdgeA11yService instance;
    private static volatile String foreground = "";
    private static volatile Consumer<String> foregroundListener;

    /** 最近出现过前台的应用，最近的在前。用来实现「上一个应用」。 */
    private static final ArrayDeque<String> HISTORY = new ArrayDeque<>();

    public static boolean connected() {
        return instance != null;
    }

    public static String foreground() {
        return foreground;
    }

    public static void setForegroundListener(Consumer<String> l) {
        foregroundListener = l;
    }

    /** 上一个应用：跳过自己、当前应用和系统界面。 */
    public static String previousPackage() {
        synchronized (HISTORY) {
            for (String p : HISTORY) {
                if (p.equals(foreground)) continue;
                if (p.equals("com.z7ui.edgepad")) continue;
                if (p.startsWith("com.android.systemui")) continue;
                return p;
            }
        }
        return "";
    }

    public static boolean global(int action) {
        EdgeA11yService s = instance;
        return s != null && s.performGlobalAction(action);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 50;
            setServiceInfo(info);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = event.getPackageName();
        if (cs == null || cs.length() == 0) return;
        String pkg = cs.toString();

        // 只记包名，不读界面内容
        if (pkg.equals(foreground)) return;
        foreground = pkg;
        synchronized (HISTORY) {
            HISTORY.remove(pkg);
            HISTORY.addFirst(pkg);
            while (HISTORY.size() > 12) HISTORY.removeLast();
        }
        Consumer<String> l = foregroundListener;
        if (l != null) l.accept(pkg);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    // ---------------- 截屏 ----------------

    public static void screenshot(Context c) {
        EdgeA11yService s = instance;
        if (s == null) {
            Actions.notifyResult(c, "截屏失败", "无障碍服务没开");
            return;
        }
        if (Build.VERSION.SDK_INT < 30) {
            Actions.notifyResult(c, "截屏失败", "系统版本低于 Android 11");
            return;
        }
        s.takeScreenshot(Display.DEFAULT_DISPLAY, ContextCompat.getMainExecutor(s),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        HardwareBuffer buffer = result.getHardwareBuffer();
                        try {
                            Bitmap hw = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
                            if (hw == null) {
                                Actions.notifyResult(c, "截屏失败", "拿不到画面");
                                return;
                            }
                            Bitmap soft = hw.copy(Bitmap.Config.ARGB_8888, false);
                            hw.recycle();
                            String where = saveBitmap(c, soft);
                            soft.recycle();
                            Actions.notifyResult(c, "已截屏", where);
                        } catch (Throwable t) {
                            Actions.notifyResult(c, "截屏失败", String.valueOf(t));
                        } finally {
                            buffer.close();
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        Actions.notifyResult(c, "截屏失败", "错误码 " + errorCode);
                    }
                });
    }

    private static String saveBitmap(Context c, Bitmap bmp) {
        String name = "EdgePad-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(new Date()) + ".png";
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                v.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/边触");
                Uri uri = c.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "写入相册失败";
                try (OutputStream os = c.getContentResolver().openOutputStream(uri)) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                }
                return "已存到相册 Pictures/边触";
            } catch (Throwable t) {
                return "保存失败：" + t;
            }
        }
        try {
            File dir = new File(c.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "边触");
            if (!dir.exists() && !dir.mkdirs()) return "建目录失败";
            File f = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            return "已存到 " + f.getAbsolutePath();
        } catch (Throwable t) {
            return "保存失败：" + t;
        }
    }
}
