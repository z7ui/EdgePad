package com.z7ui.edgepad;

import android.content.Context;
import android.provider.Settings;

/**
 * 贴边模式：root 关掉全面屏手势（切三键导航），让系统边缘手势桩消失，
 * 手柄就能贴到屏幕边缘。关掉时还原用户原来的导航模式和手柄位置。
 */
public class EdgeMode {

    public static int currentNavMode(Context c) {
        try {
            return Settings.Secure.getInt(c.getContentResolver(), "navigation_mode");
        } catch (Exception e) {
            return 2;
        }
    }

    /** 返回 false 表示 root 没生效（没授权），调用方要提示用户。 */
    public static boolean enable(Context c) {
        int cur = currentNavMode(c);
        Root.Result r = Root.run(c, "settings put secure navigation_mode 0");
        if (!r.ok) return false;
        Config.setSavedNavMode(c, cur);
        Config.setSavedInset(c, Config.insetDp(c));
        Config.setEdgeModeOn(c, true);
        Config.setInset(c, 0);
        EdgeService.refresh(c);
        return true;
    }

    public static boolean disable(Context c) {
        Root.Result r = Root.run(c, "settings put secure navigation_mode " + Config.savedNavMode(c));
        if (!r.ok) return false;
        Config.setInset(c, Config.savedInset(c));
        Config.setEdgeModeOn(c, false);
        EdgeService.refresh(c);
        return true;
    }

    /** 开机或应用启动时，如果贴边模式开着，确保系统还停在无手势状态。 */
    public static void ensure(Context c) {
        if (Config.edgeModeOn(c) && Root.available(c)) {
            Root.run(c, "settings put secure navigation_mode 0");
        }
    }
}
