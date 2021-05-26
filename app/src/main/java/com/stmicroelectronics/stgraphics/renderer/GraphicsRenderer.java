package com.stmicroelectronics.stgraphics.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.stmicroelectronics.stgraphics.renderer.ThreeD.CubeRenderer;
import com.stmicroelectronics.stgraphics.renderer.ThreeD.PyramidRenderer;
import com.stmicroelectronics.stgraphics.renderer.ThreeD.Shape3DRenderer;
import com.stmicroelectronics.stgraphics.renderer.ThreeD.SphereRenderer;

import com.stmicroelectronics.stgraphics.renderer.TwoD.CircleRenderer;
import com.stmicroelectronics.stgraphics.renderer.TwoD.Shape2DRenderer;
import com.stmicroelectronics.stgraphics.renderer.TwoD.SquareRenderer;
import com.stmicroelectronics.stgraphics.renderer.TwoD.TriangleRenderer;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import timber.log.Timber;

/**
 * Generic Graphics Renderer used to dynamically select required renderer.
 */
public class GraphicsRenderer implements GLSurfaceView.Renderer {

    private Shape2DRenderer mCurrent2DRenderer;
    private Shape3DRenderer mCurrent3DRenderer;

    private final AtomicBoolean mRendererChanged = new AtomicBoolean(false);

    // 2D shapes
    public final static String TRIANGLE = "Triangle";
    public final static String SQUARE = "Square";
    public final static String CIRCLE = "Circle";

    private TriangleRenderer mTriangleRenderer;
    private SquareRenderer mSquareRenderer;
    private CircleRenderer mCircleRenderer;

    // 3D shapes
    public final static String PYRAMID = "Pyramid";
    public final static String CUBE = "Cube";
    public final static String SPHERE = "Sphere";

    private PyramidRenderer mPyramidRenderer;
    private CubeRenderer mCubeRenderer;
    private SphereRenderer mSphereRenderer;

    // Settings
    private String mCurrentShape;

    private boolean mLightEnabled = false;
    private boolean mColorsEnabled = false;
    private boolean mColorGradientEnabled = false;
    private boolean mTextureEnabled = false;
    private boolean mKineticEnabled = false;
    private boolean m3DEnabled;

    private final Context mContext;

    public GraphicsRenderer(Context context, String shape) {
        // 3D shapes
        // mCubeRenderer = new CubeRenderer(context);
        // mSphereRenderer = new SphereRenderer(context);
        // mPyramidRenderer = new PyramidRenderer(context);

        // 2D shapes
        // mTriangleRenderer = new TriangleRenderer(context);
        // mSquareRenderer = new SquareRenderer(context);
        // mCircleRenderer = new CircleRenderer(context);
        mContext = context;

        Timber.d("Shape %s START", shape);

        mCurrentShape = shape;
        m3DEnabled = is3DShape(shape);
        if (!m3DEnabled) {
           mCurrent2DRenderer = get2DRenderer(shape);
        } else {
            mCurrent3DRenderer = get3DRenderer(shape);
        }
    }

    /**
     * Get back shape type (2D or 3D)
     *
     * @param shape tested shape
     * @return true if the shape is 3D, false otherwise
     */
    private boolean is3DShape(String shape) {
        return shape.equals(PYRAMID) || shape.equals(CUBE) || shape.equals(SPHERE);
    }

    /**
     * Get back the renderer associated to the 2D shape
     *
     * @param shape 2D shape required
     * @return the shape 2D renderer
     */
    private Shape2DRenderer get2DRenderer(String shape) {
        switch (shape) {
            case TRIANGLE:
                if (mTriangleRenderer == null) {
                    mTriangleRenderer = new TriangleRenderer(mContext);
                }
                return mTriangleRenderer;
            case SQUARE:
                if (mSquareRenderer == null) {
                    mSquareRenderer = new SquareRenderer(mContext);
                }
                return mSquareRenderer;
            case CIRCLE:
                if (mCircleRenderer == null) {
                    mCircleRenderer = new CircleRenderer(mContext);
                }
                return mCircleRenderer;
            default:
                Timber.e("Unknown shape %s", shape);
        }
        return null;
    }

    /**
     * Get back the renderer associated to the 3D shape
     *
     * @param shape 3D shape required
     * @return the shape 3D renderer
     */
    private Shape3DRenderer get3DRenderer(String shape) {
        switch (shape) {
            case PYRAMID:
                if (mPyramidRenderer == null) {
                    mPyramidRenderer = new PyramidRenderer(mContext);
                }
                return mPyramidRenderer;
            case CUBE:
                if (mCubeRenderer == null) {
                    mCubeRenderer = new CubeRenderer(mContext);
                }
                return mCubeRenderer;
            case SPHERE:
                if (mSphereRenderer == null) {
                    mSphereRenderer = new SphereRenderer(mContext);
                }
                return mSphereRenderer;
            default:
                Timber.e("Unknown shape %s", shape);
        }
        return null;
    }

