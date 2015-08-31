package com.example.rsy.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private int deviceModel = 310;
    private TextView textView_changeWeightUnit;
    private TextView textView_changePressUnit;
    private Button button_addIn, button_recycle, button_repeat, button_full, button_stop = null;
    private View layout_additional_pressDashboard, layout_additional_buttons = null;
    private ImageView imageView_settings = null;
    //ZeongMunCan：返回按钮
    private ImageView imageView_back = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (deviceModel == 210) {
            ((TextView) findViewById(R.id.textView_deviceModel)).setText(getString(R.string.text_LMC_210));
            layout_additional_pressDashboard = findViewById(R.id.layout_additionalPressDashboard);
            layout_additional_pressDashboard.setVisibility(View.GONE);
            layout_additional_buttons = findViewById(R.id.layout_additionalButtons);
            layout_additional_buttons.setVisibility(View.GONE);
        }

        final String[] weightUnits = getResources().getStringArray(R.array.array_weightUnits);
        final String[] pressUnits = getResources().getStringArray(R.array.array_pressUnits);
        textView_changeWeightUnit = (TextView) findViewById(R.id.textView_changeWeightUnity);
        textView_changePressUnit = (TextView) findViewById(R.id.textView_changePressUnity);
        textView_changeWeightUnit.setText(weightUnits[0]);
        textView_changePressUnit.setText(pressUnits[0]);

        View.OnClickListener listener = new View.OnClickListener() {
            final View view = getLayoutInflater().inflate(R.layout.ad_add_manually_style, null);
            final EditText et = (EditText) view.findViewById(R.id.edit_addInfo);
            AlertDialog.Builder ad = null;

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.textView_changeWeightUnity:
                        ad = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.text_changeUnit))
                                .setItems(weightUnits, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        textView_changeWeightUnit.setText(weightUnits[which]);
                                    }
                                });
                        ad.create().show();
                        break;
                    case R.id.textView_changePressUnity:
                        ad = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.text_changeUnit))
                                .setItems(pressUnits, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        textView_changePressUnit.setText(pressUnits[which]);
                                    }
                                });
                        ad.create().show();
                        break;
                    case R.id.button_addIn:
                        et.setHint(getString(R.string.text_inputAddIn));
                        ad = new AlertDialog.Builder(MainActivity.this)
                                .setView(view)
                                .setTitle(getString(R.string.button_addIn))
                                .setPositiveButton(getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getApplicationContext(), "add in "
                                                + et.getText(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        ad.create().show();
                        break;
                    case R.id.button_recycle:
                        et.setHint(getString(R.string.text_inputRecycle));
                        ad = new AlertDialog.Builder(MainActivity.this)
                                .setView(view)
                                .setTitle(getString(R.string.button_recycle))
                                .setPositiveButton(getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getApplicationContext(), "recycle "
                                                + et.getText(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        ad.create().show();
                        break;
                    case R.id.button_repeat:
                        ad = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.button_repeat))
                                .setMessage(getString(R.string.text_repeat))
                                .setPositiveButton(getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getApplicationContext(),
                                                "repeat the last action", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                        ad.create().show();
                        break;
                    case R.id.button_full:
                        break;
                    case R.id.button_stop:
                        break;
                }
            }
        };

        textView_changeWeightUnit.setOnClickListener(listener);
        textView_changePressUnit.setOnClickListener(listener);

        button_addIn = (Button) findViewById(R.id.button_addIn);
        button_recycle = (Button) findViewById(R.id.button_recycle);
        button_repeat = (Button) findViewById(R.id.button_repeat);
        button_full = (Button) findViewById(R.id.button_full);
        button_stop = (Button) findViewById(R.id.button_stop);

        button_addIn.setOnClickListener(listener);
        button_recycle.setOnClickListener(listener);
        button_repeat.setOnClickListener(listener);
        button_full.setOnClickListener(listener);
        button_stop.setOnClickListener(listener);

        imageView_settings = (ImageView) findViewById(R.id.bar_main_settings);
        imageView_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        //ZeongMuncan：返回按钮返回至上一界面
        imageView_back = (ImageView)findViewById((R.id.bar_main_back));
        imageView_back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,DeviceFindActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
