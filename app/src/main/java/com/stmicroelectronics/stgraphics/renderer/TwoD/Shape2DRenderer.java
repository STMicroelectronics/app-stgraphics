package com.stmicroelectronics.stgraphics.renderer.TwoD;

import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import com.stmicroelectronics.stgraphics.utils.Utility;

public abstract class Shape2DRenderer implements GLSurfaceView.Renderer {

    private volatile float mAngle;
    private volatile float mDeltaAngle;
    private boolean mPause = false;
    private boolean mColored = false;
    private boolean mColorGradient = false;

    private long mCurrentTime;
    private long mPreviousTime;
    private long mPreviousTimeNormalized;

    private float mFullTurnTime = 10000.0f;
    private boolean mClockwise = false;

    /**
     * Get current angle value
     *
     * @return current angle in degrees (float between 0.0f to 360.0f)
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Update current angle value un degrees (stored value = float between 0.0f to 360.0f)
     *
     * @param angle new angle in degrees used for next rotation
     */
    public void setAngle(float angle) {
        if (mPause) {
            // store information to calculate speed
            mPreviousTime = mCurrentTime;
            mCurrentTime = SystemClock.uptimeMillis();
            mDeltaAngle = angle - mAngle;
        }

        if (angle > 360.0f) {
            mAngle = angle - 360.0f;
        } else if (angle < 0.0f){
            mAngle = angle + 360.0f;
        } else {
            mAngle = angle;
        }
    }

    /**
     * Pause the shape (manual rotation)
     */
    public void pause() {
        mPause = true;
    }

    /**
     * Resume the shape (automatic rotation)
     */
    public void resume() {
        mFullTurnTime = Utility.getOneTurnTime(mDeltaAngle, mPreviousTime, mCurrentTime);
        if (mFullTurnTime > 10000.0f) {
            mFullTurnTime = 10000.0f;
        }
        mClockwise = mDeltaAngle < 0;

        mPreviousTimeNormalized = SystemClock.uptimeMillis() % (long) mFullTurnTime;
        mPause = false;
    }

    /**
     * Change the color state
     *
     * @param state new color state (true = colors enabled)
     */
    public void setColorState(boolean state) {
        mColored = state;
    }

    /**
     * Get back the color state
     *
     * @return color state (true = colors enabled)
     */
    boolean isColored() {
        return mColored;
    }

    /**
     * Enable/Disable color gradient
     * @param state new color gradient state
     */
    public void setColorGradient(boolean state) {
        mColorGradient = state;
    }

    /**
     * Get back color gradient state
     * @return true or false depending on the color gradient state
     */
    boolean isColorGradient() {
        return mColorGradient;
    }

    /**
     * Get back time to operate a full rotation (speed)
     *
     * @return time in ms to operate a 360° rotation
     */
    public float getSpeed() {
        return mFullTurnTime;
    }

    /**
     * Update the time to operate a full rotation (speed)
     *
     * @param fullTurnTime time in ms to operate a 360° rotation
     */
    public void setSpeed(float fullTurnTime) {
        mFullTurnTime = fullTurnTime;
    }

    /**
     * Get back rotation direction information (clockwise)
     *
     * @return true if rotation is clockwise
     */
    public boolean isClockwise() {
        return mClockwise;
    }

    /**
     * Update rotation direction information (clockwise)
     *
     * @param clockwise true if rotation is clockwise
     */
    public void setClockwise(boolean clockwise) {
        mClockwise = clockwise;
    }

    /**
     * Initialize the normalized time value (origin)
     */
    void initTime() {
        mPreviousTimeNormalized = SystemClock.uptimeMillis() % (long) mFullTurnTime;
    }

    /**
     * Get back current angle in degrees
     *   If in pause state, the value is fixed (current angle)
     *   If in resume state, the value is calculated depending on speed
     *
     * @return angle in degrees (used for shape rotation)
     */
    float getAngleInDegrees() {
        float angleInDegrees;
        if (!mPause) {
            long time = SystemClock.uptimeMillis() % (long) mFullTurnTime;
            if (mClockwise) {
                angleInDegrees = mAngle - ((360.0f / mFullTurnTime) * (int) (time - mPreviousTimeNormalized));
                if (angleInDegrees < 0.0f) {
                    angleInDegrees += 360.0f;
                }
            } else {
                angleInDegrees = mAngle + ((360.0f / mFullTurnTime) * (int) (time - mPreviousTimeNormalized));
                if (angleInDegrees > 360.0f) {
                    angleInDegrees -= 360.0f;
                }
            }
            mPreviousTimeNormalized = time;
            mAngle = angleInDegrees;
        } else {
            angleInDegrees = mAngle;
        }
        return angleInDegrees;
    }

    /**
     * Enable/Disable textures (abstract)
     * @param state new texture state
     */
    public abstract void setTextureState(boolean state);

    /**
     * Cancel rendering (detach shader and program)
     */
    public abstract void cancelRendering();

    /**
     * Check is renderer managed kinetic option
     * @return true if kinetic option managed
     */
    public abstract boolean isKineticManaged();

    /**
     * Check is renderer managed light option
     * @return true if light option managed
     */
    public abstract boolean isLightManaged();
}
