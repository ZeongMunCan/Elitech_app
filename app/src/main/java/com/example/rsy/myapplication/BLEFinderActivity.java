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
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.Objects;
import java.util.Set;

public class BLEFinderActivity extends AppCompatActivity {

    private final String TAG = "BLEFinderActivity";

    private Button button_start, button_findDevices, button_chooseInfo = null;
    private TextView textView_chooseInfo = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private ListView listView_bluetoothDevices = null;
    private MyAdapter bluetoothDeviceListAdapter = null;
    private List<Map<String, Object>> list_bd = new ArrayList<>();
    private final int bdState[] = {R.drawable.bg_blue_button, R.drawable.bg_gray_button, R.drawable.bg_green_button};

    // bluetooth
    private BluetoothAdapter bluetoothAdapter = null;
    // BLE
    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blefinder);

        textView_chooseInfo = (TextView) findViewById(R.id.textView_leChooseInfo);
        button_start = (Button) findViewById(R.id.button_leStart);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        button_findDevices = (Button) findViewById(R.id.button_leFindDevices);
        button_findDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForBLE();
            }
        });
        button_chooseInfo = (Button) findViewById(R.id.button_leChooseInfo);
        button_chooseInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> list_deviceInfo = new ArrayList<>();
                Collections.addAll(list_deviceInfo, getResources().getStringArray(R.array.array_deviceInfo));
                list_deviceInfo.add(getString(R.string.text_noMatchInfo));
                final String deviceInfo[] = list_deviceInfo.toArray(new String[list_deviceInfo.size()]);
                AlertDialog.Builder ad = new AlertDialog.Builder(BLEFinderActivity.this);
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
                            new AlertDialog.Builder(BLEFinderActivity.this).setView(view).setTitle(getString(R.string.text_newDeviceInfo))
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
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.sr_leFresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                searchForBLE();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        final String[] tag = {getString(R.string.text_bonded), getString(R.string.text_newDevices)};
        listView_bluetoothDevices = (ListView) findViewById(R.id.list_leBD);
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
        bluetoothDeviceListAdapter.initList(getBonded());

        listView_bluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String name = list_bd.get(position).get("name").toString();
                final String mac = list_bd.get(position).get("mac").toString();
                new AlertDialog.Builder(BLEFinderActivity.this)
                        .setTitle("Connect")
                        .setMessage(name + "\n" + mac)
                        .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.putExtra("name", name);
                                intent.putExtra("address", mac);
                                intent.setClass(BLEFinderActivity.this, BLECtrlActivity.class);
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton(R.string.no, null)
                        .create().show();
            }
        });

        // TODO remove bonded device from [My Device] and add new bonded device to it.
        /**
         * LeScanCallback: when Le device is found, call this
         */
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            private MyBluetoothDevice mbd = null;

            /**
             * @param device     device found
             * @param rssi       signal intensity
             * @param scanRecord I have no sense about this param
             */
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                // list must be updated in UI thread  --rsy
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mbd = new MyBluetoothDevice(device);
                        bluetoothDeviceListAdapter.addBLEDevice(mbd);
                    }
                });

            }
        };

        // auto search for BLE devices if BLE is available
        // -- BUG DONE -- the swipeRefreshLayout does not show when it start, to fix this, I add a little delay.  --rsy
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                searchForBLE();
            }
        },200);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void searchForBLE() {
        if (!bluetoothAdapter.isEnabled())
            Toast.makeText(getApplicationContext(), "Please make sure the bluetooth is on", Toast.LENGTH_SHORT).show();
        else {
            // TODO isDiscovering method is not checked
            if (bluetoothAdapter.isDiscovering())
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            bluetoothAdapter.startLeScan(mLeScanCallback);
            bluetoothDeviceListAdapter.initList(getBonded());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 5000);
            if (!swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(true);
        }
    }

    private List<MyBluetoothDevice> getBonded() {
        List<MyBluetoothDevice> list = new ArrayList<>();
        Set<BluetoothDevice> smbd = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : smbd) {
            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(bluetoothDevice).setState(1);
                list.add(myBluetoothDevice);
            }
        }
        return list;
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

        public BluetoothDevice getDevice() {
            return bd;
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

        public boolean isContain(List<?> myBluetoothDeviceList) {
            for (Object o : myBluetoothDeviceList)
                if (o instanceof MyBluetoothDevice) {
                    if (((MyBluetoothDevice) o).getAddress().equals(address)) return true;
                } else if (o instanceof BluetoothDevice) {
                    if (((BluetoothDevice) o).getAddress().equals(address)) return true;
                }
            return false;
        }

    }

    private class MyAdapter extends SimpleAdapter {

        // tag is not useful now, I can't find a way to use it correctly --rsy
        private String[] tag = null;
        private int tagResource = 0;
        private List<Map<String, Object>> mData = null;
        private int mResource = 0;
        private String[] mFrom = null;
        private int[] mTo = null;

        // devices found list
        private ArrayList<BluetoothDevice> bluetoothDevicesList = new ArrayList<>();
        // devices bonded, including bonded, bonded but absent
        private ArrayList<MyBluetoothDevice> myBluetoothBonded = new ArrayList<>();
        // devices not bonded
        private ArrayList<MyBluetoothDevice> myBluetoothUnBonded = new ArrayList<>();

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
        public MyAdapter(Context context, String[] tag, int tagResource, List<Map<String, Object>> data, int resource, String[] from, int[] to) {
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

        public void initList(List<MyBluetoothDevice> bonded) {
            bluetoothDevicesList.clear();
            myBluetoothBonded.clear();
            myBluetoothBonded.addAll(bonded);
            myBluetoothUnBonded.clear();
            notifyData(tag);
        }

        public void addBLEDevice(MyBluetoothDevice mBluetoothDevice) {
            if (mBluetoothDevice.getDevice().getBondState() != BluetoothDevice.BOND_BONDED)
                if (!mBluetoothDevice.isContain(myBluetoothUnBonded))
                    myBluetoothUnBonded.add(mBluetoothDevice);
                else
                    return;
            if (!mBluetoothDevice.isContain(bluetoothDevicesList)) {
                bluetoothDevicesList.add(mBluetoothDevice.getDevice());
                Log.i(TAG, "addBLEDevice : " + mBluetoothDevice.getName() + " : " + mBluetoothDevice.getAddress());
            } else
                return;
            for (MyBluetoothDevice myBluetoothDevice : myBluetoothBonded) {
                myBluetoothDevice.notifyState(bluetoothDevicesList);
            }
            notifyData(tag);
        }

        private List<Map<String, Object>> notifyDevices(ArrayList<MyBluetoothDevice> bdList) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MyBluetoothDevice bd : bdList) {
                list.add(addBD(bd));
            }
            return list;
        }

        private void notifyData(String tag[]) {
            List<Map<String, Object>> list = new ArrayList<>();
            // don not show this if there is no device bonded
            if (!myBluetoothBonded.isEmpty()) {
                list.add(addBD(tag[0]));
                for (Map<String, Object> map : notifyDevices(myBluetoothBonded)) {
                    list.add(map);
                }
            }
            list.add(addBD(tag[1]));
            for (Map<String, Object> map : notifyDevices(myBluetoothUnBonded)) {
                list.add(map);
            }
            mData.clear();
            mData.addAll(list);
            notifyDataSetChanged();
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

    }
}
