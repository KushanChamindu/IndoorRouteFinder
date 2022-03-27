package com.example.indoorroutefinder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AnnotationPoint {
    // longitued/latitude, order is reversed!
    @JsonProperty("coordinates")
    public double[] coordinates;
}
