package com.example.rsy.myapplication;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
    private OnBLEServiceModified mModified = null;

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

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // TODO action for broadcasting
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mModified.onConnectListener(gatt);
                Log.i(TAG, "Gatt connected");
                // attempt to discovery services after successful connection
                Log.i(TAG, "attempting to start service discovery : " + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mModified.onDisconnectListener(gatt);
                Log.i(TAG, "Gatt disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mModified.onServicesDiscovered(gatt);
            } else
                Log.w(TAG, "onServicesDiscovered status: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mModified.onCharacteristicRead(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
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

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null)
            return;
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null)
            return;
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (mBluetoothGatt == null || mBluetoothAdapter == null)
            return;
        mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
    }

    /**
     * Turn bytes to readable HEX
     *
     * @param bytes raw array with byte
     * @return output HEX, null when bytes has no element
     */
    public static StringBuilder toHex(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(bytes.length);
            for (byte byteChar : bytes)
                stringBuilder.append(String.format("%02X ", byteChar));
            return stringBuilder;
        }
        return null;
    }

    public void setOnBLEServiceModified(OnBLEServiceModified modified) {
        mModified = modified;
    }

    public static abstract class OnBLEServiceModified {
        public void onConnectListener(BluetoothGatt gatt) {
        }

        public void onDisconnectListener(BluetoothGatt gatt) {
        }

        public void onServicesDiscovered(BluetoothGatt gatt) {
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        }

    }
}
