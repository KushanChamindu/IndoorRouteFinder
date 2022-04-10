package com.example.indoorroutefinder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.appcompat.widget.SearchView;


import com.example.indoorroutefinder.utils.QRReader.QRCodeFoundListener;
import com.example.indoorroutefinder.utils.QRReader.QRCodeImageAnalyzer;
import com.example.indoorroutefinder.utils.common.CommonActivity;
import com.example.indoorroutefinder.utils.connection.BluetoothManagerActivity;
import com.example.indoorroutefinder.utils.map.MapSetupActivity;
import com.example.indoorroutefinder.utils.navigation.NavigationActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.example.indoorroutefinder.utils.search.SearchActivity;
import com.example.indoorroutefinder.utils.trilateration.SensorManagerActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback, Style.OnStyleLoaded {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
    String goeFileName = "convention_hall_lvl_0_Nav_3.geojson";
    private SymbolManager symbolManager;
    private static List<PoiGeoJsonObject> poiList = null;
    private Button routeButton;
    private Button initButton;
    private Button bluetooth_button;
    private Button cameraButton;
    private TextView txtView;
    private int destination;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mOrientationSensor;
    private SensorManagerActivity sensorManagerActivity = new SensorManagerActivity();
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static ProcessCameraProvider cameraProvider;
    private String qrCode;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothManagerActivity.onReceive(context, intent, symbolManager);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.R)
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

        previewView = findViewById(R.id.activity_main_previewView);
        previewView.setVisibility(View.INVISIBLE);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
            @Override
            public void onQRCodeFound(String _qrCode) {
                qrCode = _qrCode;
                Log.i("QR reader", _qrCode);
                findViewById(R.id.search_view).setVisibility(View.VISIBLE);
                findViewById(R.id.floor_level_buttons).setVisibility(View.VISIBLE);
                cameraProvider.unbindAll();
                previewView.setVisibility(View.INVISIBLE);
//                qrCodeFoundButton.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "QR: "+_qrCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void qrCodeNotFound() {
//                Log.i("QR reader", "QR not found");
//                qrCodeFoundButton.setVisibility(View.INVISIBLE);
            }
        }));

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
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
        cameraButton = findViewById(R.id.cameraButton);
        bluetooth_button = findViewById(R.id.bluetoothOn);

        MapSetupActivity.setInitialCamera(mapboxMap);
        GeoJsonSource indoorBuildingSource = new GeoJsonSource("indoor-building", MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        this.loadedStyle.addSource(indoorBuildingSource);
        loadSymbols(this.loadedStyle);
        poiList = POISelectionActivity.loadPOIs(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()), symbolManager);
        MapSetupActivity.loadBuildingLayersIcons(this.loadedStyle, getResources());

        bluetooth_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
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


        cameraButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                if (previewView.getVisibility() == View.INVISIBLE) {
                    previewView.setVisibility(View.VISIBLE);
                    findViewById(R.id.search_view).setVisibility(View.INVISIBLE);
                    findViewById(R.id.floor_level_buttons).setVisibility(View.INVISIBLE);
                    requestCamera();
                } else if (previewView.getVisibility() == View.VISIBLE) {
//                    bindCameraPreview(null);
                    findViewById(R.id.search_view).setVisibility(View.VISIBLE);
                    findViewById(R.id.floor_level_buttons).setVisibility(View.VISIBLE);
                    cameraProvider.unbindAll();
                    previewView.setVisibility(View.INVISIBLE);

                }

            }
        });
        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));
        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        routeButton.setOnClickListener(view -> {
            if (destination != -1) {
                if (routeButton.getText().equals(getResources().getText(R.string.calc_route))) {
                    NavigationActivity.displayRoute(1, destination, mapboxMap);
                    routeButton.setText(R.string.cancel_route);
                } else {
                    NavigationActivity.removeRoute(mapboxMap);
                    MapSetupActivity.hideView(routeButton);
                    txtView.setText("");
                    POISelectionActivity.toggleMarker(null, symbolManager);
                }
            }
        });
        handleSearch(findViewById(R.id.search_view), symbolManager, routeButton, poiList);

//        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));
//        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BluetoothManagerActivity.onActivityResult(requestCode, REQUEST_ENABLE_BT);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadSymbols(Style style) {
        symbolManager = new SymbolManager(mapView, mapboxMap, style);
        symbolManager.setIconAllowOverlap(true);
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

    public void handleSearch(SearchView searchView, SymbolManager symbolManager, Button routeB, List<PoiGeoJsonObject> poiList) {
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search here ......");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString().trim();
                destination = POISelectionActivity.updateSymbol(location, symbolManager, routeB, poiList);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
}