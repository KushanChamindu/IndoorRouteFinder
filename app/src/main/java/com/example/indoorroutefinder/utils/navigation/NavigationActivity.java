package com.example.indoorroutefinder.utils.navigation;

import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class NavigationActivity {
    private static Map<String, PoiGeoJsonObject> poiList = null;
    private static final int verticesCount = 13;
    private static final ArrayList<ArrayList<Integer>> edges = new ArrayList<>(verticesCount);
    private static Polyline polyline = null;

    public static void initNav(String geoJsonSource) {
        loadPOIs(geoJsonSource);

        for (int i = 1; i < verticesCount; i++) {
            edges.add(new ArrayList<>());
        }

        addEdge(1, 2);
        addEdge(2, 3);
        addEdge(3, 4);
        addEdge(4, 5);
        addEdge(5, 6);
        addEdge(6, 7);
        addEdge(6, 8);
        addEdge(8, 9);
        addEdge(8, 10);
        addEdge(10, 11);
    }

    private static void loadPOIs(String geoJsonSource) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        try {
            Map<String, ArrayList<LinkedHashMap>> map = objectMapper.readValue(geoJsonSource, Map.class);
            ArrayList<LinkedHashMap> pointers = map.get("features");
            poiList = new HashMap<>();
            for (LinkedHashMap pointer : pointers) {
                LinkedHashMap geometry = (LinkedHashMap) pointer.get("geometry");
                if (geometry.get("type").equals("Point")) {
                    poiList.put((String) pointer.get("id"), new PoiGeoJsonObject((String) pointer.get("id"), (String) geometry.get("type"), (LinkedHashMap<String, String>) pointer.get("properties"), (ArrayList<String>) geometry.get("coordinates")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addEdge(int i, int j) {
        edges.get(i).add(j);
        edges.get(j).add(i);
    }

    private static LinkedHashMap<String, ArrayList<String>> getShortestPath(int src, int dest) {
        // predecessor[i] array stores predecessor of
        // i and distance array stores distance of i
        // from s
        int[] predecessor = new int[verticesCount];
        int[] distance = new int[verticesCount];

        if (!BFS(src, dest, predecessor, distance)) {
            Log.i("Info", "Given source and destination are not connected");
            return null;
        }

        // LinkedList to store path
        LinkedList<Integer> path = new LinkedList<>();
        int crawl = dest;
        path.add(crawl);
        while (predecessor[crawl] != -1) {
            path.add(predecessor[crawl]);
            crawl = predecessor[crawl];
        }

        // Log distance
        Log.i("Shortest path length is", String.valueOf(distance[dest]));

        LinkedHashMap<String, ArrayList<String>> finalPath = new LinkedHashMap<>();
        for (int i = path.size() - 1; i >= 0; i--) {
            finalPath.put(String.valueOf(path.get(i)), null);
        }
        for (Map.Entry<String,PoiGeoJsonObject> entry : poiList.entrySet()){
            String key = String.valueOf(entry.getValue().props.get("Nav"));
            if(finalPath.containsKey(key)){
                finalPath.put(key, entry.getValue().coordinates);
            }
        }
        return finalPath;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void displayRoute(int src, int dest, MapboxMap mapboxMap){
        // remove any existing polyline
        if (polyline != null) {
            mapboxMap.removePolyline(polyline);
            polyline = null;
        }
        if (src != dest) {
            LinkedHashMap<String, ArrayList<String>> finalPath = getShortestPath(src, dest);
            ArrayList<LatLng> points = finalPath.values().stream().map(point -> new LatLng(
                    Double.parseDouble(String.valueOf(point.get(1))),
                    Double.parseDouble(String.valueOf(point.get(0)))
            )).collect(Collectors.toCollection(ArrayList::new));

            // Log path
            Log.i("Path is", String.valueOf(finalPath));

            // add polyline to MapboxMap object
            polyline = mapboxMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.RED)
                    .width(3f));
        }
    }

    private static boolean BFS(int src, int dest, int[] predecessor, int[] dist) {
        // a queue to maintain queue of vertices whose
        // adjacency list is to be scanned as per normal
        // BFS algorithm using LinkedList of Integer type
        LinkedList<Integer> queue = new LinkedList<>();

        // boolean array visited[] which stores the
        // information whether ith vertex is reached
        // at least once in the Breadth first search
        boolean[] visited = new boolean[verticesCount];

        // initially all vertices are unvisited
        // so v[i] for all i is false
        // and as no path is yet constructed
        // dist[i] for all i set to infinity
        for (int i = 0; i < verticesCount; i++) {
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
            predecessor[i] = -1;
        }

        // now source is first to be visited and
        // distance from source to itself should be 0
        visited[src] = true;
        dist[src] = 0;
        queue.add(src);

        // bfs Algorithm
        while (!queue.isEmpty()) {
            int u = queue.remove();
            for (int i = 0; i < edges.get(u).size(); i++) {
                if (!visited[edges.get(u).get(i)]) {
                    visited[edges.get(u).get(i)] = true;
                    dist[edges.get(u).get(i)] = dist[u] + 1;
                    predecessor[edges.get(u).get(i)] = u;
                    queue.add(edges.get(u).get(i));

                    // stopping condition (when we find
                    // our destination)
                    if (edges.get(u).get(i) == dest)
                        return true;
                }
            }
        }
        return false;
    }

}
