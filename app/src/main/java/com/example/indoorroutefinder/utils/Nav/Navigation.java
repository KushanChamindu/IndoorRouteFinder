package com.example.indoorroutefinder.utils.Nav;

import android.util.Log;

import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Navigation {
    private static Map<String, PoiGeoJsonObject> pois = null;
//    private Map<String, ArrayList<PoiGeoJsonObject>> edges = new HashMap<>();


    public static void initNav(String geoJsonSource) {
        loadPOIs(geoJsonSource);
        int v = 13;
//        for (Map.Entry<String,PoiGeoJsonObject> entry : this.pois.entrySet()){
//            this.edges.put(entry.getKey(), new ArrayList<PoiGeoJsonObject>());
//        }
//            System.out.println("Key = " + entry.getKey() +
//                    ", Value = " + entry.getValue());
//        addEdge(this.edges, pois.get("9284b1d7105a50a7cf5c7839e91e004f"), pois.get("979b96ee2d6f51cc1b6c3033d27a5cc8")); //1-2
//        addEdge(this.edges, pois.get("979b96ee2d6f51cc1b6c3033d27a5cc8"), pois.get("24df4df9d0ac8f70db736c6b8f0455f6")); //2-3
//        addEdge(this.edges, pois.get("24df4df9d0ac8f70db736c6b8f0455f6"), pois.get("9278d36bbc2476c8dc05ac864ad0bbee")); //3-4
//        addEdge(this.edges, pois.get("9278d36bbc2476c8dc05ac864ad0bbee"), pois.get("170dd8211c08038181c27677109dc230")); //4-5
//        addEdge(this.edges, pois.get("170dd8211c08038181c27677109dc230"), pois.get("166873b276bf82990dc2844f710b8521")); //5-6
//        addEdge(this.edges, pois.get("166873b276bf82990dc2844f710b8521"), pois.get("07f4576eb439063b9c8e98118206d714")); //6-7
//        addEdge(this.edges, pois.get("166873b276bf82990dc2844f710b8521"), pois.get("8a1f9be2879119c7cab2504d88055348")); //6-8
//        addEdge(this.edges, pois.get("8a1f9be2879119c7cab2504d88055348"), pois.get("f0fa4281554d39dcca3acdba8c347848")); //8-9
//        addEdge(this.edges, pois.get("8a1f9be2879119c7cab2504d88055348"), pois.get("77830a345eab7be1051ffe84cd0d5f33")); //8-10
//        addEdge(this.edges, pois.get("77830a345eab7be1051ffe84cd0d5f33"), pois.get("d6baba46e6ccd076b7e09cb14e8fc2da")); //10-11

        ArrayList<ArrayList<Integer>> adj =
                new ArrayList<ArrayList<Integer>>(v);
        for (int i = 1; i < v; i++) {
            adj.add(new ArrayList<Integer>());
        }

        addEdge(adj, 1, 2);
        addEdge(adj, 2, 3);
        addEdge(adj, 3, 4);
        addEdge(adj, 4, 5);
        addEdge(adj, 5, 6);
        addEdge(adj, 6, 7);
        addEdge(adj, 6, 8);
        addEdge(adj, 8, 9);
        addEdge(adj, 8, 10);
        addEdge(adj, 10, 11);
        int source = 1, dest = 11;
        printShortestDistance(adj, source, dest, v);
    }

    private static void loadPOIs(String geoJsonSource) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        try {
            Map<String, ArrayList<LinkedHashMap>> map = objectMapper.readValue(geoJsonSource, Map.class);
            ArrayList<LinkedHashMap> pointers = map.get("features");
            pois = new HashMap<>();
            for (LinkedHashMap pointer : pointers) {
                LinkedHashMap geometry = (LinkedHashMap) pointer.get("geometry");
                if (geometry.get("type").equals("Point")) {
                    pois.put((String) pointer.get("id"), new PoiGeoJsonObject((String) pointer.get("id"), (String) geometry.get("type"), (LinkedHashMap<String, String>) pointer.get("properties"), (ArrayList<String>) geometry.get("coordinates")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addEdge(ArrayList<ArrayList<Integer>> adj, int i, int j) {
        adj.get(i).add(j);
        adj.get(j).add(i);
    }

    private static void printShortestDistance(
            ArrayList<ArrayList<Integer>> adj,
            int s, int dest, int v) {
        // predecessor[i] array stores predecessor of
        // i and distance array stores distance of i
        // from s
        int pred[] = new int[v];
        int dist[] = new int[v];

        if (BFS(adj, s, dest, v, pred, dist) == false) {
            System.out.println("Given source and destination" +
                    "are not connected");
            return;
        }

        // LinkedList to store path
        LinkedList<Integer> path = new LinkedList<Integer>();
        int crawl = dest;
        path.add(crawl);
        while (pred[crawl] != -1) {
            path.add(pred[crawl]);
            crawl = pred[crawl];
        }

        // Print distance
        Log.i("Shortest path length is", String.valueOf(dist[dest]));

        // Print path
        Log.i("Path is ::", "null");
        for (int i = path.size() - 1; i >= 0; i--) {
            Log.i("Path" , path.get(i) + " ");
        }
    }

    private static boolean BFS(ArrayList<ArrayList<Integer>> adj, int src,
                               int dest, int v, int pred[], int dist[]) {
        // a queue to maintain queue of vertices whose
        // adjacency list is to be scanned as per normal
        // BFS algorithm using LinkedList of Integer type
        LinkedList<Integer> queue = new LinkedList<Integer>();

        // boolean array visited[] which stores the
        // information whether ith vertex is reached
        // at least once in the Breadth first search
        boolean visited[] = new boolean[v];

        // initially all vertices are unvisited
        // so v[i] for all i is false
        // and as no path is yet constructed
        // dist[i] for all i set to infinity
        for (int i = 0; i < v; i++) {
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
            pred[i] = -1;
        }

        // now source is first to be visited and
        // distance from source to itself should be 0
        visited[src] = true;
        dist[src] = 0;
        queue.add(src);

        // bfs Algorithm
        while (!queue.isEmpty()) {
            int u = queue.remove();
            for (int i = 0; i < adj.get(u).size(); i++) {
                if (visited[adj.get(u).get(i)] == false) {
                    visited[adj.get(u).get(i)] = true;
                    dist[adj.get(u).get(i)] = dist[u] + 1;
                    pred[adj.get(u).get(i)] = u;
                    queue.add(adj.get(u).get(i));

                    // stopping condition (when we find
                    // our destination)
                    if (adj.get(u).get(i) == dest)
                        return true;
                }
            }
        }
        return false;
    }

}
