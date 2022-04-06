package com.example.indoorroutefinder.utils.poiSelection;

import android.graphics.PointF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

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
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.expressions.Expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class POISelectionActivity {
    private static final List<SymbolOptions> options = new ArrayList<>();
    private static Symbol lastClickedSymbol=null;

//    public static Feature findSelectedFeature(MapboxMap mapboxMap, LatLng point) {
//        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
//        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, POI_LAYER_ID);
//        if (features != null && !features.isEmpty()) {
//            return features.get(0);
//        } else {
//            return null;
//        }
//    }

//    public static PoiGeoJsonObject findClickedPoi(Feature selectedFeature) {
//        if (selectedFeature == null)
//            return null;
//
//        String id = selectedFeature.id();
//        for (PoiGeoJsonObject poi : pois) {
//            if (poi.id.equals(id)) {
//                return poi;
//            }
//        }
//
//        return null;
//    }

//    public static void createMarker(MapView mapView, MapboxMap mapboxMap, Style style,
//                                    android.content.res.Resources resource, PoiGeoJsonObject selectedPoi, Feature selectedFeature) {
//        if (selectedPoi == null || selectedFeature == null)
//            return;
//
//        String typeField = selectedPoi.props.get("Name");
//        AnnotationPoint selectedPOI = featureToAnnotationPoint(selectedFeature);
//
//        double lat = selectedPOI.coordinates[1];
//        double lon = selectedPOI.coordinates[0];
//        Log.i("POIselect", String.valueOf(lat));
//        Marker marker = mapboxMap.addMarker(new MarkerOptions()
//                .position(new LatLng(lat, lon))
//                .title(typeField));
//        marker.showInfoWindow(mapboxMap, mapView);
//    }

//    public static void removeMarkers(MapboxMap mapboxMap) {
//        List<Marker> markers = mapboxMap.getMarkers();
//        for (Marker marker : markers) {
//            Log.i("POISelect", String.valueOf(marker.getTitle()));
//                mapboxMap.removeMarker(marker);
//
//        }
//    }

    public static List<PoiGeoJsonObject> loadPOIs(String geoJsonSource, SymbolManager symbolManager) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        List<PoiGeoJsonObject> poiList = null;
        try {
            Map<String, ArrayList<LinkedHashMap>> map = objectMapper.readValue(geoJsonSource, Map.class);
            ArrayList<LinkedHashMap> pointers = map.get("features");
            poiList = new ArrayList<>();
            for (LinkedHashMap pointer : pointers) {
                LinkedHashMap geometry = (LinkedHashMap) pointer.get("geometry");
                LinkedHashMap properties = (LinkedHashMap) pointer.get("properties");
                ArrayList<String> coordinates = (ArrayList<String>) geometry.get("coordinates");
                if (properties.get("point-type")!= null && properties.get("point-type").equals("stole")) {
                    poiList.add(new PoiGeoJsonObject((String) pointer.get("id"), (String) geometry.get("type"), (LinkedHashMap<String, String>) pointer.get("properties"), coordinates));
                    Log.i("Print", String.valueOf(coordinates));
                    options.add(new SymbolOptions()
                            .withLatLng(new LatLng(Double.parseDouble(String.valueOf(coordinates.get(1))), Double.parseDouble(String.valueOf(coordinates.get(0)))))
                            .withIconImage("marker")
                            .withTextField((String) properties.get("Name"))
                            .withTextAnchor("top")
                            .withTextOffset(new Float[] {0f, 1.5f})
                            .withIconSize(1f)
                            .withIconOffset(new Float[] {0f,-1.5f})
                    );
                }
            }
            List<Symbol> symbols = symbolManager.create(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return poiList;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void toggleMarker(Symbol symbolToUpdate, SymbolManager symbolManager){
        if (symbolToUpdate == null){
            lastClickedSymbol.setIconImage("marker");
            symbolManager.update(lastClickedSymbol);
            lastClickedSymbol = null;
        }
        else if (symbolToUpdate.getIconImage().equals("marker")) {
            if (lastClickedSymbol != null){
                lastClickedSymbol.setIconImage("marker");
            }
            symbolToUpdate.setIconImage("redMarker");
            symbolManager.update(symbolToUpdate);
            lastClickedSymbol = symbolToUpdate;
        }

//        symbolManager.delete(symbols);
////        Symbol newSymbol = symbols.stream().filter(symbol -> symbol==symbolToUpdate).collect(Collectors.toList()).get(0);
//        symbolManager.update(symbols);
    }

//    private static AnnotationPoint featureToAnnotationPoint(Feature feature) {
//        Geometry geometry = feature.geometry();
//        AnnotationPoint annotationPoint = null;
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        try {
//            annotationPoint = objectMapper.readValue(geometry.toJson(), AnnotationPoint.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//        return annotationPoint;
//    }
}
