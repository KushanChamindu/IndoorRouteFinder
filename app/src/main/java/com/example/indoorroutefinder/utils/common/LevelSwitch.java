package com.example.indoorroutefinder.utils.common;

import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import java.util.List;

/**
 * Helper class to change the selected level.
 */
public class LevelSwitch {

    /**
     * Changes the MapBox filters to display only data for the given level.
     *
     * @param style MapBox style object on which the filters will be changed.
     * @param level The level that should be displayed.
     */
    public static void updateLevel(Style style, int level) {
        List<Layer> layers = style.getLayers();

        for (Layer layer : layers) {
            String layerId = layer.getId();

            if (layerId.contains("locls")) {
                if (layer instanceof SymbolLayer) {
                    SymbolLayer symbolLayer = (SymbolLayer) layer;
                    String id = symbolLayer.getId();
                    Expression updatedFilter = calculateFilter(symbolLayer.getFilter().toArray(), level);
                    symbolLayer.setFilter(updatedFilter);
                } else if (layer instanceof FillExtrusionLayer) {
                    FillExtrusionLayer fillExtrusionLayer = (FillExtrusionLayer) layer;
                    Expression updatedFilter = calculateFilter(fillExtrusionLayer.getFilter().toArray(), level);
                    fillExtrusionLayer.setFilter(updatedFilter);
                } else if (layer instanceof FillLayer) {
                    FillLayer fillLayer = (FillLayer) layer;
                    Expression updatedFilter = calculateFilter(fillLayer.getFilter().toArray(), level);
                    fillLayer.setFilter(updatedFilter);
                } else if (layer instanceof LineLayer) {
                    LineLayer lineLayer = (LineLayer) layer;
                    Expression updatedFilter = calculateFilter(lineLayer.getFilter().toArray(), level);
                    lineLayer.setFilter(updatedFilter);
                }
            }
        }
    }

    private static Expression calculateFilter(Object[] currentFilter, int filterLevel) {
        ExpressionBuilder builder = new ExpressionBuilder(currentFilter, filterLevel);
        Expression updatedFilter = builder.buildExpression();

        return updatedFilter;
    }

    static class ExpressionValue implements ExpressionData {

        private final Expression value;

        ExpressionValue(Expression value) {
            this.value = value;
        }

        Expression getValue() {
            return value;
        }
    }
}
