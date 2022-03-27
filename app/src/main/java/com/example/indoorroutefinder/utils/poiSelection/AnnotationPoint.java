package com.example.indoorroutefinder.utils.poiSelection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationPoint {
    // longitued/latitude, order is reversed!
    @JsonProperty("coordinates")
    public double[] coordinates;
}
