package com.z7ui.edgepad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * 设置导出 / 导入。
 * 走系统文件选择器（SAF），不需要任何存储权限；
 * 备份内容就是 AppMeta.PREFS 里那几个 SharedPreferences 的全部键值。
 */
public final class Backup {

    public static final int REQ_EXPORT = 9001;
    public static final int REQ_IMPORT = 9002;

    private Backup() {
    }

    public static void startExport(Activity act) {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
            i.putExtra(Intent.EXTRA_TITLE, AppMeta.KEY + "-" + stamp + ".json");
            act.startActivityForResult(i, REQ_EXPORT);
        } catch (Throwable ignored) {
        }
    }

    public static void startImport(Activity act) {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            act.startActivityForResult(i, REQ_IMPORT);
        } catch (Throwable ignored) {
        }
    }

    /** 把配置序列化成 JSON 文本 */
    public static String dump(Context c) throws Exception {
        JSONObject root = new JSONObject();
        root.put("app", AppMeta.KEY);
        root.put("version", Updater.currentVersionName(c));
        root.put("versionCode", Updater.currentVersionCode(c));
        root.put("exportedAt",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

        JSONObject data = new JSONObject();
        for (String file : AppMeta.PREFS) {
            SharedPreferences sp = c.getSharedPreferences(file, Context.MODE_PRIVATE);
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
            data.put(file, obj);
        }
        root.put("data", data);

        // 额外的私有文件（比如 rules.json），存文本
        JSONObject files = new JSONObject();
        for (String name : AppMeta.FILES) {
            String text = readPrivate(c, name);
            if (text != null) {
                files.put(name, text);
            }
        }
        root.put("files", files);

        return root.toString(2);
    }

    /** 写回配置，返回写入的 prefs 文件数 */
    public static int restore(Context c, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        String app = root.optString("app", "");
        if (!app.isEmpty() && !app.equals(AppMeta.KEY)) {
            throw new Exception("这份备份来自「" + app + "」，不是本应用");
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new Exception("文件里没有可导入的配置");
        }

        int n = 0;
        for (String file : AppMeta.PREFS) {
            JSONObject obj = data.optJSONObject(file);
            if (obj == null) {
                continue;
            }
            SharedPreferences.Editor ed = c.getSharedPreferences(file, Context.MODE_PRIVATE).edit();
            ed.clear();
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = obj.get(k);
                if (v instanceof Boolean) {
                    ed.putBoolean(k, (Boolean) v);
                } else if (v instanceof Integer) {
                    ed.putInt(k, (Integer) v);
                } else if (v instanceof Long) {
                    ed.putLong(k, (Long) v);
                } else if (v instanceof Double) {
                    ed.putFloat(k, ((Double) v).floatValue());
                } else {
                    ed.putString(k, String.valueOf(v));
                }
            }
            ed.apply();
            n++;
        }

        // 额外的私有文件
        JSONObject files = root.optJSONObject("files");
        if (files != null) {
            for (String name : AppMeta.FILES) {
                String text = files.optString(name, null);
                if (text != null) {
                    writePrivate(c, name, text);
                    n++;
                }
            }
        }
        return n;
    }

    private static String readPrivate(Context c, String name) {
        File f = new File(c.getFilesDir(), name);
        if (!f.isFile()) {
            return null;
        }
        try {
            byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
            return new String(b, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void writePrivate(Context c, String name, String text) throws Exception {
        File f = new File(c.getFilesDir(), name);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new Exception("建不了目录");
        }
        try (OutputStream out = new java.io.FileOutputStream(f)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public static String readUri(Context c, Uri uri) throws Exception {
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new Exception("读不到这个文件");
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    public static void writeUri(Context c, Uri uri, String text) throws Exception {
        try (OutputStream out = c.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new Exception("写不了这个文件");
            }
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
