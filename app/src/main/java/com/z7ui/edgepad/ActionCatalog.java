package com.z7ui.edgepad;

import java.util.ArrayList;
import java.util.List;

/** 可绑定的动作清单。加动作只改这里和 Actions，不用动界面。 */
public class ActionCatalog {

    public static class Def {
        public final String key;
        public final String label;
        public final String group;

        Def(String key, String label, String group) {
            this.key = key;
            this.label = label;
            this.group = group;
        }
    }

    private static final List<Def> LIST = new ArrayList<>();

    static {
        add("none", "不绑定", "其它");

        add("back", "返回", "导航");
        add("home", "回到主屏", "导航");
        add("recents", "最近任务", "导航");
        add("previous_app", "上一个应用", "导航");

        add("notifications", "通知面板", "面板");
        add("quick_settings", "快捷设置", "面板");
        add("power_dialog", "电源菜单", "面板");

        add("screenshot", "截屏", "屏幕与工具");
        add("lock_screen", "锁屏", "屏幕与工具");
        add("torch", "手电筒", "屏幕与工具");
        add("split_screen", "分屏", "屏幕与工具");

        add("volume_up", "音量 +", "声音");
        add("volume_down", "音量 −", "声音");
        add("mute", "静音", "声音");
        add("media_play", "播放 / 暂停", "声音");
        add("media_next", "下一首", "声音");
        add("media_prev", "上一首", "声音");

        add("open_app", "打开应用", "应用");
        add("search", "搜索", "应用");
        add("assist", "语音助手", "应用");
        add("system_settings", "系统设置", "应用");
    }

    private static void add(String key, String label, String group) {
        LIST.add(new Def(key, label, group));
    }

    public static List<Def> all() {
        return LIST;
    }

    public static List<String> groups() {
        List<String> out = new ArrayList<>();
        for (Def d : LIST) {
            if (!out.contains(d.group)) out.add(d.group);
        }
        return out;
    }

    public static List<Def> inGroup(String group) {
        List<Def> out = new ArrayList<>();
        for (Def d : LIST) {
            if (d.group.equals(group)) out.add(d);
        }
        return out;
    }

    public static Def find(String key) {
        for (Def d : LIST) {
            if (d.key.equals(key)) return d;
        }
        return null;
    }

    /** 给界面看的完整描述，带参数的动作会换成人话。 */
    public static String describe(String value) {
        String key = Config.key(value);
        String arg = Config.arg(value);
        if ("open_app".equals(key) && !arg.isEmpty()) {
            int bar = arg.indexOf('|');
            return "打开 " + (bar >= 0 ? arg.substring(bar + 1) : arg);
        }
        Def d = find(key);
        return d == null ? "不绑定" : d.label;
    }

    /** open_app 的值：open_app:包名|名字 */
    public static String openAppValue(String pkg, String label) {
        return "open_app:" + pkg + "|" + label;
    }

    public static String openAppPackage(String value) {
        String arg = Config.arg(value);
        int bar = arg.indexOf('|');
        return bar >= 0 ? arg.substring(0, bar) : arg;
    }
}
