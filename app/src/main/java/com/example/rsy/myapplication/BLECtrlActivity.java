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
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class BLECtrlActivity extends AppCompatActivity {

    private final String TAG = "BLECtrlActivity";

    private String mBLEName, mBLEAddress = null;
    private BLEService mBLEService = null;
    private BroadcastReceiver mBroadcastReceiver = null;

    private List<BluetoothGattService> mGattServices = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blectrl);

        Intent intent = getIntent();
        mBLEName = intent.getStringExtra("name");
        mBLEAddress = intent.getStringExtra("address");
        Log.i(TAG, "name : " + mBLEName + " : " + "address : " + mBLEAddress);

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
                            }
                        }
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
