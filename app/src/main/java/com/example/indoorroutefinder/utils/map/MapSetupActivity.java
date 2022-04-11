package com.example.indoorroutefinder.utils.map;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AlphaAnimation;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import com.example.indoorroutefinder.R;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MapSetupActivity {

    public static void setInitialCamera(MapboxMap mapboxMap){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(6.795577, 79.919745)) // Sets the new camera position
                .zoom(21.3) // Sets the zoom
                .bearing(80) // Rotate the camera
                .tilt(0) // Set the camera tilt
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public static String loadJsonFromAsset(String filename, AssetManager assets) {
        // Using this method to load in GeoJSON files from the assets folder.
        try {
            InputStream is = assets.open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void loadBuildingLayersIcons(@NonNull Style style, Resources resources) {
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
                lineCap(Property.LINE_CAP_ROUND),
                lineOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));
        style.addLayer(indoorBuildingLineLayer);

        Bitmap icon = BitmapFactory.decodeResource(resources, R.drawable.location);
        style.addImage("marker", icon);
        Bitmap icon1 = BitmapFactory.decodeResource(resources, R.drawable.redlocation);
        Bitmap compassNeedleSymbolLayerIcon = BitmapFactory.decodeResource(
                resources, R.drawable.icons8_navigation_50);
        style.addImage("UserLoc", compassNeedleSymbolLayerIcon);
        style.addImage("redMarker", icon1);
//        SymbolLayer indoorBuildingSymbolLayer = new SymbolLayer("indoor-building-line-symbol", "indoor-building").withProperties(
//                PropertyFactory.iconImage("marker")
//        );
//        indoorBuildingSymbolLayer.setFilter(eq(get("point-type"), literal("stole")));
//        style.addLayer(indoorBuildingSymbolLayer);
    }

    public static void hideView(View button) {
        // When the user moves away from our bounding box region or zooms out far enough the floor level
        // buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        button.startAnimation(animation);
        button.setVisibility(View.INVISIBLE);
    }

    public static void showView(View button) {
        // When the user moves inside our bounding box region or zooms in to a high enough zoom level,
        // the floor level buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        button.startAnimation(animation);
        button.setVisibility(View.VISIBLE);
    }

    public static void handleSearchClickViews(SearchView searchView, View button){
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        button.startAnimation(animation);
        searchView.setVisibility(View.VISIBLE);
        button.setVisibility(View.GONE);
    }

}
