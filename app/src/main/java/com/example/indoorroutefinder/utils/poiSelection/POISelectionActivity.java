package com.example.indoorroutefinder.utils.poiSelection;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;

import com.example.indoorroutefinder.utils.map.MapSetupActivity;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class POISelectionActivity {
    private static final List<SymbolOptions> options = new ArrayList<>();
    private static Symbol lastClickedSymbol = null;
    private static Symbol userLoc = null;
    private static List<Symbol> symbols;

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
                if (properties.get("point-type") != null && properties.get("point-type").equals("stole")) {
                    poiList.add(new PoiGeoJsonObject((String) pointer.get("id"), (String) geometry.get("type"), (LinkedHashMap<String, String>) pointer.get("properties"), coordinates));
                    Log.i("Print", String.valueOf(coordinates));
                    String pointName = Objects.requireNonNull(properties.get("Name")).toString().replace("_lvl_0", "");
                    assert coordinates != null;
                    options.add(new SymbolOptions()
                                    .withLatLng(new LatLng(Double.parseDouble(String.valueOf(coordinates.get(1))), Double.parseDouble(String.valueOf(coordinates.get(0)))))
                                    .withIconImage("marker")
//                            .withTextField((String) properties.get("Name"))
                                    .withTextField(pointName.substring(0, 1).toUpperCase() + pointName.substring(1))
                                    .withTextAnchor("top")
                                    .withTextOffset(new Float[]{0f, 1.5f})
                                    .withIconSize(1f)
                                    .withIconOffset(new Float[]{0f, -1.5f})
                    );
                }
            }
            options.add(new SymbolOptions()
                    .withLatLng(new LatLng(6.795565, 79.919774))
                    .withIconImage("UserLoc")
                    .withTextField("")
                    .withTextOffset(new Float[]{0f, 1.5f})
                    .withIconSize(1f)
                    .withIconOffset(new Float[]{0f, -1.5f}));
            symbols = symbolManager.create(options);
            userLoc = symbols.get(symbols.size() - 1);
//            userLoc.setIconRotate((float) headDirection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return poiList;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void toggleMarker(Symbol symbolToUpdate, SymbolManager symbolManager) {
        if (symbolToUpdate == null) {
            lastClickedSymbol.setIconImage("marker");
            symbolManager.update(lastClickedSymbol);
            lastClickedSymbol = null;
        } else if (symbolToUpdate.getIconImage().equals("marker")) {
            if (lastClickedSymbol != null) {
                lastClickedSymbol.setIconImage("marker");
            }
            symbolToUpdate.setIconImage("redMarker");
            symbolManager.update(symbolToUpdate);
            lastClickedSymbol = symbolToUpdate;
        }
    }

    public static void userRelocate(double lon, double lat, SymbolManager symbolManager) {
        userLoc.setLatLng(new LatLng(lat, lon));
        symbolManager.update(userLoc);
    }

    public static int updateSymbol(String name, SymbolManager symbolManager, Button routeButton, List<PoiGeoJsonObject> poiList) {
        for (Symbol symbol : symbols) {
            String symbolName = symbol.getTextField();
            if(name!=null & symbolName!=null) {
                if (symbolName.equalsIgnoreCase(name)) {
                    if (lastClickedSymbol != null) {
                        lastClickedSymbol.setIconImage("marker");
                    }
                    symbol.setIconImage("redMarker");
                    symbolManager.update(symbol);
                    lastClickedSymbol = symbol;
                    if (routeButton.getVisibility() != View.VISIBLE) {
                        MapSetupActivity.showView(routeButton);
                    }
                    for (PoiGeoJsonObject poi: poiList){
                        if(Objects.requireNonNull(poi.props.get("Name")).equalsIgnoreCase((symbolName+"_lvl_0"))){
                            return Integer.parseInt(String.valueOf(poi.props.get("Nav")));
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static void userMarkRotate(double azimuth, SymbolManager symbolManager) {
        userLoc.setIconRotate((float) azimuth);
        symbolManager.update(userLoc);
    }
}
