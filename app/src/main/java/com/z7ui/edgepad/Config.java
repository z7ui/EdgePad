package com.z7ui.edgepad;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置全部存在 SharedPreferences 里。手势绑定存成字符串：
 * 简单动作就是键本身，带参数的动作写成 "open_app:包名|显示名"。
 */
public class Config {

    public static final String PREFS = "edgepad";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";

    /** 6 种手势：方向 × 长短。顺序也是界面上的显示顺序。 */
    public static final String[] GESTURES = {
            "short_h", "long_h", "short_up", "long_up", "short_down", "long_down"
    };

    public static final String[] GESTURE_TITLES = {
            "短滑 · 水平", "长滑 · 水平", "短滑 · 斜上",
            "长滑 · 斜上", "短滑 · 斜下", "长滑 · 斜下"
    };

    public static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------------- 手势绑定 ----------------

    public static String action(Context c, String side, String gesture) {
        return p(c).getString("map_" + side + "_" + gesture, "none");
    }

    public static void setAction(Context c, String side, String gesture, String value) {
        p(c).edit().putString("map_" + side + "_" + gesture, value).apply();
    }

    public static String key(String value) {
        if (value == null) return "none";
        int i = value.indexOf(':');
        return i < 0 ? value : value.substring(0, i);
    }

    public static String arg(String value) {
        if (value == null) return "";
        int i = value.indexOf(':');
        return i < 0 ? "" : value.substring(i + 1);
    }

    // ---------------- 手柄外观 ----------------

    public static boolean sideOn(Context c, String side) {
        return p(c).getBoolean("on_" + side, RIGHT.equals(side));
    }

    public static void setSideOn(Context c, String side, boolean on) {
        p(c).edit().putBoolean("on_" + side, on).apply();
    }

    public static int widthDp(Context c) {
        return clamp(p(c).getInt("width_dp", 16), 8, 32);
    }

    public static int heightPct(Context c) {
        return clamp(p(c).getInt("height_pct", 46), 20, 90);
    }

    public static int insetDp(Context c) {
        // 默认 40dp：全面屏手势下，屏幕边缘那一条是系统返回，贴边会被吃掉，
        // 实测 40dp 能稳定收到真实滑动。用户可以在外观里调到贴边（三键导航时可用）。
        return clamp(p(c).getInt("inset_dp", 40), 0, 120);
    }

    public static int alphaPct(Context c) {
        return clamp(p(c).getInt("alpha", 42), 10, 100);
    }

    public static int offsetPct(Context c) {
        return clamp(p(c).getInt("offset_pct", 0), -40, 40);
    }

    public static void setGeometry(Context c, int widthDp, int heightPct,
                                   int insetDp, int alphaPct, int offsetPct) {
        p(c).edit()
                .putInt("width_dp", clamp(widthDp, 8, 32))
                .putInt("height_pct", clamp(heightPct, 20, 90))
                .putInt("inset_dp", clamp(insetDp, 0, 120))
                .putInt("alpha", clamp(alphaPct, 10, 100))
                .putInt("offset_pct", clamp(offsetPct, -40, 40))
                .apply();
    }

    /** 单独设离边缘，贴边模式开启时直接写 0。 */
    public static void setInset(Context c, int insetDp) {
        p(c).edit().putInt("inset_dp", clamp(insetDp, 0, 120)).apply();
    }

    // ---------------- 贴边模式（需 root） ----------------

    public static boolean edgeModeOn(Context c) {
        return p(c).getBoolean("edge_mode_on", false);
    }

    public static void setEdgeModeOn(Context c, boolean on) {
        p(c).edit().putBoolean("edge_mode_on", on).apply();
    }

    /** 开启贴边前记住用户原来的导航模式，关闭时还原。默认 2（全面屏手势）。 */
    public static int savedNavMode(Context c) {
        return p(c).getInt("saved_nav_mode", 2);
    }

    public static void setSavedNavMode(Context c, int mode) {
        p(c).edit().putInt("saved_nav_mode", mode).apply();
    }

    public static int savedInset(Context c) {
        return p(c).getInt("saved_inset", 40);
    }

    public static void setSavedInset(Context c, int inset) {
        p(c).edit().putInt("saved_inset", inset).apply();
    }

    // ---------------- 例外清单 ----------------

    public static Set<String> exceptions(Context c) {
        return new HashSet<>(p(c).getStringSet("exceptions", new HashSet<>()));
    }

    public static void setExceptions(Context c, Set<String> set) {
        p(c).edit().putStringSet("exceptions", new HashSet<>(set)).apply();
    }

    public static boolean excepted(Context c, String pkg) {
        return pkg != null && !pkg.isEmpty() && exceptions(c).contains(pkg);
    }

    // ---------------- 手电筒状态 ----------------

    public static boolean torchOn(Context c) {
        return p(c).getBoolean("torch_on", false);
    }

    public static void setTorchOn(Context c, boolean on) {
        p(c).edit().putBoolean("torch_on", on).apply();
    }

    public static boolean ready(Context c) {
        return android.provider.Settings.canDrawOverlays(c) && EdgeA11yService.connected();
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
