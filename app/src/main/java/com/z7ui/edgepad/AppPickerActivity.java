package com.z7ui.edgepad;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** 选一个应用，返回给手势绑定页。 */
public class AppPickerActivity extends AppCompatActivity {

    private final List<Apps.Item> all = new ArrayList<>();
    private final List<Apps.Item> shown = new ArrayList<>();
    private ArrayAdapter<Apps.Item> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), 0);

        TextView title = new TextView(this);
        title.setText("选一个应用");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        EditText filter = new EditText(this);
        filter.setHint("搜索应用名");
        filter.setSingleLine(true);
        root.addView(filter);

        ListView list = new ListView(this);
        root.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        all.addAll(Apps.launchable(this));
        shown.addAll(all);

        adapter = new ArrayAdapter<Apps.Item>(this, android.R.layout.simple_list_item_2,
                android.R.id.text1, shown) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                Apps.Item it = getItem(position);
                t1.setText(it.label);
                t2.setText(it.pkg);
                t2.setTextSize(11);
                t2.setTextColor(ContextCompat.getColor(AppPickerActivity.this, R.color.muted));
                return v;
            }
        };
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            Apps.Item it = shown.get(position);
            Intent data = new Intent();
            data.putExtra("pkg", it.pkg);
            data.putExtra("label", it.label);
            setResult(RESULT_OK, data);
            finish();
        });

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String q = s.toString().trim().toLowerCase();
                shown.clear();
                for (Apps.Item it : all) {
                    if (q.isEmpty() || it.label.toLowerCase().contains(q)
                            || it.pkg.toLowerCase().contains(q)) {
                        shown.add(it);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
