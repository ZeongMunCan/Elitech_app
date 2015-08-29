package com.example.rsy.myapplication;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BLECtrlActivity extends AppCompatActivity {

    private final String TAG = "BLECtrlActivity";

    private Spinner spinner = null;
    private TextView textView_read = null;
    private EditText editText_write = null;
    private Button button_read, button_write = null;

    private BluetoothGattCharacteristic mCharacteristic = null;

    private String mBLEName, mBLEAddress = null;
    private BLEService mBLEService = null;
    private BroadcastReceiver mBroadcastReceiver = null;

    private List<BluetoothGattService> mGattServices = null;
    private List<BluetoothGattCharacteristic> mCharacteristics = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blectrl);

        Intent intent = getIntent();
        mBLEName = intent.getStringExtra("name");
        mBLEAddress = intent.getStringExtra("address");
        Log.i(TAG, "name : " + mBLEName + " : " + "address : " + mBLEAddress);

        spinner = (Spinner) findViewById(R.id.spinner_UUID);
        textView_read = (TextView) findViewById(R.id.textView_read);
        editText_write = (EditText) findViewById(R.id.edit_write);
        button_read = (Button) findViewById(R.id.button_read);
        button_write = (Button) findViewById(R.id.button_write);

        final List<String> uuids = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, uuids);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCharacteristic = mCharacteristics.get(position);
                Toast.makeText(getApplicationContext(), mCharacteristic.getUuid().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        button_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCharacteristic == null) {
                    Toast.makeText(getApplicationContext(), "choose one characteristic first", Toast.LENGTH_SHORT).show();
                    return;
                }
                mBLEService.readCharacteristic(mCharacteristic);
            }
        });

        button_write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCharacteristic == null) {
                    Toast.makeText(getApplicationContext(), "choose one characteristic first", Toast.LENGTH_SHORT).show();
                    return;
                }
                String value = editText_write.getText().toString();
                // TODO write value
            }
        });

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case BLEService.ACTION_GATT_CONNECTED:
                        break;
                    case BLEService.ACTION_GATT_DISCONNECTED:
                        break;
                    case BLEService.ACTION_GATT_SERVICES_DISCOVERED:
                        // TODO show these services in list
                        mGattServices = mBLEService.getSupportedGattServices();
                        for (BluetoothGattService gattService : mGattServices) {
                            Log.i(TAG, "GattService : " + gattService.getUuid().toString());
                            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                                Log.i(TAG, "Chara : " + characteristic.getUuid().toString());
                                mCharacteristics.add(characteristic);
                                uuids.add(characteristic.getUuid().toString());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        break;
                    case BLEService.ACTION_DATA_AVAILABLE:
                        byte[] bytes = intent.getByteArrayExtra(BLEService.EXTRA_DATA);
                        String value = new String(bytes);
                        Log.i(TAG, "read : " + value);
                        textView_read.setText(value);
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        registerReceiver(mBroadcastReceiver, intentFilter);

        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * get service controller
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBLEService = ((BLEService.BLEBinder) service).getService();
            if (!mBLEService.initialize()) {
                Toast.makeText(getApplicationContext(), "unable to initialize BLE Service", Toast.LENGTH_SHORT).show();
                finish();
            }
            mBLEService.connect(mBLEAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBLEService = null;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        mBLEService.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);
    }
}
