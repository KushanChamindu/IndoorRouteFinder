package com.example.indoorroutefinder.utils.poiSelection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoiGeoJsonObject implements Serializable {
    @JsonProperty("uid")
    public String uid;
    @JsonProperty("type")
    public String type;
    @JsonProperty("props")
    public HashMap<String, String> props;
}