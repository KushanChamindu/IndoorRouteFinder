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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
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
import com.example.indoorroutefinder.utils.search.POI;
import com.example.indoorroutefinder.utils.search.ListViewAdapter;
import com.example.indoorroutefinder.utils.trilateration.SensorManagerActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback, Style.OnStyleLoaded{

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
    String goeFileName = "convention_hall_lvl_0_Nav_3.geojson";
    private SymbolManager symbolManager;
    private static List<PoiGeoJsonObject> poiList = null;
    private static List<PoiGeoJsonObject> NavList = null;
    private Button routeButton;
    private ImageButton initButton;
    private Button bluetooth_button;
    private ImageButton cameraButton;
    private TextView txtView;
    private SearchView searchView;
    private int destination;
    private int scource;
    private static final int REQUEST_ENABLE_BT = 0;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mOrientationSensor;
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static ProcessCameraProvider cameraProvider;
    private String qrCode;
    private Symbol scr_loc;
    private Symbol des_loc;
    private boolean isRouteDisplay = false;
    private ListView suggestionList;
    private ListViewAdapter adapter;
    ArrayList<POI> arrayPOIList = new ArrayList<>();
    private final SensorManagerActivity sensorManagerActivity = new SensorManagerActivity();

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

        //initialize bluetooth management
        IntentFilter intent_filter = BluetoothManagerActivity.initializeAdapter();
        registerReceiver(receiver, intent_filter);
        Log.i("Bluetooth", "receiver " + String.valueOf(receiver));

        //initialize sensors
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //views for QR code scanning
        previewView = findViewById(R.id.activity_main_previewView);
        previewView.setVisibility(View.INVISIBLE);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
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
        searchView = findViewById(R.id.search_view);

        MapSetupActivity.setInitialCamera(mapboxMap);
        GeoJsonSource indoorBuildingSource = new GeoJsonSource("indoor-building", MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        this.loadedStyle.addSource(indoorBuildingSource);
        loadSymbols(this.loadedStyle);
        MapSetupActivity.loadBuildingLayersIcons(this.loadedStyle, getResources());

        poiList = POISelectionActivity.loadPOIs(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()), symbolManager);
        mapboxMap.getUiSettings().setCompassEnabled(false);

        mapboxMap.addOnMapClickListener(point -> {
            if (!searchView.isIconified()) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
            }
            return true;
        });

        bluetooth_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                BluetoothManagerActivity.startDiscovery(context);
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                if (previewView.getVisibility() == View.INVISIBLE) {
                    previewView.setVisibility(View.VISIBLE);
                    findViewById(R.id.search_view).setVisibility(View.INVISIBLE);
                    findViewById(R.id.bottom_buttons).setVisibility(View.INVISIBLE);
                    findViewById(R.id.initButton).setVisibility(View.INVISIBLE);
                    requestCamera();
                } else if (previewView.getVisibility() == View.VISIBLE) {
//                    bindCameraPreview(null);
                    findViewById(R.id.search_view).setVisibility(View.VISIBLE);
                    findViewById(R.id.bottom_buttons).setVisibility(View.VISIBLE);
                    findViewById(R.id.initButton).setVisibility(View.VISIBLE);
                    cameraProvider.unbindAll();
                    previewView.setVisibility(View.INVISIBLE);

                }

            }
        });

        initButton.setOnClickListener(view -> MapSetupActivity.setInitialCamera(mapboxMap));

        routeButton.setOnClickListener(view -> {
            if (destination != -1) {
                if (routeButton.getText().equals(getResources().getText(R.string.calc_route))) {
                    NavigationActivity.displayRoute(scource, destination, mapboxMap);
                    isRouteDisplay = true;
                    routeButton.setText(R.string.cancel_route);
                } else {
                    isRouteDisplay = false;
                    NavigationActivity.removeRoute(mapboxMap);
                    MapSetupActivity.hideView(routeButton);
                    txtView.setText("");
                    POISelectionActivity.toggleMarker(null, symbolManager);
                }
            }
        });

        handleSearch(searchView, symbolManager, routeButton, poiList);

        NavigationActivity.initNav(MapSetupActivity.loadJsonFromAsset(goeFileName, getAssets()));
        NavList = NavigationActivity.getNavPoints();

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

    // ======================================== Map setup Utils ========================================
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadSymbols(Style style) {

        symbolManager = new SymbolManager(mapView, mapboxMap, style);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setTextAllowOverlap(true);
        symbolManager.addClickListener(symbol -> {
            PoiGeoJsonObject scr_poi = null;
            PoiGeoJsonObject des_poi = null;
            if (scr_loc == null) {
                if (isRouteDisplay) {
                    isRouteDisplay = false;
                    NavigationActivity.removeRoute(mapboxMap);
                    MapSetupActivity.hideView(routeButton);
                    txtView.setText("");
                    POISelectionActivity.toggleMarker(null, symbolManager);
                }
                scr_loc = symbol;
                POISelectionActivity.toggleMarker(symbol, symbolManager);
            } else if (des_loc == null) {
                des_loc = symbol;
                POISelectionActivity.toggleMarker(symbol, symbolManager);
            } else if (scr_loc.equals(des_loc)) {
                POISelectionActivity.toggleMarker(symbol, symbolManager);
            }

            if (scr_loc != null && des_loc != null) {
                if (routeButton.getVisibility() != View.VISIBLE) {
                    MapSetupActivity.showView(routeButton);
                }
                if (routeButton.getText().equals(getResources().getText(R.string.cancel_route))) {
                    routeButton.setText(R.string.calc_route);
                }
                NavigationActivity.removeRoute(mapboxMap);
                txtView.setText(symbol.getTextField());
                scr_poi = NavList.stream().filter(obj ->
                        String.valueOf(obj.coordinates.get(0)).equals(String.valueOf(scr_loc.getGeometry().longitude()))
                                && String.valueOf(obj.coordinates.get(1)).equals(String.valueOf(scr_loc.getGeometry().latitude()))
                ).collect(Collectors.toList()).get(0);
                des_poi = NavList.stream().filter(obj ->
                        String.valueOf(obj.coordinates.get(0)).equals(String.valueOf(des_loc.getGeometry().longitude()))
                                && String.valueOf(obj.coordinates.get(1)).equals(String.valueOf(des_loc.getGeometry().latitude()))
                ).collect(Collectors.toList()).get(0);
                scr_loc = null;
                des_loc = null;
            }
            if (scr_poi != null && des_poi != null) {
                destination = Integer.parseInt(String.valueOf(des_poi.props.get("Nav")));
                scource = Integer.parseInt(String.valueOf(scr_poi.props.get("Nav")));
                scr_poi = null;
                des_poi = null;

            }
        });

    }

    // ======================================== Sensor Utils ========================================
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        sensorManagerActivity.onSensorChanged(sensorEvent, symbolManager);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // ======================================== Bluetooth Utils ========================================
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothManagerActivity.onReceive(context, intent, symbolManager);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BluetoothManagerActivity.onActivityResult(requestCode, REQUEST_ENABLE_BT);
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ======================================== Search Utils ========================================
    public void handleSearch(SearchView searchView, SymbolManager symbolManager, Button routeB, List<PoiGeoJsonObject> poiList) {
        searchView.setIconifiedByDefault(true);
        searchView.setQueryHint("Search here .....");
        searchView.onWindowFocusChanged(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);

        for (int i = 0; i < poiList.size(); i++) {
            POI POINames = new POI(poiList.get(i).props.get("Name").replace("_lvl_0", ""));
            // Binds all strings into an array
            arrayPOIList.add(POINames);
        }
        // Locate the ListView in listview_main.xml
        suggestionList = (ListView) findViewById(R.id.suggestionForSearch);
        // Pass results to ListViewAdapter Class
        adapter = new ListViewAdapter(this, arrayPOIList);
        // Binds the Adapter to the ListView
        suggestionList.setAdapter(adapter);
        suggestionList.setVisibility(View.INVISIBLE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString().trim();
                suggestionList.setVisibility(View.INVISIBLE);
                ArrayList<Object> result = POISelectionActivity.updateSymbol(location, symbolManager, routeB, poiList);
                if ((Integer) result.get(0) == -1) {
                    Toast.makeText(context, "Search location not found", Toast.LENGTH_SHORT).show();
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                    return false;
                } else {
                    if (isRouteDisplay) {
                        isRouteDisplay = false;
                        NavigationActivity.removeRoute(mapboxMap);
                        MapSetupActivity.hideView(routeB);
                        txtView.setText("");
                        POISelectionActivity.toggleMarker(null, symbolManager);
                        scr_loc = null;
                        des_loc = null;
                    }
                    if (scr_loc == null) {
                        scource = (Integer) result.get(0);
                        scr_loc = (Symbol) result.get(1);
                    } else if (des_loc == null) {
                        destination = (Integer) result.get(0);
                        des_loc = (Symbol) result.get(1);
                    }
                    if (scr_loc != null && des_loc != null) {
                        if (routeButton.getVisibility() != View.VISIBLE) {
                            MapSetupActivity.showView(routeButton);
                        }
                        if (routeButton.getText().equals(getResources().getText(R.string.cancel_route))) {
                            routeButton.setText(R.string.calc_route);
                        }
                        scr_loc = null;
                        des_loc = null;
                    }
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                    return false;
                }
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String text = newText;
                adapter.filter(text);
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                suggestionList.setVisibility(View.INVISIBLE);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setFocusable(true);
                suggestionList.setVisibility(View.VISIBLE);
                searchView.requestFocusFromTouch();
            }
        });
        suggestionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onItemClick(AdapterView<?> listView, View itemView, int itemPosition, long itemId)
            {
                String location = poiList.get(itemPosition).props.get("Name").replace("_lvl_0", "");
                suggestionList.setVisibility(View.INVISIBLE);
                ArrayList<Object> result = POISelectionActivity.updateSymbol(location, symbolManager, routeB, poiList);
                if ((Integer) result.get(0) == -1) {
                    Toast.makeText(context, "Search location not found", Toast.LENGTH_SHORT).show();
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                } else {

                    if (isRouteDisplay) {
                        isRouteDisplay = false;
                        NavigationActivity.removeRoute(mapboxMap);
                        MapSetupActivity.hideView(routeB);
                        txtView.setText("");
                        POISelectionActivity.toggleMarker(null, symbolManager);
                        scr_loc = null;
                        des_loc = null;
                    }
                    if (scr_loc == null) {
                        scource = (Integer) result.get(0);
                        scr_loc = (Symbol) result.get(1);
                    } else if (des_loc == null) {
                        destination = (Integer) result.get(0);
                        des_loc = (Symbol) result.get(1);
                    }
                    if (scr_loc != null && des_loc != null) {
                        if (routeButton.getVisibility() != View.VISIBLE) {
                            MapSetupActivity.showView(routeButton);
                        }
                        if (routeButton.getText().equals(getResources().getText(R.string.cancel_route))) {
                            routeButton.setText(R.string.calc_route);
                        }
                        scr_loc = null;
                        des_loc = null;
                    }
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                }

            }
        });
    }

    // ======================================== Camera utils ========================================
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
                findViewById(R.id.bottom_buttons).setVisibility(View.VISIBLE);
                findViewById(R.id.initButton).setVisibility(View.VISIBLE);
                cameraProvider.unbindAll();
                previewView.setVisibility(View.INVISIBLE);
//                qrCodeFoundButton.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "QR: " + _qrCode, Toast.LENGTH_SHORT).show();
                double lat = Double.parseDouble(qrCode.split(",")[0]);
                double lon = Double.parseDouble(qrCode.split(",")[1]);
                POISelectionActivity.userRelocate(lat, lon, symbolManager);
            }
            @Override
            public void qrCodeNotFound() {
                Toast.makeText( getApplicationContext(), "Scan QR code properly", Toast.LENGTH_SHORT).show();
//                Log.i("QR reader", "QR not found");
//                qrCodeFoundButton.setVisibility(View.INVISIBLE);
            }
        }));

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

}