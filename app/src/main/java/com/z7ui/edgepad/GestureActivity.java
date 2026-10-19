package com.z7ui.edgepad;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** 一侧 6 个手势的绑定界面。 */
public class GestureActivity extends AppCompatActivity {

    private String side;
    private LinearLayout list;
    private int pending = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        side = getIntent().getStringExtra("side");
        if (!Config.LEFT.equals(side)) side = Config.RIGHT;

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(24));
        sv.addView(root);
        setContentView(sv);

        TextView title = new TextView(this);
        title.setText(Config.LEFT.equals(side) ? "左侧手势" : "右侧手势");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("滑动的起点必须在手柄上。短滑与长滑的分界是屏高的 16%，"
                + "方向按水平 / 斜上 / 斜下分。");
        sub.setTextSize(12);
        sub.setTextColor(ContextCompat.getColor(this, R.color.muted));
        sub.setPadding(0, dp(6), 0, dp(16));
        root.addView(sub);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        for (int i = 0; i < Config.GESTURES.length; i++) {
            final int index = i;
            String gesture = Config.GESTURES[i];
            String value = Config.action(this, side, gesture);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(14), dp(14));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(1), Color.parseColor("#1F000000"));
            row.setBackground(bg);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView t = new TextView(this);
            t.setText(Config.GESTURE_TITLES[i]);
            t.setTextSize(16);
            texts.addView(t);
            TextView s = new TextView(this);
            s.setText(ActionCatalog.describe(value));
            s.setTextSize(12);
            s.setTextColor(ContextCompat.getColor(this, R.color.muted));
            texts.addView(s);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = new TextView(this);
            arrow.setText("›");
            arrow.setTextSize(22);
            arrow.setTextColor(ContextCompat.getColor(this, R.color.muted));
            row.addView(arrow);

            row.setOnClickListener(v -> pickGroup(index));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.bottomMargin = dp(10);
            list.addView(row, p);
        }
    }

    /** 两级选择：先选分类，再选动作。 */
    private void pickGroup(int index) {
        pending = index;
        List<String> groups = ActionCatalog.groups();
        new AlertDialog.Builder(this)
                .setTitle("选个分类")
                .setItems(groups.toArray(new String[0]), (d, which) -> pickAction(groups.get(which)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void pickAction(String group) {
        List<ActionCatalog.Def> defs = ActionCatalog.inGroup(group);
        List<String> labels = new ArrayList<>();
        for (ActionCatalog.Def d : defs) labels.add(d.label);
        new AlertDialog.Builder(this)
                .setTitle(group)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    ActionCatalog.Def def = defs.get(which);
                    if ("open_app".equals(def.key)) {
                        startActivityForResult(new Intent(this, AppPickerActivity.class), 11);
                    } else {
                        bind(def.key);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void bind(String value) {
        if (pending < 0) return;
        Config.setAction(this, side, Config.GESTURES[pending], value);
        EdgeService.refresh(this);
        pending = -1;
        render();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11 && resultCode == RESULT_OK && data != null) {
            String pkg = data.getStringExtra("pkg");
            String label = data.getStringExtra("label");
            bind(ActionCatalog.openAppValue(pkg, label));
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
