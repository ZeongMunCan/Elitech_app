package com.example.rsy.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ListView listView_settings = null;
    private ImageView imageView_back = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        listView_settings = (ListView) findViewById(R.id.listView_settings);
        String settingItems[] = getResources().getStringArray(R.array.array_settingsItem);

        List<Map<String, Object>> data = new ArrayList<>();
        for (String s : settingItems) {
            data.add(addItem(s));
        }
//        DeviceFindActivity.MyAdapter myAdapter = new DeviceFindActivity.MyAdapter(this,
//                tag,
//                R.layout.list_tag_bd_style,
//                data,
//                R.layout.list_settings_item,
//                new String[]{"item"},
//                new int[]{R.id.textView_settingsItem}
//                );
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, data, R.layout.list_settings_item, new String[]{"item"}, new int[]{R.id.textView_settingsItem});
        listView_settings.setAdapter(simpleAdapter);

        imageView_back = (ImageView) findViewById(R.id.bar_settings_back);
        imageView_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SettingsActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private Map<String, Object> addItem(String item) {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        return map;
    }
}
