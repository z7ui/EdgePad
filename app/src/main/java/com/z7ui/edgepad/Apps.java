package com.z7ui.edgepad;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 应用清单。带启动入口的用来绑动作，全部已安装的用来做例外列表。 */
public class Apps {

    public static class Item {
        public final String pkg;
        public final String label;

        Item(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    public static List<Item> launchable(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        List<Item> out = new ArrayList<>();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName())) continue;
            if (contains(out, pkg)) continue;
            out.add(new Item(pkg, ri.loadLabel(pm).toString()));
        }
        sort(out);
        return out;
    }

    public static List<Item> installed(Context c) {
        PackageManager pm = c.getPackageManager();
        List<Item> out = new ArrayList<>();
        for (android.content.pm.ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if (ai.packageName.equals(c.getPackageName())) continue;
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
            out.add(new Item(ai.packageName, pm.getApplicationLabel(ai).toString()));
        }
        sort(out);
        return out;
    }

    private static boolean contains(List<Item> list, String pkg) {
        for (Item i : list) if (i.pkg.equals(pkg)) return true;
        return false;
    }

    private static void sort(List<Item> list) {
        Collections.sort(list, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
    }
}
