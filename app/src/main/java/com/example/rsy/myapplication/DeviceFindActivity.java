package com.example.rsy.myapplication;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeviceFindActivity extends AppCompatActivity {

    private final String TAG = "DeviceFindActivity";

    private Button button_start, button_findDevices, button_chooseInfo = null;
    private TextView textView_chooseInfo = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private ListView listView_bluetoothDevices = null;
    private SimpleAdapter bluetoothDeviceListAdapter = null;
    private List<Map<String, Object>> list_bd = new ArrayList<>();
    private final int bdState[] = {R.drawable.bg_blue_button, R.drawable.bg_gray_button, R.drawable.bg_green_button};

    // bluetooth
    private BluetoothAdapter bluetoothAdapter = null;
    private BroadcastReceiver broadcastReceiver = null;
    private MyBluetoothSocket myBluetoothSocket = null;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicefind);

        textView_chooseInfo = (TextView) findViewById(R.id.textView_chooseInfo);
        button_start = (Button) findViewById(R.id.button_start);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        button_findDevices = (Button) findViewById(R.id.button_findDevices);
        button_findDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForBluetooth();
                if (!swipeRefreshLayout.isRefreshing())
                    swipeRefreshLayout.setRefreshing(true);
            }
        });
        button_chooseInfo = (Button) findViewById(R.id.button_chooseInfo);
        button_chooseInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> list_deviceInfo = new ArrayList<>();
                Collections.addAll(list_deviceInfo, getResources().getStringArray(R.array.array_deviceInfo));
                list_deviceInfo.add(getString(R.string.text_noMatchInfo));
                final String deviceInfo[] = list_deviceInfo.toArray(new String[list_deviceInfo.size()]);
                AlertDialog.Builder ad = new AlertDialog.Builder(DeviceFindActivity.this);
                ad.setTitle(getString(R.string.text_chooseMyDevice));
                ad.setItems(deviceInfo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != deviceInfo.length - 1)
                            textView_chooseInfo.setText(deviceInfo[which]);
                        else {
                            final View view = getLayoutInflater().inflate(R.layout.ad_add_manually_style, null);
                            final EditText et = (EditText) view.findViewById(R.id.edit_addInfo);
                            et.setHint(getString(R.string.text_inputYourInfo));
                            new AlertDialog.Builder(DeviceFindActivity.this).setView(view).setTitle(getString(R.string.text_newDeviceInfo))
                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            textView_chooseInfo.setText(et.getText());
                                        }
                                    }).create().show();
                        }
                    }
                });
                ad.create().show();
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.sr_fresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                searchForBluetooth();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        final String[] tag = {getString(R.string.text_bonded), getString(R.string.text_newDevices)};
        listView_bluetoothDevices = (ListView) findViewById(R.id.list_bd);
        bluetoothDeviceListAdapter = new MyAdapter(this, tag, R.layout.list_tag_bd_style, list_bd, R.layout.list_bd_style, new String[]{"icon",
                "name", "mac", "state"}, new int[]{R.id.bd_list_logo, R.id.bd_list_name,
                R.id.bd_list_mac, R.id.bd_list_state});
        listView_bluetoothDevices.setAdapter(bluetoothDeviceListAdapter);

        // for bluetooth
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device do not support BLE", Toast.LENGTH_SHORT).show();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // when this activity is created, turn on the Bluetooth without manually click a power switch.  --rsy
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "bluetooth is down, now turn on it", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.enable();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // devices found list
        final ArrayList<BluetoothDevice> bluetoothDevicesList = new ArrayList<>();
        // devices bonded, including bonded, bonded but absent
        final ArrayList<MyBluetoothDevice> myBluetoothBonded = new ArrayList<>();
        // devices not bonded
        final ArrayList<MyBluetoothDevice> myBluetoothUnBonded = new ArrayList<>();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                MyBluetoothDevice mbd;
                switch (action) {
                    // called when the bluetooth power is on
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_ON)
                            Toast.makeText(getApplicationContext(), "Bluetooth is on", Toast.LENGTH_SHORT).show();
                        else if (state == BluetoothAdapter.STATE_OFF)
                            Toast.makeText(getApplicationContext(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
                        break;
                    // stop the swipeRefreshLayout when discovery finished, but the discovery takes a lot of time.
                    // add a manually time setting  --rsy
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i(TAG, "discovery finished");
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        Log.i(TAG, "discovery started");
                        // clear the list when rescan the bluetooth
                        // --BUG DONE--  clear except bonded device  --rsy
                        // there are some situations :
                        // 1. clear unBonded devices
                        // 2. update the state of bonded devices
                        // (bonded but not find nearby : gray) (bonded and found : green) (unBonded and found : blue) preliminary setting
                        bluetoothDevicesList.clear();
                        myBluetoothBonded.clear();
                        myBluetoothUnBonded.clear();
                        list_bd.clear();
                        myBluetoothBonded.addAll(getBonded());
                        list_bd.addAll(notifyData(tag, myBluetoothBonded, myBluetoothUnBonded));
                        bluetoothDeviceListAdapter.notifyDataSetChanged();
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        Log.i(TAG, "find bd: " + bd.getName());
                        mbd = new MyBluetoothDevice(bd);
                        // distinguish between bonded and unBonded devices
                        if (bd.getBondState() != BluetoothDevice.BOND_BONDED)
                            myBluetoothUnBonded.add(mbd);
                        bluetoothDevicesList.add(bd);
                        // check the state of bonded device
                        for (MyBluetoothDevice myBluetoothDevice : myBluetoothBonded) {
                            myBluetoothDevice.notifyState(bluetoothDevicesList);
                        }
                        list_bd.clear();
                        list_bd.addAll(notifyData(tag, myBluetoothBonded, myBluetoothUnBonded));
                        // the list_bd is a pointer that point to a list should be given to adapter, in this case, list_bd can not be assigned again  --rsy
                        // list_bd = notifyData(tag, notifyBonded(bluetoothDevicesList), notifyUnBonded(bluetoothDevicesList));
                        bluetoothDeviceListAdapter.notifyDataSetChanged();
                        break;
                    // bond device
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        switch (bd.getBondState()) {
                            case BluetoothDevice.BOND_BONDING:
                                Log.i("bt", "pairing");
                                break;
                            case BluetoothDevice.BOND_BONDED:
//                                btConnected = btd;
                                Toast.makeText(getApplicationContext(), "Succeed to pair to " + bd.getName(), Toast.LENGTH_SHORT).show();
                                // bonded means this device is present
                                mbd = new MyBluetoothDevice(bd).setState(0);
                                myBluetoothBonded.add(mbd);
                                mbd.removeFrom(myBluetoothUnBonded);
                                list_bd.clear();
                                list_bd.addAll(notifyData(tag, myBluetoothBonded, myBluetoothUnBonded));
                                bluetoothDeviceListAdapter.notifyDataSetChanged();
                                break;
                            case BluetoothDevice.BOND_NONE:
                                Toast.makeText(getApplicationContext(), "Device : " + bd.getName() + " has been removed", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    case BluetoothDevice.ACTION_PAIRING_REQUEST:
                        // set default pin code : 1234
                        // TODO auto bond without showing pin dialog
                        if (bd.setPin("1234".getBytes())) Log.i("bt", "setPin");
                        break;
                    default:
                        break;
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);

        listView_bluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                swipeRefreshLayout.setRefreshing(false);
                BluetoothDevice btd = bluetoothAdapter.getRemoteDevice(list_bd.get(position).get("mac").toString());
                if (btd.getBondState() != BluetoothDevice.BOND_BONDED) {
                    btd.createBond();
                    Toast.makeText(getApplicationContext(), "Trying to pair " + btd.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    //    btConnected = btd;
                    Toast.makeText(getApplicationContext(), "This device is already paired", Toast.LENGTH_SHORT).show();
                }
            }
        });

        listView_bluetoothDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, final long id) {
                final BluetoothDevice bd = bluetoothAdapter.getRemoteDevice(list_bd.get(position).get("mac").toString());
                final MyBluetoothDevice mbd = new MyBluetoothDevice(bd);
                AlertDialog.Builder ad = new AlertDialog.Builder(DeviceFindActivity.this);
                if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                    ad.setTitle(R.string.text_removeBonded);
                    ad.setMessage(getString(R.string.text_sureToRemove) + bd.getName());
                    ad.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Method removePair = BluetoothDevice.class.getMethod("removeBond");
                                removePair.invoke(bd);
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            // it takes a little time to removeBond device
                            myBluetoothBonded.clear();
                            myBluetoothBonded.addAll(getBonded());
                            // manually remove the bonded device  --rsy
                            // to make sure remove successfully  --rsy
                            mbd.removeFrom(myBluetoothBonded);
                            myBluetoothUnBonded.add(mbd);
                            list_bd.clear();
                            list_bd.addAll(notifyData(tag, myBluetoothBonded, myBluetoothUnBonded));
                            bluetoothDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                    ad.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
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
        myBluetoothBonded.addAll(getBonded());
        list_bd.addAll(notifyData(tag, myBluetoothBonded, myBluetoothUnBonded));
        bluetoothDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void searchForBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Please make sure the bluetooth is on", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "searching for Bluetooth devices...", Toast.LENGTH_SHORT).show();
            if (bluetoothAdapter.isDiscovering())
                bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();

            // set the discovery time manually
            // TODO --BUG--  cannot stop when discovery cancelled manually  --rsy
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.cancelDiscovery();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 10000);
        }
    }

    private List<MyBluetoothDevice> getBonded() {
        List<MyBluetoothDevice> list = new ArrayList<>();
        Set<BluetoothDevice> smbd = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : smbd) {
            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(bluetoothDevice);
                myBluetoothDevice.setState(1);
                list.add(myBluetoothDevice);
            }
        }
        return list;
    }

    private List<Map<String, Object>> notifyDevices(ArrayList<MyBluetoothDevice> bdList) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MyBluetoothDevice bd : bdList) {
            list.add(addBD(bd));
        }
        return list;
    }

    private List<Map<String, Object>> notifyData(String tag[], ArrayList<MyBluetoothDevice> bonded, ArrayList<MyBluetoothDevice> unBonded) {
        List<Map<String, Object>> list = new ArrayList<>();
        // don not show this if there is no device bonded
        if (!bonded.isEmpty()) {
            list.add(addBD(tag[0]));
            for (Map<String, Object> map : notifyDevices(bonded)) {
                list.add(map);
            }
        }
        list.add(addBD(tag[1]));
        for (Map<String, Object> map : notifyDevices(unBonded)) {
            list.add(map);
        }
        return list;
    }

    private Map<String, Object> addBD(MyBluetoothDevice myBluetoothDevice) {
        String name = myBluetoothDevice.getName();
        String mac = myBluetoothDevice.getAddress();
        int state = myBluetoothDevice.getState();
        //TODO the icon should be changed
        int icon = myBluetoothDevice.getIcon();
        Map<String, Object> map = new HashMap<>();
        map.put("tag", false);
        map.put("icon", icon);
        map.put("name", name);
        map.put("mac", mac);
        map.put("state", bdState[state]);
        return map;
    }

    private Map<String, Object> addBD(String tag) {
        Map<String, Object> map = new HashMap<>();
        map.put("tag", true);
        map.put("name", tag);
        return map;
    }

    private class MyBluetoothDevice {

        protected BluetoothDevice bd = null;
        protected String name = "null", address = "null";
        protected int icon = R.drawable.icon_bt_gray, state = 2;
        // state 0 : bonded and present
        //       1 : bonded but absent
        //       2 : not bonded

        public MyBluetoothDevice(BluetoothDevice bd) {
            name = bd.getName();
            address = bd.getAddress();
            icon = name.startsWith("LMC") ? R.drawable.icon_bt
                    : R.drawable.icon_bt_gray;
            this.bd = bd;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public int getIcon() {
            return icon;
        }

        public int getState() {
            return state;
        }

        public void notifyState(List<BluetoothDevice> bdList) {
            if (bd.getBondState() == BluetoothDevice.BOND_BONDED && bdList.contains(bd))
                state = 0;
            else if (bd.getBondState() == BluetoothDevice.BOND_BONDED)
                state = 1;
            else
                state = 2;
        }

        public MyBluetoothDevice setState(int state) {
            this.state = state;
            return this;
        }

        // --BUG DONE-- myBluetoothBonded.iterator() inside has no next  --rsy
        public void removeFrom(List<MyBluetoothDevice> list) {
            Iterator<MyBluetoothDevice> iterator = list.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getAddress().equals(this.getAddress()))
                    iterator.remove();
            }
        }

    }

    public class MyAdapter extends SimpleAdapter {

        // tag is not useful now, I can't find a way to use it correctly --rsy
        private String[] tag = null;
        private int tagResource = 0;
        private List<? extends Map<String, ?>> mData = null;
        private int mResource = 0;
        private String[] mFrom = null;
        private int[] mTo = null;

        /**
         * Constructor
         *
         * @param context  The context where the View associated with this SimpleAdapter is running
         * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
         *                 Maps contain the data for each row, and should include all the entries specified in
         *                 "from"
         * @param resource Resource identifier of a view layout that defines the views for this list
         *                 item. The layout file should include at least those named views defined in "to"
         * @param from     A list of column names that will be added to the Map associated with each
         *                 item.
         * @param to       The views that should display column in the "from" parameter. These should all be
         *                 TextViews. The first N views in this list are given the values of the first N columns
         */
        public MyAdapter(Context context, String[] tag, int tagResource, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            this.mResource = resource;
            this.mData = data;
            this.mFrom = from;
            this.mTo = to;
            this.tag = tag;
            this.tagResource = tagResource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (mData.get(position).get("tag").equals(true)) {
                view = getLayoutInflater().inflate(tagResource, null);
                TextView textView = (TextView) view.findViewById(R.id.bd_list_tag);
                textView.setText((String) mData.get(position).get("name"));
            } else {
                view = getLayoutInflater().inflate(mResource, null);
                bindView(position, view);
            }
            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            // the tag item is not clickable
            return !mData.get(position).get("tag").equals(true);
        }

        private void bindView(int position, View view) {
            final Map dataSet = mData.get(position);
            if (dataSet == null) {
                return;
            }

            final String[] from = mFrom;
            final int[] to = mTo;
            final int count = to.length;

            for (int i = 0; i < count; i++) {
                final View v = view.findViewById(to[i]);
                if (v != null) {
                    final Object data = dataSet.get(from[i]);
                    String text = data == null ? "" : data.toString();
                    if (text == null) {
                        text = "";
                    }
                    if (v instanceof TextView) {
                        // Note: keep the instanceof TextView check at the bottom of these
                        // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                        setViewText((TextView) v, text);
                    } else if (v instanceof ImageView) {
                        if (data instanceof Integer) {
                            setViewImage((ImageView) v, (Integer) data);
                        } else {
                            setViewImage((ImageView) v, text);
                        }
                    } else {
                        throw new IllegalStateException(v.getClass().getName() + " is not a " +
                                " view that can be bounds by this SimpleAdapter");
                    }

                }
            }
        }
    }
}
