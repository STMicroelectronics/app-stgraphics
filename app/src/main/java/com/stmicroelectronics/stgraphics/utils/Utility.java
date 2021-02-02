package com.stmicroelectronics.stgraphics.utils;

import android.graphics.Point;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;

/**
 * Utility class
 */
public class Utility {

    /**
     * Normalize RGBA color
     *
     * @param color 32bit RGBA color
     * @return normalized color table (max: 1.0f)
     */
    public static float[] normalizeColor(int color) {

        float r = ((color >> 16) & 0xff) / 255.0f;
        float g = ((color >>  8) & 0xff) / 255.0f;
        float b = ((color      ) & 0xff) / 255.0f;
        float a = ((color >> 24) & 0xff) / 255.0f;

        return new float[]{roundFloat(r), roundFloat(g), roundFloat(b), roundFloat(a)};
    }

    /**
     * Round float value (two decimal points)
     *
     * @param value original float value
     * @return rounded float value
     */
    private static float roundFloat(float value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Float.parseFloat(df.format(value));
    }

    /**
     * Calculate angle from origin to destination vs a center point
     *
     * @param center center point 2D coordinates (X,Y)
     * @param origin origin point 2D coordinates (X,Y)
     * @param destination destination point 2D coordinates (X,Y)
     * @return calculated angle in degrees (between 0.0f and 360.0f)
     */
    public static float getAngle(Point center, Point origin, Point destination) {
        double radian1 = Math.atan2(origin.x - center.x, origin.y - center.y);
        double radian2 = Math.atan2(destination.x - center.x, destination.y - center.y);
        // check sign change when crossing 0 abscissa
        if ((radian1 < 0) && (radian2 > 0)) {
            if (destination.y < center.y) {
                radian2 -= 2.0 * Math.PI;
            }
        }
        if ((radian2 < 0) && (radian1 > 0)) {
            // case 0 abscissa is crossed
            if (destination.y < center.y) {
                radian2 += 2.0 * Math.PI;
            }
        }
        return (float) Math.toDegrees(radian2 - radian1);
    }

    /**
     * Calculate 3D angle from origin to destination (based on scale factor)
     *
     * @param origin origin point 2D coordinates (X,Y)
     * @param destination destination point 2D coordinates (X,Y)
     * @return calculated angles (XY) in degrees
     */
    public static float[] getDelta(Point origin, Point destination) {
        final float TOUCH_SCALE_FACTOR = 180.0f / 320;
        float[] delta = new float[2];
        delta[0] = - (destination.x - origin.x) * TOUCH_SCALE_FACTOR;
        delta[1] = - (destination.y - origin.y) * TOUCH_SCALE_FACTOR;
        return delta;
    }

    /**
     * Get back duration to execute one full turn
     *
     * @param angle angle calculated between origin (previous time) and destination (current time)
     * @param previousTime origin time
     * @param currentTime destination time
     * @return duration to execute one full turn (ms)
     */
    public static float getOneTurnTime(float angle, long previousTime, long currentTime) {
        if ((currentTime - previousTime) > 0) {
            return Math.round((360.0f / Math.abs(angle)) * (int) (currentTime - previousTime));
        } else {
            // return default speed
            return 10000.0f;
        }
    }

    /**
     * Translate vertices by offset in Z axe
     *
     * @param vertices original vertices
     * @param offset offset on Z axe required
     * @return vertices translated
     */
    public static float[] zTranslateVertices(@NonNull float[] vertices, float offset) {
        float[] outVertices = new float[vertices.length];
        int zIndex = 2;

        if (vertices.length < 3)
            return vertices;

        for (int i=0;i<vertices.length;i++) {
            if (i == zIndex) {
                outVertices[i] = vertices[i] + offset;
                zIndex+=3;
            } else {
                outVertices[i] = vertices[i];
            }
        }

        return outVertices;
    }
}
