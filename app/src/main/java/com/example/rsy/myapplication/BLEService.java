package com.example.rsy.myapplication;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEService extends Service {

    private final static String TAG = BLEService.class.getSimpleName();
    // to interact with some activities
    private final IBinder mBinder = new BLEBinder();

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private String mBLEAddress = null;

    public final static String ACTION_GATT_CONNECTED           = "com.example.rsy.myapplication.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.example.rsy.myapplication.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.rsy.myapplication.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.example.rsy.myapplication.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "com.example.rsy.myapplication.EXTRA_DAT";


    public BLEService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public class BLEBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    private void sendBroadcast(String action) {
        sendBroadcast(new Intent(action));
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // TODO action for broadcasting
            String action;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                action = ACTION_GATT_CONNECTED;
                sendBroadcast(action);
                Log.i(TAG, "Gatt connected");
                // attempt to discovery services after successful connection
                Log.i(TAG, "attempting to start service discovery : " + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                action = ACTION_GATT_DISCONNECTED;
                sendBroadcast(action);
                Log.i(TAG, "Gatt disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                sendBroadcast(ACTION_GATT_SERVICES_DISCOVERED);
            else
                Log.w(TAG, "onServicesDiscovered status: " + status);
        }
    };

    public boolean initialize() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "initialize - " + "unable to get BLE");
            Toast.makeText(getApplicationContext(), "unable to get BLE in initialize", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public boolean connect(String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "connect - " + "unable to get BLE");
            Toast.makeText(getApplicationContext(), "unable to get BLE in connect", Toast.LENGTH_SHORT).show();
            return false;
        }

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        // TODO does it take time ?  --rsy
        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        Log.i(TAG, "connection complete");
        mBLEAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "disconnect - " + "adapter or gatt is null");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a BLE, release its resources.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "close - " + "Gatt is null");
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        return mBluetoothGatt == null ? null : mBluetoothGatt.getServices();
    }
}
