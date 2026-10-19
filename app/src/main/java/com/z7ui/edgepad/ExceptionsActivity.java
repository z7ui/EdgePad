package com.z7ui.edgepad;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 例外清单：勾上的应用里不显示手柄，也不识别手势。 */
public class ExceptionsActivity extends AppCompatActivity {

    private final List<Apps.Item> items = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), 0);

        TextView title = new TextView(this);
        title.setText("应用例外");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("勾上的应用里，手柄会隐藏、手势不生效。适合游戏、视频和输入法场景。");
        sub.setTextSize(12);
        sub.setTextColor(ContextCompat.getColor(this, R.color.muted));
        sub.setPadding(0, dp(4), 0, dp(8));
        root.addView(sub);

        list = new ListView(this);
        root.addView(list, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        selected.addAll(Config.exceptions(this));
        items.addAll(Apps.installed(this));

        List<String> labels = new ArrayList<>();
        for (Apps.Item it : items) labels.add(it.label + "\n" + it.pkg);

        // 用两行显示，第二行是包名，方便确认是哪个
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, labels) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                             android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                TextView t = v.findViewById(android.R.id.text1);
                t.setTextSize(13);
                t.setPadding(dp(4), dp(8), dp(4), dp(8));
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < items.size(); i++) {
            list.setItemChecked(i, selected.contains(items.get(i).pkg));
        }

        list.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = items.get(position).pkg;
            if (selected.contains(pkg)) selected.remove(pkg);
            else selected.add(pkg);
            list.setItemChecked(position, selected.contains(pkg));
            Config.setExceptions(this, selected);
            EdgeService.refresh(this);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Config.setExceptions(this, selected);
        EdgeService.refresh(this);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
