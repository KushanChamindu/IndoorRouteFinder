package com.example.indoorroutefinder.utils.search;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;

import com.example.indoorroutefinder.utils.poiSelection.POISelectionActivity;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;

import java.util.List;

public class SearchActivity {

    public static void handleSearch(SearchView searchView, SymbolManager symbolManager, Button routeB,
                                    List<PoiGeoJsonObject> poiList, Context context){
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search here ......");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                POISelectionActivity.updateSymbol(location, symbolManager, routeB, poiList, context);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
}