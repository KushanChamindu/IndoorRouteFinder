package com.example.indoorroutefinder.utils.poiSelection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoiGeoJsonObject implements Serializable {
    @JsonProperty("id")
    public String id;
    @JsonProperty("type")
    public String type;
    @JsonProperty("props")
    public LinkedHashMap<String, String> props;
    @JsonProperty("coordinates")
    public ArrayList<String> coordinates;

    public PoiGeoJsonObject(String id, String type, LinkedHashMap<String,String> props, ArrayList<String> coordinates){
        this.coordinates=coordinates;
        this.id=id;
        this.props=props;
        this.type=type;
    }
}