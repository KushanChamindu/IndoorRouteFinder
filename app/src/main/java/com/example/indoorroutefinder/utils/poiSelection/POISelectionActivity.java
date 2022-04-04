package com.example.indoorroutefinder.utils.poiSelection;

import android.graphics.PointF;
import android.util.Log;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class POISelectionActivity {

    private static final String POI_LAYER_ID = "indoor-building-line-symbol";
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

        String id = selectedFeature.id();
        for (PoiGeoJsonObject poi : pois) {
            if (poi.id.equals(id)) {
                return poi;
            }
        }

        return null;
    }

    public static void createMarker(MapView mapView, MapboxMap mapboxMap, Style style,
                                    android.content.res.Resources resource, PoiGeoJsonObject selectedPoi, Feature selectedFeature) {
        if (selectedPoi == null || selectedFeature == null)
            return;

        String typeField = selectedPoi.props.get("Name");
        AnnotationPoint selectedPOI = featureToAnnotationPoint(selectedFeature);

        double lat = selectedPOI.coordinates[1];
        double lon = selectedPOI.coordinates[0];
        Log.i("POIselect", String.valueOf(lat));
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(typeField));
        marker.showInfoWindow(mapboxMap, mapView);
    }

    public static void removeMarkers(MapboxMap mapboxMap) {
        List<Marker> markers = mapboxMap.getMarkers();
        for (Marker marker : markers) {
            Log.i("POISelect", String.valueOf(marker.getTitle()));
                mapboxMap.removeMarker(marker);

        }
    }

    public static void loadPOIs(String geoJsonSource) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        try {
            Map<String, ArrayList<LinkedHashMap>> map = objectMapper.readValue(geoJsonSource, Map.class);
            ArrayList<LinkedHashMap> pointers = map.get("features");
            pois = new ArrayList<PoiGeoJsonObject>();
            for (LinkedHashMap pointer : pointers) {
                LinkedHashMap geometry = (LinkedHashMap) pointer.get("geometry");
                if (geometry.get("type").equals("Point")) {
                    pois.add(new PoiGeoJsonObject((String) pointer.get("id"), (String) geometry.get("type"), (LinkedHashMap<String, String>) pointer.get("properties"), (ArrayList<String>) geometry.get("coordinates")));
                }
            }
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
