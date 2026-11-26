package com.z7ui.edgepad;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * 关于页：版本、检查更新、博客、源码、更多应用、崩溃记录。
 * 所有 App 共用同一份，只依赖 AppMeta 里的常量。
 */
public class AboutActivity extends AppCompatActivity {

    private TextView tvUpdateState;
    private boolean autoChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar bar = findViewById(R.id.aboutToolbar);
        bar.setNavigationOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tvAppName);
        TextView tvVersion = findViewById(R.id.tvAppVersion);
        tvName.setText(R.string.app_name);
        tvVersion.setText("v" + Updater.currentVersionName(this)
                + " · build " + Updater.currentVersionCode(this));

        tvUpdateState = findViewById(R.id.tvUpdateState);
        findViewById(R.id.rowUpdate).setOnClickListener(v -> checkUpdate(true));
        findViewById(R.id.rowBlog).setOnClickListener(v -> open(AppMeta.BLOG_URL));
        findViewById(R.id.rowRepo).setOnClickListener(v -> open(AppMeta.REPO_URL));
        findViewById(R.id.rowMore).setOnClickListener(v -> open(AppMeta.MORE_URL));

        setupCrashRow();

        // 进页面自动静默查一次，不打扰
        checkUpdate(false);
    }

    private void setupCrashRow() {
        View row = findViewById(R.id.rowCrash);
        TextView state = findViewById(R.id.tvCrashState);
        String last = Crashes.last(this);
        if (TextUtils.isEmpty(last)) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            state.setText("上次运行崩溃过，点击查看并反馈");
            row.setOnClickListener(v -> showCrash(last));
        }
    }

    private void checkUpdate(boolean manual) {
        if (!manual && autoChecked) {
            return;
        }
        autoChecked = true;
        tvUpdateState.setText("检查中…");

        Updater.check(this, info -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (info == null) {
                tvUpdateState.setText("已是最新");
                if (manual) {
                    toast("已是最新版本");
                }
                return;
            }
            tvUpdateState.setText("有新版本 " + info.version);
            if (manual) {
                showUpdateDialog(info);
            }
        });
    }

    private void showUpdateDialog(Updater.Info info) {
        String msg = TextUtils.isEmpty(info.notes)
                ? "当前 v" + Updater.currentVersionName(this) + "，最新 " + info.version
                : info.notes;
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + info.version)
                .setMessage(msg)
                .setNegativeButton("以后再说", null)
                .setPositiveButton("去下载", (d, w) -> open(info.apkUrl))
                .show();
    }

    private void showCrash(String text) {
        String body = text.length() > 4000 ? text.substring(0, 4000) + "\n\n…（已截断）" : text;
        new AlertDialog.Builder(this)
                .setTitle("上次崩溃记录")
                .setMessage(body)
                .setNegativeButton("关闭", null)
                .setNeutralButton("清除", (d, w) -> {
                    Crashes.clear(this);
                    toast("已清除");
                    setupCrashRow();
                })
                .setPositiveButton("分享", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT,
                            getString(R.string.app_name) + " 崩溃记录");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "分享崩溃记录"));
                })
                .show();
    }

    private void open(String url) {
        if (!Updater.openUrl(this, url)) {
            toast("没有可用的浏览器");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
