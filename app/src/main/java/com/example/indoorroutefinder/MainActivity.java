package com.example.indoorroutefinder;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorroutefinder.LevelSwitch;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.TelemetryDefinition;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {
    private final String STYLE_URL = "https://tilesservices.webservices.infsoft.com/api/mapstyle/style/";
    private final String API_KEY = "8c97d7c6-0c3a-41de-b67a-fb7628efba79";
    private final String INITIAL_3D = "FALSE";

    private final String POI_LAYER_ID = "locls-pois";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style loadedStyle;

    private int currentLevel = 0;
    private GeoJsonSource routeSource;

    private List<PoiGeoJsonObject> pois = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        TelemetryDefinition telemetry = Mapbox.getTelemetry();
        telemetry.setUserTelemetryRequestState(false);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.getMapAsync(this);

        loadPOIs();
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

        initSource(style);
        setInitialCamera();

        Button levelSwitch = findViewById(R.id.switchLevelButton);
        levelSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLevel = (currentLevel + 1) % 4;
                LevelSwitch.updateLevel(loadedStyle, currentLevel);
            }
        });

        Button routeButton = findViewById(R.id.calcRouteButton);
        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCalcRouteClicked();
            }
        });

        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public boolean onMapClick(@NonNull LatLng point) {
                removeMarkers(mapboxMap);
                Feature selectedFeature = findSelectedFeature(point);
                PoiGeoJsonObject selectedPoi = findClickedPoi(selectedFeature);
                createMarker(selectedPoi, selectedFeature);

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

    private void initSource(Style style) {
        List<Source> sources = style.getSources();
        for (Source source : sources) {
            if (source.getId().contains("route") && source instanceof GeoJsonSource) {
                routeSource = (GeoJsonSource) source;
            }
        }
    }

    private void onCalcRouteClicked() {
        String urlString = "https://routes.webservices.infsoft.com/API/Calc?"
                + "apikey=" + API_KEY
                + "&startlat=" + 49.86739
                + "&startlon=" + 10.89190
                + "&startlevel=" + 0
                + "&endlat=" + 49.86701
                + "&endlon=" + 10.89054
                + "&endlevel=" + 0;

        String rawRouteJson = null;
        try {
            RestCall restCall = new RestCall();
            rawRouteJson = restCall.execute(urlString).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setGeoJson(rawRouteJson);
    }

    private void setGeoJson(String rawRouteJson){
        JSONArray array;
        try {
            array = new JSONArray(rawRouteJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObj = array.getJSONObject(i);
                rawRouteJson = jsonObj.getString("GeoJson");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        routeSource.setGeoJson(rawRouteJson);
    }

    private Feature findSelectedFeature(LatLng point) {
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, POI_LAYER_ID);
        if (features != null && !features.isEmpty()) {
            return features.get(0);
        } else {
            return null;
        }
    }

    private PoiGeoJsonObject findClickedPoi(Feature selectedFeature) {
        if (selectedFeature == null)
            return null;

        String id = selectedFeature.getStringProperty("uid");
        for (PoiGeoJsonObject poi : pois) {
            if (poi.uid.equals(id)) {
                return poi;
            }
        }

        return null;
    }

    private void createMarker(PoiGeoJsonObject selectedPoi, Feature selectedFeature) {
        if (selectedPoi == null || selectedFeature == null)
            return;

        String typeField = selectedPoi.type;
        AnnotationPoint selectedPOI = featureToAnnotationPoint(selectedFeature);

        double lat = selectedPOI.coordinates[1];
        double lon = selectedPOI.coordinates[0];
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(typeField));
        marker.showInfoWindow(mapboxMap, mapView);
    }

    private void removeMarkers(MapboxMap mapboxMap) {
        List<Marker> markers = mapboxMap.getMarkers();
        for (Marker marker : markers) {
            mapboxMap.removeMarker(marker);
        }
    }
    public void loadPOIs() {
        String geojsonbaseURL = "https://tiles.infsoft.com/api/geoobj/json/";
        String icid = "/en/";
        String revision = "0";
        String urlString = geojsonbaseURL + API_KEY + icid + revision;
        String poiGeoJson = null;

        try {
            RestCall restCall = new RestCall();
            poiGeoJson = restCall.execute(urlString).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            pois = objectMapper.readValue(poiGeoJson, new TypeReference<ArrayList<PoiGeoJsonObject>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AnnotationPoint featureToAnnotationPoint(Feature feature) {
        Geometry geometry = feature.geometry();
        AnnotationPoint annotationPoint = null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        try {
            annotationPoint = objectMapper.readValue(geometry.toJson(), AnnotationPoint.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return annotationPoint;
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