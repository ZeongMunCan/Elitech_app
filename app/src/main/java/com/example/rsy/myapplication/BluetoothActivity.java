package com.example.rsy.myapplication;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    Button button_search, button_send = null;
    ToggleButton button_connect = null;
    Switch switch_bt = null;
    ListView listView_btFound = null;
    BluetoothAdapter bluetoothAdapter;
    BroadcastReceiver btStatusReceiver = null;
    ArrayList<BluetoothDevice> btDevicesFound = new ArrayList<>();
    List<Map<String, Object>> list_btFound = new ArrayList<>();

    SimpleAdapter btFoundAdapter = null;

    BluetoothDevice btConnected = null;

    MyBluetoothSocket mbs = null;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.obj.toString();
            switch (msg.what) {
                case 1:
                    Log.i("bt msg get", message);
                    Toast.makeText(getApplicationContext(), "get message : " + msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case -1:
                    button_connect.setChecked(false);
                    Toast.makeText(getApplicationContext(), "ERROR : failed to connect to device", Toast.LENGTH_SHORT).show();
                    break;
                case -2:
                    button_connect.setChecked(false);
                    Toast.makeText(getApplicationContext(), "ERROR : lost connection", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device do not support BLE", Toast.LENGTH_SHORT).show();
        }

        button_search = (Button) findViewById(R.id.button_search);
        button_connect = (ToggleButton) findViewById(R.id.button_connect);
        button_send = (Button) findViewById(R.id.button_send);
        switch_bt = (Switch) findViewById(R.id.switch_bt);
        listView_btFound = (ListView) findViewById(R.id.listView_Found);

        btFoundAdapter = new SimpleAdapter(this, list_btFound,
                R.layout.btfoundlist, new String[]{"btName", "btAddress"}, new int[]{R.id.btName, R.id.btAddress});
        listView_btFound.setAdapter(btFoundAdapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        button_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "Please make sure Bluetooth is on", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Scanning for Bluetooth devices...", Toast.LENGTH_SHORT).show();
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                    btDevicesFound.clear();
                    list_btFound.clear();
                    btFoundAdapter.notifyDataSetChanged();
                    showBondedDevices(bluetoothAdapter);
                    bluetoothAdapter.startDiscovery();
                }
            }
        });

        btStatusHandler(bluetoothAdapter.getState());

        btStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        Log.i("bt", "all found : " + btd.getName());
                        if (!btDevicesFound.contains(btd)) {
                            btDevicesFound.add(btd);
                            if (btd.getBondState() != BluetoothDevice.BOND_BONDED) {
                                Log.i("bt", "not bonded : " + btd.getName());
                                list_btFound.add(addFoundBTInfo(btd));
                            }
                        }
                        btFoundAdapter.notifyDataSetChanged();
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i("bt", "Discovery finished");
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        btStatusHandler(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        switch (btd.getBondState()) {
                            case BluetoothDevice.BOND_BONDING:
                                Log.i("bt", "pairing");
                                break;
                            case BluetoothDevice.BOND_BONDED:
                                btConnected = btd;
                                Toast.makeText(getApplicationContext(), "Succeed to pair to " + btd.getName(), Toast.LENGTH_SHORT).show();
                                break;
                            case BluetoothDevice.BOND_NONE:
                                Toast.makeText(getApplicationContext(), "Device : " + btd.getName() + " has been removed", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    case BluetoothDevice.ACTION_PAIRING_REQUEST:
                        // set default pin code : 1234
                        // TODO auto bond without showing pin dialog
                        if (btd.setPin("1234".getBytes())) Log.i("bt", "setPin");
                        break;
                    default:
                        break;
                }
            }
        };

        //注册蓝牙状态接收广播
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btStatusReceiver, intent);

        switch_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switch_bt.isChecked()) {
                    if (!bluetoothAdapter.isEnabled()) {
                        switch_bt.setChecked(false);
                        switch_bt.setEnabled(false);
                        bluetoothAdapter.enable();
                        Toast.makeText(getApplicationContext(), "Turning ON the Bluetooth...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.disable();
                    }
                }
            }
        });

        // bond to the device you clicked
        listView_btFound.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                BluetoothDevice btd = bluetoothAdapter.getRemoteDevice(list_btFound.get(position).get("btAddress").toString());
                if (btd.getBondState() != BluetoothDevice.BOND_BONDED) {
                    btd.createBond();
                    Toast.makeText(getApplicationContext(), "Trying to pair " + btd.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    btConnected = btd;
                    Toast.makeText(getApplicationContext(), "This device is already paired", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // long click to remove the bonded device
        listView_btFound.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice btd = bluetoothAdapter.getRemoteDevice(list_btFound.get(position).get("btAddress").toString());
                AlertDialog.Builder ad = new AlertDialog.Builder(getApplicationContext());
                if (btd.getBondState() == BluetoothDevice.BOND_BONDED) {
                    ad.setTitle("Remove");
                    ad.setMessage("Sure to remove " + btd.getName());
                    ad.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Method removePair = BluetoothDevice.class.getMethod("removeBond");
                                removePair.invoke(btd);
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            showBondedDevices(bluetoothAdapter);
                        }
                    });
                    ad.setPositiveButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    ad.create().show();

                } else {
                    Toast.makeText(getApplicationContext(), "This device is not bonded.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // from off to on
                if (button_connect.isChecked())
                    if (btConnected == null) {
                        button_connect.setChecked(false);
                        Toast.makeText(getApplicationContext(), "Please choose one device to connect...", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            mbs = new MyBluetoothSocket(btConnect(btConnected), handler);
                            if (mbs.connect()) {
                                Log.i("bt", "bt is connected");
                                mbs.start();
                                button_connect.setChecked(true);
                            } else
                                button_connect.setChecked(false);
                        } catch (IOException e) {
                            button_connect.setChecked(false);
                            e.printStackTrace();
                        }
                    }
                else {
                    if (mbs != null)
                        try {
                            mbs.exit();
                            button_connect.setChecked(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        });

        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mbs != null) {
                    int i;
                    i = mbs.write("hello world");
                    Log.i("bt", "write errorCode : " + i);
                }
            }
        });

    }

    // 检查蓝牙状态，并根据状态改变开关
    private void btStatusHandler(int state) {
        if (state == BluetoothAdapter.STATE_ON) {
            switch_bt.setEnabled(true);
            switch_bt.setChecked(true);
        } else if (state == BluetoothAdapter.STATE_OFF) {
            switch_bt.setEnabled(true);
            switch_bt.setChecked(false);
        }
    }

    private Map<String, Object> addFoundBTInfo(BluetoothDevice bd) {
        String btName = bd.getName(), btAddress = bd.getAddress();
        Map<String, Object> map = new HashMap<>();
        map.put("btName", btName);
        map.put("btAddress", btAddress);
        return map;
    }

    private void showBondedDevices(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        list_btFound.clear();
        for (BluetoothDevice bd : bluetoothDevices) {
            if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i("bt", "bonded : " + bd.getName());
                list_btFound.add(addFoundBTInfo(bd));
            }
        }
        btFoundAdapter.notifyDataSetChanged();
    }

    private BluetoothSocket btConnect(BluetoothDevice bd) throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//        return bd.createRfcommSocketToServiceRecord(uuid);
        return bd.createInsecureRfcommSocketToServiceRecord(uuid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter != null) {
            btStatusHandler(bluetoothAdapter.getState());
            showBondedDevices(bluetoothAdapter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mbs != null)
            try {
                mbs.exit();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btStatusReceiver);
        if (mbs != null)
            try {
                mbs.exit();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
