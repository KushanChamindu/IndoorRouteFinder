package com.example.indoorroutefinder.utils.poiSelection;

import android.graphics.PointF;

import com.example.indoorroutefinder.utils.restCall.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class POISelectionActivity {

    private static final String POI_LAYER_ID = "locls-pois";
    private static List<PoiGeoJsonObject> pois = null;

    public static Feature findSelectedFeature(MapboxMap mapboxMap, LatLng point) {
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, POI_LAYER_ID);
        if (features != null && !features.isEmpty()) {
            return features.get(0);
        } else {
            return null;
        }
    }

    public static PoiGeoJsonObject findClickedPoi(Feature selectedFeature) {
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

    public static void createMarker(MapView mapView, MapboxMap mapboxMap, PoiGeoJsonObject selectedPoi, Feature selectedFeature) {
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

    public static void removeMarkers(MapboxMap mapboxMap) {
        List<Marker> markers = mapboxMap.getMarkers();
        for (Marker marker : markers) {
            mapboxMap.removeMarker(marker);
        }
    }

    public static void loadPOIs(String API_KEY) {
        String geojsonbaseURL = "https://tiles.infsoft.com/api/geoobj/json/";
        String icid = "/en/";
        String revision = "0";
        String urlString = geojsonbaseURL + API_KEY + icid + revision;
        String poiGeoJson = null;

        try {
            RestCall restCall = new RestCall();
            poiGeoJson = restCall.execute(urlString).get();
        } catch (ExecutionException | InterruptedException e) {
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

    private static AnnotationPoint featureToAnnotationPoint(Feature feature) {
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
}
