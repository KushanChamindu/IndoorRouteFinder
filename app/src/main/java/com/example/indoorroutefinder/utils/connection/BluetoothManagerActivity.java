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
import com.example.indoorroutefinder.utils.trilateration.Anchor;
import com.example.indoorroutefinder.utils.trilateration.Point;
import com.example.indoorroutefinder.utils.trilateration.SensorManagerActivity;
import com.example.indoorroutefinder.utils.trilateration.Trilateration;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


@RequiresApi(api = Build.VERSION_CODES.R)
public class BluetoothManagerActivity {
    private static BluetoothAdapter mBluetoothAdapter;
    private static ArrayList<Integer> rssi_list = new ArrayList<>();
    private static int count = 0;
    private static final double N = 2.4038;
    private static final double C = 61.3776;
    private static Map<String, Anchor> anchor_list = new HashMap<String, Anchor>() {{
        put("Anchor 1", new Anchor(79.91970864014434, 6.795573572472691, "Anchor 1"));
        put("Anchor 2", new Anchor(79.91979407661779, 6.795534487593358, "Anchor 2"));
        put("Anchor 3", new Anchor(79.91977868141629, 6.795588418609739, "Anchor 3"));
    }};
    private static Point p1;
    private static Point p2;
    private static Point p3;

    @SuppressLint("MissingPermission")
    public static void onReceive(Context context, Intent intent, SymbolManager symbolManager) {
        String action = intent.getAction();
        Log.i("Bluetooth", "  RSSI: " + action + "dBm");
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

            // double distance = ((Math.pow(10,rssi/-20.0)) * 0.125)/(4*Math.PI)/10;
            // n= 2.4038 and c = 61.3776
            double distance = Math.pow(10, (rssi + C) / (-10 * N));
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i("Rassi list", String.valueOf(rssi_list));

            if (count == 30) {
                Double average = rssi_list.stream()
                        .mapToDouble(d -> d)
                        .average()
                        .orElse(0.0);
                Log.i("Rssi list with 30 item", String.valueOf(average));
            } else if (device.getName().equals("Anchor 2")) {
                count += 1;
                rssi_list.add(rssi);
            }

            if (anchor_list.containsKey(device.getName())) {
                anchor_list.get(device.getName()).setDistanceToUser(distance);
                anchor_list.get(device.getName()).setUpdate(true);
                if (device.getName().equals("Anchor 1")) {
                    p1 = new Point(anchor_list.get("Anchor 1").getLat(), anchor_list.get("Anchor 1").getLon(), distance);

                } else if (device.getName().equals("Anchor 2")) {
                    p2 = new Point(anchor_list.get("Anchor 2").getLat(), anchor_list.get("Anchor 2").getLon(), distance);

                } else if (device.getName().equals("Anchor 3")) {
                    p3 = new Point(anchor_list.get("Anchor 3").getLat(), anchor_list.get("Anchor 3").getLon(), distance);
                }
            }

            if (anchor_list.get("Anchor 1").isUpdate() && anchor_list.get("Anchor 2").isUpdate() &&
                    anchor_list.get("Anchor 3").isUpdate()) {
                double[] a = Trilateration.Compute(p1, p2, p3);
                Log.i("User localization 1", String.valueOf(p1.gr()));
                Log.i("User localization 2", String.valueOf(p2.gr()));
                Log.i("User localization 3", String.valueOf(p3.gr()));
                if(a != null){
                // POISelectionActivity.userRelocate(a[0] , a[1] , symbolManager);
                    Log.i("Trilateration", "LatLon: " + a[0] + ", " + a[1]);
                }

                anchor_list.get("Anchor 1").setUpdate(false);
                anchor_list.get("Anchor 2").setUpdate(false);
                anchor_list.get("Anchor 3").setUpdate(false);
            }

//            Point p1 = new Point(anchor_list.get("Anchor 1").getLat(), anchor_list.get("Anchor 1").getLon(), distance);
//            Point p2 = new Point(anchor_list.get("Anchor 2").getLat(), anchor_list.get("Anchor 2").getLon(), distance);
//            Point p3 = new Point(anchor_list.get("Anchor 3").getLat(), anchor_list.get("Anchor 3").getLon(), distance);
//            double[] a = Trilateration.Compute(p1, p2, p3);
//            Log.i("Trilateration", "LatLon: " + a[0] + ", " + a[1]);
//            POISelectionActivity.userRelocate(79.919685, 6.795557, symbolManager);
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
    }

    public static void startDiscovery(Context context) {
        new SensorManagerActivity();
        if (!mBluetoothAdapter.isEnabled()) {
            CommonActivity.showDialog("Warning!", "Please turn on Bluetooth and Try Again.");

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

        // to Discoverable mode on
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
