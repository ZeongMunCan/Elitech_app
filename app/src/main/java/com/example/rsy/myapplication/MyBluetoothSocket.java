package com.example.rsy.myapplication;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by rsy on 2015/8/9.
 */
public class MyBluetoothSocket extends Thread {

    final String LOG_TAG = "MyBluetoothSocket";

    private BluetoothSocket bluetoothSocket = null;
    private BufferedWriter out = null;
    private BufferedReader in = null;
    private Handler handler = null;
    public boolean isConnected = false;
    private boolean exit = false;

    public MyBluetoothSocket(BluetoothSocket bs, Handler handler) {
        this.bluetoothSocket = bs;
        this.handler = handler;
    }

    public boolean connect() {
        // 如果蓝牙已经连接了，就不再次进行连接
        if (!isConnected)
            try {
                bluetoothSocket.connect();
                out = new BufferedWriter(new OutputStreamWriter(bluetoothSocket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                isConnected = true;
                return true;
            } catch (IOException e) {
                Message message = new Message();
                message.what = -1;
                message.obj = "";
                handler.sendMessage(message);
                e.printStackTrace();
                return false;
            }
        else
            return false;
    }

    public int write(String writeBuff) {
        if (!isConnected) {
            Log.i(LOG_TAG, "bts not connected");
            return -1;
        }
        if (out == null) return -2;
        try {
            out.write(writeBuff + "\n");
            out.flush();
            return writeBuff.length();
        } catch (IOException e) {
            e.printStackTrace();
            return -3;
        }
    }

    @Override
    public void run() {
        super.run();
        String read = "";
        while (!exit) {
            if (!isConnected) {
                Log.i(LOG_TAG, "in reading thread bts not connected");
                break;
            }
            if (in == null) {
                Log.i(LOG_TAG, "bts in is null");
                break;
            }
            try {
                Log.i(LOG_TAG, "reading...");
                read = in.readLine();
            } catch (IOException e) {
                exit = true;
                e.printStackTrace();
            }
            Log.i(LOG_TAG, "handler time");
            if (read.equals("exit") || exit) {
                try {
                    this.exit();
                    Log.i(LOG_TAG, "get exit signal");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!read.isEmpty()) {
                // msg may be occupied, so new one every time
                Message message = new Message();
                message.what = 1;
                message.obj = read;
                handler.sendMessage(message);
                message.recycle();
            }
        }
    }

    public void exit() throws IOException {
        exit = true;
        bluetoothSocket.close();
        if (isConnected) {
            isConnected = false;
            Message message = new Message();
            message.what = -2;
            message.obj = "";
            handler.sendMessage(message);
            Log.i(LOG_TAG, "bts exit...");
        }
    }

}
