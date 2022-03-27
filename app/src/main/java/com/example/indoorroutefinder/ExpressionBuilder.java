package com.example.indoorroutefinder;

import com.mapbox.mapboxsdk.style.expressions.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Requirement: The level check in an existing filter(comes from the server) must be changed.
 * Other values must not be altered.
 * <p>
 * The filter comes in the form on an Object[]. At the end, an Expression must be returned. The Expression
 * MUST ONLY CONSIST OF OTHER EXPRESSIONS.
 * <p>
 * Every Object[] consists of 1 operator(equals, notEquals, bigger, smaller..) and X ExpressionData.
 * ExpressionData can be either ExpressionValue(a single string or float packaged in an expression)
 * or another ExpressionBuilder. Example expression:
 * ["all",
 * ["==", ["geometry-type"], "LineString"],
 * ["==", ["get", "level"], 0.0],
 * ["any",
 * ["==", ["get", "style"], "route"],
 * ["all",
 * ["==", ["get", "style"], "routedirection"],
 * ["==", ["get", "step"], 1.0]
 * ]
 * ]
 * ]
 */
class ExpressionBuilder implements ExpressionData {
    private String operator;
    private List<ExpressionData> arguments;
    private static boolean levelFound = false;

    /**
     * Breaks the given filter into Expression objects.
     *
     * @param filter Object[] representing the current MapBox layer filter.
     * @param level The level to be filtered for.
     */
    ExpressionBuilder(Object[] filter, int level) {
        if (filter[0] instanceof String) {
            operator = (String) filter[0];
        } else {
            return;
        }

        arguments = new ArrayList<>();

        for (int i = 1; i < filter.length; i++) {
            if (filter[i] instanceof Object[]) {
                Object[] subArray = (Object[]) filter[i];

                ExpressionBuilder subExpression = new ExpressionBuilder(subArray, level);
                arguments.add(subExpression);
            } else {
                Expression literal;
                if (filter[i] instanceof String) {
                    String valueString = (String) filter[i];

                    if (valueString.equals("level")) {
                        levelFound = true;
                    }

                    literal = Expression.literal(valueString);
                } else if (filter[i] instanceof Float) {
                    Float valueFloat;
                    if (levelFound) {
                        valueFloat = (float) level;
                        levelFound = false;
                    } else {
                        valueFloat = (Float) filter[i];
                    }
                    literal = Expression.literal(valueFloat);
                } else {
                    arguments = null;
                    return;
                }

                arguments.add(new ExpressionValue(literal));
            }
        }
    }

    /**
     * Reassembles the filter given in the constructor, with filters for the selected level.
     *
     * @return MapBox filter for the selected level.
     */
    Expression buildExpression() {
        Expression expression;

        List<Expression> combinedElements = new ArrayList<>();
        for (ExpressionData data : arguments) {
            if (data instanceof ExpressionBuilder) {
                ExpressionBuilder subBuilder = (ExpressionBuilder) data;
                combinedElements.add(subBuilder.buildExpression());
            } else if (data instanceof ExpressionData) {
                ExpressionValue subValue = (ExpressionValue) data;
                combinedElements.add(subValue.getValue());
            }
        }

        Expression[] expressions = new Expression[combinedElements.size()];
        expressions = combinedElements.toArray(expressions);
        expression = new Expression(operator, expressions);

        return expression;
    }
}
