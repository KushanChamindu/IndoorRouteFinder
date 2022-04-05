package com.example.indoorroutefinder;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.utils.navigation.NavigationActivity;
import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

    private GeoJsonSource indoorBuildingSource;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
    String goeFileName = "convention_hall_lvl_0_Nav_1.geojson";

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
        indoorBuildingSource = new GeoJsonSource(
                "indoor-building", loadJsonFromAsset(goeFileName));
        POISelectionActivity.loadPOIs(loadJsonFromAsset(goeFileName));
        this.loadedStyle.addSource(indoorBuildingSource);
        loadBuildingLayer(this.loadedStyle);
        mapboxMap.addOnMapClickListener(point -> {
            Feature selectedFeature = POISelectionActivity.findSelectedFeature(mapboxMap, point);
            PoiGeoJsonObject selectedPoi = POISelectionActivity.findClickedPoi(selectedFeature);
            if(selectedFeature != null) {
                POISelectionActivity.removeMarkers(mapboxMap);
            }
            POISelectionActivity.createMarker(mapView, mapboxMap, loadedStyle,getResources(),selectedPoi, selectedFeature);
            return true;
        });

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

//        levelSwitch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                currentLevel = (currentLevel + 1) % 2;
//                if (currentLevel==0){
//                    indoorBuildingSource.setGeoJson(loadJsonFromAsset("convention_hall_lvl_0.geojson"));
////                    loadBuildingLayer(loadedStyle);
//                } else {
//                    indoorBuildingSource.setGeoJson(loadJsonFromAsset("map.geojson"));
//                }
//                // LevelSwitch.updateLevel(loadedStyle, currentLevel);
//            }
//        });

        routeButton.setOnClickListener(view -> {
            // DisplayRouteActivity.onCalcRouteClicked(API_KEY);
//                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//                List<ScanResult> results = wifiManager.getScanResults();
//                // int level = getPowerPercentage(results.get(0).level);
//                Log.i("wifi", String.valueOf(results));
            int source = 1, dest = 11;
            NavigationActivity.getShortestPath(source, dest, mapboxMap);
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
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void loadBuildingLayer(@NonNull Style style) {
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
        SymbolLayer indoorBuildingSymbolLayer = new SymbolLayer("indoor-building-line-symbol", "indoor-building").withProperties(
                PropertyFactory.iconImage("marker")
        );
        indoorBuildingSymbolLayer.setFilter(eq(get("point-type"), literal("stole")));
        style.addLayer(indoorBuildingSymbolLayer);
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