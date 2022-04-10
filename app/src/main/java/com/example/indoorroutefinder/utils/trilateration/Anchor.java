package com.example.indoorroutefinder.utils.trilateration;

public class Anchor {
    private double lat;
    private double lon;
    private String anchorName;
    private double distanceToUser;
    private boolean isUpdate = false;

    public boolean isUpdate() {
        return isUpdate;
    }

    public Anchor(double lat, double lon, String anchorName) {
        this.lat = lat;
        this.lon = lon;
        this.anchorName = anchorName;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public void setDistanceToUser(double distance) {
        this.distanceToUser = distance;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getDistanceToUser() {
        return distanceToUser;
    }
}
