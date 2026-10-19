package com.z7ui.edgepad;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/** 手柄外观：五个参数，拖完立刻生效。 */
public class AppearanceActivity extends AppCompatActivity {

    private int width, height, inset, alpha, offset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        width = Config.widthDp(this);
        height = Config.heightPct(this);
        inset = Config.insetDp(this);
        alpha = Config.alphaPct(this);
        offset = Config.offsetPct(this);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        sv.addView(root);
        setContentView(sv);

        TextView title = new TextView(this);
        title.setText("手柄外观");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("改完立刻生效。手柄越大越好按，但挡住的内容也越多。");
        sub.setTextSize(12);
        sub.setTextColor(ContextCompat.getColor(this, R.color.muted));
        sub.setPadding(0, dp(6), 0, dp(18));
        root.addView(sub);

        root.addView(slider("宽度", "手柄横向有多粗", 8, 32, width, "dp",
                v -> width = v));
        root.addView(slider("高度", "占屏幕高度的比例", 20, 90, height, "%",
                v -> height = v));
        root.addView(slider("离边缘", "往屏幕里缩多少，避开系统返回手势", 0, 40, inset, "dp",
                v -> inset = v));
        root.addView(slider("透明度", "数值越大越显眼", 10, 100, alpha, "%",
                v -> alpha = v));
        root.addView(slider("垂直位置", "负数往上，正数往下", -40, 40, offset, "%",
                v -> offset = v));

        TextView tip = new TextView(this);
        tip.setText("建议：全面屏手势下，离边缘至少留 6dp。手柄贴着边缘时，"
                + "它覆盖的那一段会吃掉系统返回手势。");
        tip.setTextSize(12);
        tip.setTextColor(ContextCompat.getColor(this, R.color.muted));
        tip.setPadding(0, dp(16), 0, 0);
        root.addView(tip);
    }

    private ViewGroup slider(String name, String desc, int min, int max, int value,
                             String unit, OnValue changed) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#1F000000"));
        box.setBackground(bg);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(15);
        head.addView(t, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView val = new TextView(this);
        val.setText(value + unit);
        val.setTextSize(15);
        val.setTextColor(ContextCompat.getColor(this, R.color.brand));
        head.addView(val);
        box.addView(head);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(11);
        d.setTextColor(ContextCompat.getColor(this, R.color.muted));
        d.setPadding(0, dp(2), 0, dp(4));
        box.addView(d);

        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(value - min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                val.setText((min + progress) + unit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                changed.set(min + sb.getProgress());
                save();
            }
        });
        box.addView(bar);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(10);
        box.setLayoutParams(p);
        return box;
    }

    private void save() {
        Config.setGeometry(this, width, height, inset, alpha, offset);
        EdgeService.refresh(this);
    }

    private interface OnValue {
        void set(int v);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
