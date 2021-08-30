package com.stmicroelectronics.stgraphics.renderer.ThreeD;

import android.opengl.GLSurfaceView;
import android.os.SystemClock;

public abstract class Shape3DRenderer implements GLSurfaceView.Renderer {

    private volatile float[] mDeltaAngle = new float[2];
    private final float[] mDeltaSpeed = new float[2];

    private final static int MAX_FIXED_ANGLE_INSTANCES = 2;
    private final float[] mAngleFixed = new float[] {0.0f, 0.0f};
    private final float[] mSpeedFixed = new float[] {0.0f, 0.0f};
    private final long[] mStoreTimeFixedSpeed = new long[MAX_FIXED_ANGLE_INSTANCES];

    private boolean mPause = false;
    private boolean mColored = false;
    private boolean mColorGradientEnabled = false;
    private boolean mKineticEnable = false;

    void initDelta() {
        if (! mPause) {
            mDeltaAngle[0] = 2.0f;
            mDeltaAngle[1] = 2.0f;
            mDeltaSpeed[0] = mDeltaAngle[0] / 20;
            mDeltaSpeed[1] = mDeltaAngle[1] / 20;
        }
    }

    public void setDelta(float[] delta) {
        mDeltaAngle = delta;
    }

    public void setDeltaSpeed(float[] delta, long deltaTime) {
        mDeltaAngle = delta;
        mDeltaSpeed[0] = delta[0] / deltaTime;
        mDeltaSpeed[1] = delta[1] / deltaTime;
    }

    float[] getDeltaAngle() {
        float[] delta = new float[2];

        delta[0] = mDeltaAngle[0];
        delta[1] = mDeltaAngle[1];

        if (mPause) {
            mDeltaAngle[0] = 0.0f;
            mDeltaAngle[1] = 0.0f;
        }
        return delta;
    }

    float[] getDeltaAngleWithSpeed(long deltaTime) {
        if (mPause) {
            return getDeltaAngle();
        }

        float[] delta = new float[2];

        delta[0] = mDeltaSpeed[0] * deltaTime;
        delta[1] = mDeltaSpeed[1] * deltaTime;

        return delta;
    }

    float getDeltaTotal() {
        float total = (float) Math.sqrt(Math.pow(mDeltaAngle[0], 2) + Math.pow(mDeltaAngle[1], 2));
        if ((mDeltaAngle[0] < 0.0f) && (mDeltaAngle[1] > 0.0f)) {
            total = -total;
        }
        if (mPause) {
            mDeltaAngle[0] = 0.0f;
            mDeltaAngle[1] = 0.0f;
        }
        return total;
    }

    float getDeltaTotalWithSpeed(long deltaTime) {
        if (mPause) {
            return getDeltaTotal();
        }

        float total = (float) Math.sqrt(Math.pow(mDeltaSpeed[0] * deltaTime, 2) + Math.pow(mDeltaSpeed[1] * deltaTime, 2));
        if ((mDeltaSpeed[0] < 0.0f) && (mDeltaSpeed[1] > 0.0f)) {
            total = -total;
        }
        return total;
    }

    /**
     * Pause the rendering
     */
    public void pause() {
        mPause = true;
    }

    /**
     * Resume the rendering, speed is updated
     */
    public void resume() {
        mPause = false;
    }

    /**
     * Return rendering pause state
     * @return true if rendering paused
     */
    boolean isStarted() {
        return !mPause;
    }

    /**
     * Enable/Disable colors
     * @param state new color state
     */
    public void setColorState(boolean state) {
        mColored = state;
    }

    /**
     * Get back color state
     * @return true or false depending on the color state
     */
    boolean isColored() {
        return mColored;
    }

    /**
     * Enable/Disable color gradient
     * @param state new color gradient state
     */
    public void setColorGradient(boolean state) {
        mColorGradientEnabled = state;
    }

    /**
     * Get back color gradient state
     * @return true or false depending on the color gradient state
     */
    boolean isColorGradient() {
        return mColorGradientEnabled;
    }

    /**
     * Enable/Disable kinetic
     * @param state new kinetic state
     */
    public void setKineticState(boolean state) {
        mKineticEnable = state;
    }

    /**
     * Get back kinetic state
     * @return true or false depending on kinetic state
     */
    boolean ismKineticEnable() {
        return mKineticEnable;
    }

    /**
     * Enable/Disable textures (abstract)
     * @param state new texture state
     */
    public abstract void setTextureState(boolean state);

    /**
     * Enable/Disable light (abstract)
     * @param state new light state
     */
    public abstract void setLightState(boolean state);

    /**
     * Cancel rendering (detach shader and program)
     */
    public abstract void cancelRendering();

    /**
     * Check if renderer managed kinetic option
     * @return true if kinetic option managed
     */
    public abstract boolean isKineticManaged();

    /**
     * Check if renderer managed light option
     * @return true if light option managed
     */
    public abstract boolean isLightManaged();

    /**
     * Check if the shape is moving or not
     * @return true if the shape is stationary
     */
    boolean isStationary() {
        return (Math.abs(mDeltaAngle[0]) < 0.2f) && (Math.abs(mDeltaAngle[1]) < 0.2f);
    }

    /**
     * Initialize normalized time and speed for the selected instance
     * @param speed required speed
     * @param index instance
     */
    void initFixedSpeed(float speed, int index) {
        mSpeedFixed[index] = speed;
        mStoreTimeFixedSpeed[index] = SystemClock.uptimeMillis();
    }

    /**
     * Get updated angle in degrees
     *   case Pause: return stored angle
     *   case Resume: calculate new angle depending on speed for the instance (initialized)
     * @param index instance
     * @return angle in degrees which shall be applied to the rotation
     */
    float getAngleInDegreesFixedSpeed(int index) {
        float angleInDegrees;
        if (!mPause) {
            long time = SystemClock.uptimeMillis();
            angleInDegrees = mAngleFixed[index] - ((360.0f / mSpeedFixed[index]) * (int) (time - mStoreTimeFixedSpeed[index]));
            if (angleInDegrees < 0.0f) {
                angleInDegrees += 360.0f;
            }
            mStoreTimeFixedSpeed[index] = time;
            mAngleFixed[index] = angleInDegrees;
        } else {
            angleInDegrees = mAngleFixed[index];
        }
        return angleInDegrees;
    }
}
