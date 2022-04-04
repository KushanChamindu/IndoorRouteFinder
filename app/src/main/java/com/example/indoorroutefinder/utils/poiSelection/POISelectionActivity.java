package com.example.indoorroutefinder.utils.poiSelection;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.e;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.geometryType;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;

import android.graphics.PointF;
import android.util.Log;

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
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        String id = selectedFeature.getStringProperty("id");
        for (PoiGeoJsonObject poi : pois) {
            if (poi.id.equals(id)) {
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

    public static void loadPOIs(String geoJsonSource) {
//        Log.i("POIselection", String.valueOf(geoJsonSource));
//        List<Feature> poiGeoJson = geoJsonSource.querySourceFeatures(Expression.all());
//        Log.i("POIselection", String.valueOf(geoJsonSource));
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
            Log.i("POIselection", String.valueOf(pois));
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
