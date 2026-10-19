package com.z7ui.edgepad;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout root;
    private TextView overlayStatus, a11yStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);

        TextView title = new TextView(this);
        title.setText("边触");
        title.setTextSize(30);
        title.setTextColor(color(android.R.color.black));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("从屏幕边缘往里滑，执行你绑定的动作。左边和右边各 6 种手势。");
        sub.setTextSize(13);
        sub.setTextColor(color(R.color.muted));
        sub.setPadding(0, dp(6), 0, dp(18));
        root.addView(sub);

        root.addView(permCard("悬浮窗", "手柄要画在其他应用上面",
                v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())))));
        root.addView(permCard("无障碍", "返回、主屏、最近任务、截屏要用",
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(sectionTitle("手柄"));
        root.addView(sideSwitch(Config.LEFT, "左手柄", "从左侧往右滑"));
        root.addView(sideSwitch(Config.RIGHT, "右手柄", "从右侧往左滑"));

        root.addView(sectionTitle("设置"));
        root.addView(row("左侧手势", "6 个方向 × 长短，各自绑定动作",
                v -> open(GestureActivity.class, Config.LEFT)));
        root.addView(row("右侧手势", "6 个方向 × 长短，各自绑定动作",
                v -> open(GestureActivity.class, Config.RIGHT)));
        root.addView(row("手柄外观", "宽度 / 高度 / 位置 / 透明度",
                v -> open(AppearanceActivity.class, null)));
        root.addView(row("应用例外", "在这些应用里不显示手柄、不识别手势",
                v -> open(ExceptionsActivity.class, null)));
        root.addView(row("关于", "版本与实现说明", v -> about()));

        TextView note = new TextView(this);
        note.setText("提示：全面屏手势下，屏幕左右边缘那一条是系统「返回」，第三方应用盖不掉。"
                + "所以手柄默认离边缘 40dp，待在系统手势区之外。\n\n"
                + "默认手柄宽 16dp、高 46%、离边缘 40dp、透明度 42%。"
                + "短滑与长滑的分界是屏高的 16%。"
                + "如果你改用三键导航，可以把手柄在「手柄外观」里调到贴边。");
        note.setTextSize(12);
        note.setTextColor(color(R.color.muted));
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note);
    }

    // ---------------- 组件 ----------------

    private View permCard(String name, String why, View.OnClickListener click) {
        LinearLayout box = card();
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(16);
        t.setTextColor(color(android.R.color.black));
        texts.addView(t);
        TextView w = new TextView(this);
        w.setText(why);
        w.setTextSize(12);
        w.setTextColor(color(R.color.muted));
        texts.addView(w);
        line.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView status = new TextView(this);
        status.setTextSize(13);
        line.addView(status);

        box.addView(line);
        box.setOnClickListener(click);
        if ("悬浮窗".equals(name)) overlayStatus = status;
        else a11yStatus = status;
        return box;
    }

    private View sideSwitch(String side, String title, String sub) {
        LinearLayout box = card();
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        t.setTextColor(color(android.R.color.black));
        texts.addView(t);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(12);
        s.setTextColor(color(R.color.muted));
        texts.addView(s);
        line.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(Config.sideOn(this, side));
        sw.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            Config.setSideOn(this, side, checked);
            apply();
        });
        line.addView(sw);
        box.addView(line);
        return box;
    }

    private View row(String title, String sub, View.OnClickListener click) {
        LinearLayout box = card();
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        t.setTextColor(color(android.R.color.black));
        texts.addView(t);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(12);
        s.setTextColor(color(R.color.muted));
        texts.addView(s);
        line.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(22);
        arrow.setTextColor(color(R.color.muted));
        line.addView(arrow);

        box.addView(line);
        box.setOnClickListener(click);
        return box;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#1F000000"));
        box.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(10);
        box.setLayoutParams(p);
        return box;
    }

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(color(R.color.muted));
        t.setPadding(dp(4), dp(14), 0, dp(8));
        return t;
    }

    // ---------------- 状态 ----------------

    private void refresh() {
        if (overlayStatus != null) {
            boolean ok = Settings.canDrawOverlays(this);
            overlayStatus.setText(ok ? "已开启" : "去开启");
            overlayStatus.setTextColor(color(ok ? R.color.ok : R.color.warn));
            overlayStatus.setOnClickListener(v -> startActivity(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()))));
        }
        if (a11yStatus != null) {
            boolean ok = a11yOn();
            a11yStatus.setText(ok ? "已开启" : "去开启");
            a11yStatus.setTextColor(color(ok ? R.color.ok : R.color.warn));
        }
        apply();
    }

    private void apply() {
        boolean any = Config.sideOn(this, Config.LEFT) || Config.sideOn(this, Config.RIGHT);
        if (Settings.canDrawOverlays(this) && any) {
            EdgeService.refresh(this);
        } else {
            EdgeService.stop(this);
        }
    }

    private boolean a11yOn() {
        if (EdgeA11yService.connected()) return true;
        String want = getPackageName() + "/" + EdgeA11yService.class.getName();
        String cur = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (cur == null) return false;
        for (String s : cur.split(":")) {
            if (s.equalsIgnoreCase(want)) return true;
        }
        return false;
    }

    private void open(Class<?> cls, String side) {
        Intent i = new Intent(this, cls);
        if (side != null) i.putExtra("side", side);
        startActivity(i);
    }

    private void about() {
        new AlertDialog.Builder(this)
                .setTitle("边触 0.1")
                .setMessage("把三星 One Hand Operation+ 的思路搬到普通安卓上：\n\n"
                        + "· 左右各一条悬浮手柄，每侧 6 种手势（水平 / 斜上 / 斜下 × 短滑 / 长滑）\n"
                        + "· 动作 21 个：导航、面板、截屏、手电筒、音量与媒体键、打开应用、分屏…\n"
                        + "· 应用例外清单里不显示手柄\n"
                        + "· 手柄外观可调：宽度、高度、离边缘距离、透明度、垂直位置\n\n"
                        + "和原版不同的地方：原版是系统签名应用，能注入按键、能让系统导航手势让位。"
                        + "边触只能靠悬浮窗抢那一小段边缘的触摸，所以手柄默认往屏幕里缩了 8dp，"
                        + "避开系统返回手势的热区。\n\n"
                        + "数据都在本机，不联网。")
                .setPositiveButton("好", null)
                .show();
    }

    private int color(int id) {
        return ContextCompat.getColor(this, id);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