    /**
     * Select the renderer depending on shape
     *
     * @param shape 2D or 3D shape required
     */
    public void selectShape(String shape) {

        if (m3DEnabled != is3DShape(shape)) {
            if (m3DEnabled) {
                mCurrent3DRenderer.cancelRendering();
            } else {
                mCurrent2DRenderer.cancelRendering();
            }
        }

        m3DEnabled = is3DShape(shape);

        if (!shape.equals(mCurrentShape)) {
            Timber.d("Shape %s STOP", mCurrentShape);
            Timber.d("Shape %s START", shape);
            if (!m3DEnabled) {
                Timber.d("Update Renderer required, new shape = %s", shape);
                float angle = 0.0f;
                float speed = 10000.0f;
                boolean clockwise = false;
                if (mCurrent2DRenderer != null) {
                    // get back current settings
                    angle = mCurrent2DRenderer.getAngle();
                    speed = mCurrent2DRenderer.getSpeed();
                    clockwise = mCurrent2DRenderer.isClockwise();
                    // TODO check if cancel rendering is required
                    mCurrent2DRenderer.cancelRendering();
                }
                // select new renderer
                mCurrent2DRenderer = get2DRenderer(shape);
                if (mCurrent2DRenderer != null) {
                    // update settings
                    mCurrent2DRenderer.setAngle(angle);
                    mCurrent2DRenderer.setSpeed(speed);
                    mCurrent2DRenderer.setClockwise(clockwise);
                    mCurrent2DRenderer.setColorState(mColorsEnabled);
                    mCurrent2DRenderer.setColorGradient(mColorGradientEnabled);
                    mCurrent2DRenderer.setTextureState(mTextureEnabled);
                }
            } else {
                mCurrent3DRenderer = get3DRenderer(shape);
                if (mCurrent3DRenderer != null) {
                    mCurrent3DRenderer.setColorState(mColorsEnabled);
                    mCurrent3DRenderer.setTextureState(mTextureEnabled);
                    mCurrent3DRenderer.setLightState(mLightEnabled);
                    mCurrent3DRenderer.setColorGradient(mColorGradientEnabled);
                    mCurrent3DRenderer.setKineticState(mKineticEnabled);
                }
            }
            mCurrentShape = shape;
            mRendererChanged.set(true);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.onSurfaceCreated(gl, config);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.onSurfaceCreated(gl, config);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.onSurfaceChanged(gl, width, height);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.onDrawFrame(gl);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.onDrawFrame(gl);
        }
    }




    /**
     * Pause the active shape renderer
     */
    public void pause() {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.pause();
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.pause();
        }
    }

    /**
     * Resume the active shape renderer
     */
    public void resume() {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.resume();
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.resume();
        }
    }

    /**
     * Get back the current angle of the active shape renderer
     *
     * @return angle in degrees (value between 0.0f and 360.0f)
     */
    public float getAngle() {
        if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            return mCurrent2DRenderer.getAngle();
        } else {
            return 0.0f;
        }
    }

    /**
     * Set the angle to the active shape renderer
     *
     * @param angle angle in degrees (value can be < 0.0f or > 360.0f)
     */
    public void setAngle(float angle) {
        if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.setAngle(angle);
        }
    }

    /**
     * Set the delta XY angle to the active shape renderer
     *
     * @param angle delta XY angle in degrees
     */
    public void setDelta(float[] angle) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setDelta(angle);
        }
    }

    /**
     * Change color state for the active shape renderer
     *
     * @param state new color state (true if color enabled, false otherwise)
     */
    public void setColorState(boolean state) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setColorState(state);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.setColorState(state);
        }
        mColorsEnabled = state;
    }

    /**
     * Change color gradient state for the active shape renderer
     * @param state new color gradient state (true if gradient color)
     */
    public void setColorGradientState(boolean state) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setColorGradient(state);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.setColorGradient(state);
        }
        mColorGradientEnabled = state;
    }

    /**
     * Change texture state for the active shape renderer
     *
     * @param state new texture state (true if texture enabled, false otherwise)
     */
    public void setTextureState(boolean state) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setTextureState(state);
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            mCurrent2DRenderer.setTextureState(state);
        }
        mTextureEnabled = state;
    }

    /**
     * Check is renderer managed kinetic optionadb
     * @return true if kinetic option managed
     */
    public boolean isKineticManaged() {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            return mCurrent3DRenderer.isKineticManaged();
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            return mCurrent2DRenderer.isKineticManaged();
        } else {
            return false;
        }
    }

    /**
     * Change kinetic state for the active shape renderer
     * @param state new kinetic state (true if kinetic enabled, false otherwise)
     */
    public void setKineticState(boolean state) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setKineticState(state);
        }
        mKineticEnabled = state;
    }

    /**
     * Check is renderer managed light option
     * @return true if light option managed
     */
    public boolean isLightManaged() {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            return mCurrent3DRenderer.isLightManaged();
        } else if ((!m3DEnabled) && (mCurrent2DRenderer != null)) {
            return mCurrent2DRenderer.isLightManaged();
        } else {
            return false;
        }
    }

    /**
     * Change light state for the active shape renderer (only for 3D)
     *
     * @param state new light state (true if light enabled, false otherwise)
     */
    public void setLightState(boolean state) {
        if ((m3DEnabled) && (mCurrent3DRenderer != null)) {
            mCurrent3DRenderer.setLightState(state);
        }
        mLightEnabled = state;
    }


}
