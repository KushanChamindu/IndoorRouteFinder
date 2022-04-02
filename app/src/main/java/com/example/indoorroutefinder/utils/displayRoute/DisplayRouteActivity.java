package com.example.indoorroutefinder.utils.displayRoute;

import com.example.indoorroutefinder.utils.restCall.RestCall;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class DisplayRouteActivity {
    private static GeoJsonSource routeSource;

    public static void initSource(Style style) {
        List<Source> sources = style.getSources();
        for (Source source : sources) {
            if (source.getId().contains("route") && source instanceof GeoJsonSource) {
                routeSource = (GeoJsonSource) source;
            }
        }
    }

    public static void onCalcRouteClicked(String API_KEY) {
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
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        setGeoJson(rawRouteJson);
    }

    private static void setGeoJson(String rawRouteJson){
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

}
