package com.example.indoorroutefinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.utils.common.CommonActivity;
import com.example.indoorroutefinder.utils.connection.BluetoothManagerActivity;
import com.example.indoorroutefinder.utils.map.MapSetupActivity;
import com.example.indoorroutefinder.utils.navigation.NavigationActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.example.indoorroutefinder.utils.search.SearchActivity;
import com.example.indoorroutefinder.utils.trilateration.SensorManagerActivity;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback, Style.OnStyleLoaded {

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
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mOrientationSensor;
    private SensorManagerActivity sensorManagerActivity = new SensorManagerActivity();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothManagerActivity.onReceive(context, intent, symbolManager);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.getMapAsync(this);
        context = getApplicationContext();
        CommonActivity.initializeAlertBuilder(MainActivity.this);
        //initialize adapter
        IntentFilter intent_filter = BluetoothManagerActivity.initializeAdapter();
        registerReceiver(receiver, intent_filter);
        Log.i("Bluetooth", "receiver " + String.valueOf(receiver));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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
                BluetoothManagerActivity.startDiscovery(context);
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
        SearchActivity.handleSearch(findViewById(R.id.search_view), symbolManager);

//        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));
//        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BluetoothManagerActivity.onActivityResult(requestCode, REQUEST_ENABLE_BT);
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
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // ...and the orientation sensor
        mSensorManager.registerListener(this, mOrientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
        mSensorManager.unregisterListener(this);
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
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        sensorManagerActivity.onSensorChanged(sensorEvent, symbolManager);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}