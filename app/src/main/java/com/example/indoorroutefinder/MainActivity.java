package com.example.indoorroutefinder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.utils.common.LevelSwitch;
import com.example.indoorroutefinder.utils.displayRoute.DisplayRouteActivity;
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
import com.mapbox.mapboxsdk.maps.TelemetryDefinition;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {
    private final String STYLE_URL = "https://tilesservices.webservices.infsoft.com/api/mapstyle/style/";
    private final String API_KEY = "8c97d7c6-0c3a-41de-b67a-fb7628efba79";
    private final String INITIAL_3D = "FALSE";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;

    private int currentLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        TelemetryDefinition telemetry = Mapbox.getTelemetry();
        telemetry.setUserTelemetryRequestState(false);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.getMapAsync(this);

        POISelectionActivity.loadPOIs(API_KEY);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        String styleUrl = STYLE_URL + API_KEY + "?config=3d:" + INITIAL_3D;
        this.mapboxMap.setStyle(new Style.Builder().fromUri(styleUrl), this);

        this.mapboxMap.getUiSettings().setAttributionEnabled(false);
        this.mapboxMap.getUiSettings().setLogoEnabled(false);
    }

    @Override
    public void onStyleLoaded(@NonNull Style style) {
        this.loadedStyle = style;
        LevelSwitch.updateLevel(style,0);

        DisplayRouteActivity.initSource(style);
        setInitialCamera();

        Button levelSwitch = findViewById(R.id.switchLevelButton);
        Button routeButton = findViewById(R.id.calcRouteButton);

        levelSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLevel = (currentLevel + 1) % 4;
                LevelSwitch.updateLevel(loadedStyle, currentLevel);
            }
        });

        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DisplayRouteActivity.onCalcRouteClicked(API_KEY);
            }
        });

        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public boolean onMapClick(@NonNull LatLng point) {
                POISelectionActivity.removeMarkers(mapboxMap);
                Feature selectedFeature = POISelectionActivity.findSelectedFeature(mapboxMap, point);
                PoiGeoJsonObject selectedPoi = POISelectionActivity.findClickedPoi(selectedFeature);
                POISelectionActivity.createMarker(mapView, mapboxMap, selectedPoi, selectedFeature);

                return true;
            }
        });
    }

    private void setInitialCamera(){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(49.867630660511715, 10.89075028896332)) // Sets the new camera position
                .zoom(18) // Sets the zoom
                .bearing(0) // Rotate the camera
                .tilt(45) // Set the camera tilt
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