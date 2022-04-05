package com.example.indoorroutefinder;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.utils.navigation.NavigationActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
    String goeFileName = "convention_hall_lvl_0_Nav_2.geojson";
    private SymbolManager symbolManager;
    private static List<PoiGeoJsonObject> poiList = null;
    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                Toast.makeText(getApplicationContext(),"  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
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
        setInitialCamera();
        GeoJsonSource indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset(goeFileName));
        symbolManager = new SymbolManager(mapView, mapboxMap, style);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setTextAllowOverlap(true);
        symbolManager.addClickListener(symbol -> {
            Toast.makeText(getApplicationContext(), "Displaying route to " + symbol.getTextField(), Toast.LENGTH_SHORT).show();
            PoiGeoJsonObject poi = poiList.stream().filter(obj ->
                    String.valueOf(obj.coordinates.get(0)).equals(String.valueOf(symbol.getGeometry().longitude()))
                            && String.valueOf(obj.coordinates.get(1)).equals(String.valueOf(symbol.getGeometry().latitude()))
            ).collect(Collectors.toList()).get(0);
            int source = 1, dest = Integer.parseInt(String.valueOf(poi.props.get("Nav")));
            NavigationActivity.displayRoute(source, dest, mapboxMap);
        });

        this.loadedStyle.addSource(indoorBuildingSource);
        poiList = POISelectionActivity.loadPOIs(loadJsonFromAsset(goeFileName), symbolManager);
        loadBuildingLayers(this.loadedStyle);

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

//        Button levelSwitch = findViewById(R.id.switchLevelButton);

        Button routeButton = findViewById(R.id.calcRouteButton);
        Button initButton = findViewById(R.id.initButton);

        routeButton.setOnClickListener(view -> {
//            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//            List<ScanResult> results = wifiManager.getScanResults();
//            // int level = getPowerPercentage(results.get(0).level);
//            Log.i("wifi", String.valueOf(results));
        });

        initButton.setOnClickListener(view -> setInitialCamera());
        NavigationActivity.initNav(loadJsonFromAsset(goeFileName));
    }

    private String loadJsonFromAsset(String filename) {
        // Using this method to load in GeoJSON files from the assets folder.
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void loadBuildingLayers(@NonNull Style style) {
        // Method used to load the indoor layer on the map. First the fill layer is drawn and then the
        // line layer is added.
//        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill", "indoor-building").withProperties(
//                fillColor(Color.parseColor("#eeeeee")),
//                // Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
//                // necessary to show the indoor map at high zoom levels.
//                fillOpacity(interpolate(exponential(0.2f), zoom(),
//                        stop(16f, 0f),
//                        stop(16.5f, 0.5f),
//                        stop(17f, 1f)))
//        );
//
//        style.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line", "indoor-building").withProperties(
                lineColor(Color.parseColor("#50667f")),
                lineWidth(3f),
                lineOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));
        style.addLayer(indoorBuildingLineLayer);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.location);
        style.addImage("marker", icon);
//        SymbolLayer indoorBuildingSymbolLayer = new SymbolLayer("indoor-building-line-symbol", "indoor-building").withProperties(
//                PropertyFactory.iconImage("marker")
//        );
//        indoorBuildingSymbolLayer.setFilter(eq(get("point-type"), literal("stole")));
//        style.addLayer(indoorBuildingSymbolLayer);
    }

    private void setInitialCamera(){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(6.795577, 79.91975)) // Sets the new camera position
                .zoom(21.3) // Sets the zoom
                .bearing(80) // Rotate the camera
                .tilt(0) // Set the camera tilt
                .build();
        this.mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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