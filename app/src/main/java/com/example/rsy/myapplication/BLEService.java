package com.example.rsy.myapplication;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEService extends Service {

    private final static String TAG = BLEService.class.getSimpleName();
    // to interact with some activities
    private final IBinder mBinder = new BLEBinder();

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private String mBLEAddress = null;

    public BLEService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class BLEBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }
    };

    private boolean initialize() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG,"initialize - " + "unable to get BLE");
            Toast.makeText(getApplicationContext(), "unable to get BLE in initialize", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean connect(String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG,"connect - " + "unable to get BLE");
            Toast.makeText(getApplicationContext(), "unable to get BLE in connect", Toast.LENGTH_SHORT).show();
            return false;
        }

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        // TODO does it take time ?  --rsy
        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        mBLEAddress = address;
        return true;
    }

    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "disconnect - " + "adapter or gatt is null");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a BLE, release its resources.
     */
    private void close() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "close - " + "Gatt is null");
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
