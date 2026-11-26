package com.z7ui.edgepad;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 崩溃留档：只写本机私有目录，不上传。
 * 下次启动时可以在「关于」里看到并导出。
 */
public final class Crashes {

    private static final String DIR = "crash";
    private static final String FILE = "last.txt";

    private Crashes() {
    }

    public static void install(final Context app) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                write(app, t, e);
            } catch (Throwable ignored) {
            }
            if (prev != null) {
                prev.uncaughtException(t, e);
            }
        });
    }

    private static void write(Context c, Thread t, Throwable e) {
        File dir = new File(c.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        File f = new File(dir, FILE);
        try (PrintWriter w = new PrintWriter(new FileWriter(f, false))) {
            w.println("时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date()));
            w.println("版本: " + Updater.currentVersionName(c)
                    + " (" + Updater.currentVersionCode(c) + ")");
            w.println("设备: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                    + " / Android " + android.os.Build.VERSION.RELEASE);
            w.println("线程: " + (t == null ? "?" : t.getName()));
            w.println();
            e.printStackTrace(w);
        } catch (Throwable ignored) {
        }
    }

    /** 没有记录时返回 null */
    public static String last(Context c) {
        File f = new File(new File(c.getFilesDir(), DIR), FILE);
        if (!f.isFile()) {
            return null;
        }
        try {
            byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
            String s = new String(b, "UTF-8").trim();
            return s.isEmpty() ? null : s;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void clear(Context c) {
        File f = new File(new File(c.getFilesDir(), DIR), FILE);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
