package com.example.indoorroutefinder;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.indoorroutefinder.utils.map.MapSetupActivity;
import com.example.indoorroutefinder.utils.navigation.NavigationActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity<prteccted> extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
    String goeFileName = "convention_hall_lvl_0_Nav_2.geojson";
    private SymbolManager symbolManager;
    private static List<PoiGeoJsonObject> poiList = null;
    private Button routeButton;
    private Button initButton;
    private Button bluetooth_button;
    private TextView txtView;
    private int destination;
    BluetoothAdapter mBlutoothAdapter;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.i("Bluetooth", "  RSSI: " + action + "dBm");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.i("Bluetooth", "  RSSI: " + rssi + "dBm");
                Toast.makeText(getApplicationContext(), "  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.getMapAsync(this);
        //adapter
        mBlutoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBlutoothAdapter == null) {
            Log.i("Bluetooth", "Bluetooth adapter is null");
        } else {
            Log.i("Bluetooth", "Bluetooth adapter is not null");
        }

        if (mBlutoothAdapter.isEnabled()) {
            Log.i("Bluetooth", "Bluetooth is enabled");
        } else {
            Log.i("Bluetooth", "Bluetooth is disable");
        }
        IntentFilter intent_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, intent_filter);
        Log.i("Bluetooth", "receiver "+ String.valueOf(receiver));
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStyleLoaded(@NonNull Style style) {
        this.loadedStyle = style;
        routeButton = findViewById(R.id.calcRouteButton);
        initButton = findViewById(R.id.initButton);
        txtView = findViewById(R.id.stoleText);
        bluetooth_button = findViewById(R.id.bluetoothOn);

        MapSetupActivity.setInitialCamera(mapboxMap);
        GeoJsonSource indoorBuildingSource = new GeoJsonSource("indoor-building", MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        this.loadedStyle.addSource(indoorBuildingSource);
        loadSymbols(this.loadedStyle);
        poiList = POISelectionActivity.loadPOIs(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()), symbolManager);
        MapSetupActivity.loadBuildingLayersIcons(this.loadedStyle, getResources());

        bluetooth_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mBlutoothAdapter.isEnabled()) {
                    Log.i("Bluetooth", "Turning on Bluetooth.....");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                           int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivityForResult(intent, REQUEST_ENABLE_BT);

                } else {
                    Log.i("Bluetooth", "Bluetooth already on");
                }


                //to Discoverable mode on
                Intent intent_discover = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(intent_discover, REQUEST_DISCOVER_BT);

                //Paired devices
                Set<BluetoothDevice> devices = mBlutoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    Log.i("Bluetooth", device.getName()+ " " + device);
                }
                mBlutoothAdapter.startDiscovery();
            }
        });

//        mapboxMap.addOnMapClickListener(point -> {
//            Feature selectedFeature = POISelectionActivity.findSelectedFeature(mapboxMap, point);
//            PoiGeoJsonObject selectedPoi = POISelectionActivity.findClickedPoi(selectedFeature);
//            if(selectedFeature != null) {
//                POISelectionActivity.removeMarkers(mapboxMap);
//            }
//            POISelectionActivity.createMarker(mapView, mapboxMap, loadedStyle,getResources(),selectedPoi, selectedFeature);
//            return true;
//        });

//        mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
//            @Override
//            public void onCameraMove() {
//                if (mapboxMap.getCameraPosition().zoom > 16) {
//                    if (TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
//                            mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))) {
//                        if (levelButtons.getVisibility() != View.VISIBLE) {
//                            showLevelButton();
//                        }
//                    } else {
//                        if (levelButtons.getVisibility() == View.VISIBLE) {
//                            hideLevelButton();
//                        }
//                    }
//                } else if (levelButtons.getVisibility() == View.VISIBLE) {
//                    hideLevelButton();
//                }
//            }
//        });
        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));
        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        routeButton.setOnClickListener(view -> {
            if (routeButton.getText() == getResources().getText(R.string.calc_route)) {
                NavigationActivity.displayRoute(1, destination, mapboxMap);
                routeButton.setText(R.string.cancel_route);
            } else {
                NavigationActivity.removeRoute(mapboxMap);
                MapSetupActivity.hideView(routeButton);
                txtView.setText("");
                POISelectionActivity.toggleMarker(null, symbolManager);
            }
        });

//        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));
//        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (requestCode == RESULT_OK) {
                    Log.i("Bluetooth", "Bluetooth is on ");
                } else {
                    Log.i("Bluetooth", "Couldn't on bluetooth ");
                }
                break;
        }
//        Log.i("Bluetooth", String.valueOf(requestCode) +" "+ resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadSymbols(Style style) {
        symbolManager = new SymbolManager(mapView, mapboxMap, style);
        symbolManager.setIconAllowOverlap(false);
        symbolManager.setTextAllowOverlap(true);
        symbolManager.addClickListener(symbol -> {
            POISelectionActivity.toggleMarker(symbol, symbolManager);
            if (routeButton.getVisibility() != View.VISIBLE) {
                MapSetupActivity.showView(routeButton);
            }
            if (routeButton.getText().equals(getResources().getText(R.string.cancel_route))) {
                routeButton.setText(R.string.calc_route);
            }
            NavigationActivity.removeRoute(mapboxMap);
            txtView.setText(symbol.getTextField());
            // Toast.makeText(getApplicationContext(), "Displaying route to " + symbol.getTextField(), Toast.LENGTH_SHORT).show();
            PoiGeoJsonObject poi = poiList.stream().filter(obj ->
                    String.valueOf(obj.coordinates.get(0)).equals(String.valueOf(symbol.getGeometry().longitude()))
                            && String.valueOf(obj.coordinates.get(1)).equals(String.valueOf(symbol.getGeometry().latitude()))
            ).collect(Collectors.toList()).get(0);
            destination = Integer.parseInt(String.valueOf(poi.props.get("Nav")));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        symbolManager.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}