package com.example.rsy.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private int languageID = 0;

    private Button button_login = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languageID = getSharedPreferences("settings", Activity.MODE_PRIVATE).getInt("language", 1);
        switchLanguage(languageID);

        setContentView(R.layout.activity_login);

        TextView textView_changeLanguage = (TextView) findViewById(R.id.text_changeLanguage);
        textView_changeLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String language[] = getResources().getStringArray(R.array.array_languages);
                AlertDialog.Builder ad = new AlertDialog.Builder(LoginActivity.this);
                ad.setTitle(R.string.text_changeLanguage);
                ad.setSingleChoiceItems(language, languageID, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        languageID = which;
                    }
                });
                ad.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchLanguage(languageID);
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), LoginActivity.class);
                        // 两句位置调换 效果不同
                        startActivity(intent);
                        finish();
                    }
                });
                ad.create().show();
            }
        });

        button_login = (Button) findViewById(R.id.button_login);
        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), BLEFinderActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void switchLanguage(int languageID) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        switch (languageID) {
            case 0:
                configuration.locale = Locale.US;
                break;
            case 1:
                configuration.locale = Locale.SIMPLIFIED_CHINESE;
                break;
            default:
                break;
        }
        resources.updateConfiguration(configuration, dm);
        SharedPreferences sp = getSharedPreferences("settings", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("language", languageID);
        editor.apply();
    }
}