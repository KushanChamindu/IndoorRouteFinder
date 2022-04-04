package com.example.indoorroutefinder;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.geometryType;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;
//    private View levelButtons;
    private int currentLevel = 0;
    private final String goeFileName = "convention_hall_lvl_0.geojson";

    // private final String STYLE_URL = "https://tilesservices.webservices.infsoft.com/api/mapstyle/style/";
    // private final String API_KEY = "8c97d7c6-0c3a-41de-b67a-fb7628efba79";
    // private final String INITIAL_3D = "FALSE";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
//        TelemetryDefinition telemetry = Mapbox.getTelemetry();
//        telemetry.setUserTelemetryRequestState(false);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
//        String styleUrl = STYLE_URL + API_KEY + "?config=3d:" + INITIAL_3D;
        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, this);
//        this.mapboxMap.getUiSettings().setAttributionEnabled(false);
//        this.mapboxMap.getUiSettings().setLogoEnabled(false);
    }

    @Override
    public void onStyleLoaded(@NonNull Style style) {
        this.loadedStyle = style;
//        levelButtons = findViewById(R.id.floor_level_buttons);

//        final List<Point> boundingBox = new ArrayList<>();
//
//        boundingBox.add(Point.fromLngLat(79.91980388760567,6.795640228656255));
//        boundingBox.add(Point.fromLngLat(79.91982266306877,6.795542349975353));
//        boundingBox.add(Point.fromLngLat(79.91969928145409,6.795500401963159));
//        boundingBox.add(Point.fromLngLat(79.91967715322971,6.795608934114187));
//
//        boundingBoxList = new ArrayList<>();
//        boundingBoxList.add(boundingBox);

        // LevelSwitch.updateLevel(style,0);
        // DisplayRouteActivity.initSource(style);
        setInitialCamera();
        indoorBuildingSource = new GeoJsonSource(
                "indoor-building", loadJsonFromAsset(goeFileName));
        POISelectionActivity.loadPOIs(loadJsonFromAsset(goeFileName));
        this.loadedStyle.addSource(indoorBuildingSource);
//        List<Layer> layers = style.getLayers();
//        Log.i("Layers", String.valueOf(layers));
//        LevelSwitch.updateLevel(style,0);
        loadBuildingLayer(this.loadedStyle);
        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public boolean onMapClick(@NonNull LatLng point) {
                Feature selectedFeature = POISelectionActivity.findSelectedFeature(mapboxMap, point);
                PoiGeoJsonObject selectedPoi = POISelectionActivity.findClickedPoi(selectedFeature);
                if(selectedFeature != null) {
                    POISelectionActivity.removeMarkers(mapboxMap);
                }
//                Log.i("POISelect", String.valueOf(selectedFeature));
//                Log.i("POISelect", String.valueOf(selectedPoi));
                POISelectionActivity.createMarker(mapView, mapboxMap, loadedStyle,getResources(),selectedPoi, selectedFeature);
//                return true;
                return true;
            }
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

        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // DisplayRouteActivity.onCalcRouteClicked(API_KEY);
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> results = wifiManager.getScanResults();
                // int level = getPowerPercentage(results.get(0).level);
                Log.i("wifi", String.valueOf(results));
            }
        });

        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialCamera();
            }
        });
    }

//    private void hideLevelButton() {
//        // When the user moves away from our bounding box region or zooms out far enough the floor level
//        // buttons are faded out and hidden.
//        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
//        animation.setDuration(500);
//        levelButtons.startAnimation(animation);
//        levelButtons.setVisibility(View.GONE);
//    }
//
//    private void showLevelButton() {
//        // When the user moves inside our bounding box region or zooms in to a high enough zoom level,
//        // the floor level buttons are faded out and hidden.
//        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
//        animation.setDuration(500);
//        levelButtons.startAnimation(animation);
//        levelButtons.setVisibility(View.VISIBLE);
//    }

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
        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill", "indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")),
                // Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
                // necessary to show the indoor map at high zoom levels.
                fillOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));

        style.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line", "indoor-building");
        style.addLayer(indoorBuildingLineLayer);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.location);
        style.addImage("marker", icon);
        SymbolLayer indoorBuildingSymbolLayer = new SymbolLayer("indoor-building-line-symbol", "indoor-building").withProperties(
                PropertyFactory.iconImage("marker")
        );
        indoorBuildingSymbolLayer.setFilter(eq(geometryType(), literal("Point")));
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}