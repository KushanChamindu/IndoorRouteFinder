package com.example.indoorroutefinder.utils.common;

import com.example.indoorroutefinder.utils.common.ExpressionData;
import com.mapbox.mapboxsdk.style.expressions.Expression;

class ExpressionValue implements ExpressionData {

    private final Expression value;

    ExpressionValue(Expression value) {
        this.value = value;
    }

    Expression getValue() {
        return value;
    }
}