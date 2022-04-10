package com.example.indoorroutefinder.utils.connection;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.indoorroutefinder.utils.common.CommonActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.trilateration.Point;
import com.example.indoorroutefinder.utils.trilateration.SensorManagerActivity;
import com.example.indoorroutefinder.utils.trilateration.Trilateration;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;

import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothManagerActivity {
    private static BluetoothAdapter mBluetoothAdapter;
    private static ArrayList<Integer> rssi_list = new ArrayList<>();
    private static int count=0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    public static void onReceive(Context context, Intent intent, SymbolManager symbolManager) {
        String action = intent.getAction();
        Log.i("Bluetooth", "  RSSI: " + action + "dBm");
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

//            double distance = ((Math.pow(10,rssi/-20.0)) * 0.125)/(4*Math.PI)/10;
            double distance = Math.pow(10,(rssi + 45.6714)/ (-10*4.7375));
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            Log.i("Bluetooth", "Device: " + device.getName() + "  RSSI: " + rssi + "dBm \n distance ::" + distance);
//            Toast.makeText(context, "Device: " + device.getName() + "  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
            Log.i("Rassi list", String.valueOf(rssi_list));
            if(count == 30){
                Double average = rssi_list.stream()
                        .mapToDouble(d -> d)
                        .average()
                        .orElse(0.0);
                Log.i("Rssi list with 30 item", String.valueOf(average));
            }else if(device.getName().equals("Anchor 2")){
                count+=1;
                rssi_list.add(rssi);
            }


            Point p1=new Point(-19.6685,-69.1942,84);
            Point p2=new Point(-20.2705,-70.1311,114);
            Point p3=new Point(-20.5656,-70.1807,120);
            double[] a= Trilateration.Compute(p1,p2,p3);
            Log.i("Trilateration", "LatLon: "+a[0]+", "+a[1]);
            POISelectionActivity.userRelocate(79.919685 , 6.795557, symbolManager);
        }
    }

    public static IntentFilter initializeAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i("Bluetooth", "Bluetooth adapter is null");
        } else {
            Log.i("Bluetooth", "Bluetooth adapter is not null");
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.i("Bluetooth", "Bluetooth is enabled");
        } else {
            Log.i("Bluetooth", "Bluetooth is disable");
        }
        IntentFilter intent_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        return intent_filter;
    }

    public static void onActivityResult(int requestCode, int REQUEST_ENABLE_BT) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (requestCode == RESULT_OK) {
                Log.i("Bluetooth", "Bluetooth is on ");
            } else {
                Log.i("Bluetooth", "Couldn't on bluetooth ");
            }
        }
        // Log.i("Bluetooth", String.valueOf(requestCode) +" "+ resultCode);
    }

    public static void startDiscovery(Context context) {
        new SensorManagerActivity();
        if (!mBluetoothAdapter.isEnabled()) {
            CommonActivity.showDialog("Warning!", "Please turn on Bluetooth and Try Again.");
            // Log.i("Bluetooth", "Turning on Bluetooth.....");
            // Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                           int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            // startActivityForResult(intent, REQUEST_ENABLE_BT);
        } else {
            Log.i("Bluetooth", "Bluetooth already on");
        }

        //to Discoverable mode on
        // Intent intent_discover = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        // startActivityForResult(intent_discover, REQUEST_DISCOVER_BT);

        // Get paired devices
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            Log.i("Bluetooth", device.getName() + " " + device);
        }
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                //Here you can use handler or whatever you want to use.
                Log.i("Bluetooth", "Discovery Starting");
                mBluetoothAdapter.startDiscovery();
            }
        }, 0, 15000);
    }
}
