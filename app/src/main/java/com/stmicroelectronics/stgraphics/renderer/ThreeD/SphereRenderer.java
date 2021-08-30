package com.stmicroelectronics.stgraphics.renderer.ThreeD;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

import com.stmicroelectronics.stgraphics.R;
import com.stmicroelectronics.stgraphics.utils.ShaderHelper;
import com.stmicroelectronics.stgraphics.utils.TextureHelper;
import com.stmicroelectronics.stgraphics.utils.Utility;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL surface renderer for Sphere
 */
public class SphereRenderer extends Shape3DRenderer {

    // Angle step between sphere points in degrees
    private final static float SPHERE_ANGLE_STEP = 3f;
    private final static float SATELLITE_ANGLE_STEP = 5f;

    // Sphere radius (normalized)
    private final static float SPHERE_RADIUS = 0.5f;
    private final static float SATELLITE_RADIUS = 0.1f;

    private final static float SATELLITE_ORBIT_RADIUS = 0.85f;
    private final static float SATELLITE_ORBIT_ANGLE = 35.0f;

    // How many bytes per float
    private final static int NB_BYTES_PER_FLOAT = 4;

    /** Store our model data in a float buffer */
    private final FloatBuffer mVerticesNoColor;
    private final FloatBuffer mVerticesColor;
    private final FloatBuffer mVerticesColorGradient;

    private final FloatBuffer mVerticesSatelliteNoColor;
    private final FloatBuffer mVerticesSatelliteColorGradient;
    private final FloatBuffer mVerticesSatelliteColor;

    private final FloatBuffer mTextureCoordinateBuffer;
    private final FloatBuffer mTextureSatelliteCoordinateBuffer;

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private final float[] mViewMatrix = new float[16];

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private final float[] mModelMatrix = new float[16];
    private final float[] mModelSatelliteMatrix = new float[16];

    private final float[] mAccumulatedRotation = new float[16];
    private final float[] mCurrentRotation = new float[16];

    private float mSatelliteAngle;

    /**
     * Store the light position.
     */
    private final float[] mLightPos = new float[4];

    /** This is a handle to our texture data. */
    private int[] mTextureDataHandles;

    /** This is a handles to program and shader. */
    private int mProgramHandle;
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;

    private final int mNbVertices;
    private final int mNbVerticesSatellite;

    private final Context mContext;
    private Sphere mSphere;

    private boolean mTextureEnabled = false;
    private boolean mLightEnabled = false;

    private long mPreviousTime = 0;

    public SphereRenderer(Context context) {
        mContext = context;

        float[] colorLight, color1, color2, color3;

        colorLight = Utility.normalizeColor(mContext.getColor(R.color.colorLightBlue));

        color1 = Utility.normalizeColor(mContext.getColor(R.color.colorShape1));
        color2 = Utility.normalizeColor(mContext.getColor(R.color.colorShape3));
        color3 = Utility.normalizeColor(mContext.getColor(R.color.colorShape4));

        // Calculate number of vertices required
        mNbVertices = 2 * Math.round((180 / SPHERE_ANGLE_STEP) * ((360 / SPHERE_ANGLE_STEP) + 1));
        mNbVerticesSatellite = 2 * Math.round((180 / SATELLITE_ANGLE_STEP) * ((360 / SATELLITE_ANGLE_STEP) + 1));

        // add non colored sphere in float buffer (XYZ + RGBA)
        mVerticesNoColor = getSphereVertices(colorLight, colorLight, colorLight, false,1, mNbVertices, SPHERE_ANGLE_STEP, SPHERE_RADIUS);

        // add colored sphere in float buffer (XYZ + RGBA)
        mVerticesColor = getSphereVertices(color1, color2, color3, false, 3, mNbVertices, SPHERE_ANGLE_STEP, SPHERE_RADIUS);

        // add non colored sphere in float buffer (XYZ + RGBA)
        mVerticesColorGradient = getSphereVertices(color1, color2, color3, true, 3, mNbVertices, SPHERE_ANGLE_STEP,  SPHERE_RADIUS);

        // add non colored sphere in float buffer (XYZ + RGBA)
        mVerticesSatelliteNoColor = getSphereVertices(colorLight, colorLight, colorLight,false, 1, mNbVerticesSatellite, SATELLITE_ANGLE_STEP, SATELLITE_RADIUS);

        // add non gradient colored sphere in float buffer (XYZ + RGBA)
        mVerticesSatelliteColor = getSphereVertices(color2, color3, color2,false, 1, mNbVerticesSatellite, SATELLITE_ANGLE_STEP, SATELLITE_RADIUS);

        // add gradient colored sphere in float buffer (XYZ + RGBA)
        mVerticesSatelliteColorGradient = getSphereVertices(color2, color3, color2,true, 2, mNbVerticesSatellite, SATELLITE_ANGLE_STEP, SATELLITE_RADIUS);

        // add texture coordinates in float buffer (XY)
        mTextureCoordinateBuffer = getSphereTextureCoordinates(mNbVertices, SPHERE_ANGLE_STEP);

        // add texture coordinates (case satellite) in float buffer (XY)
        mTextureSatelliteCoordinateBuffer = getSphereTextureCoordinates(mNbVerticesSatellite, SATELLITE_ANGLE_STEP);
    }

    /**
     * Get back float buffer with all vertices and associated colors
     *
     * @param color1 start color value(normalized)
     * @param color2 end color value(normalized)
     * @return float buffer containing the sphere vertices and the associated colors
     */
    private FloatBuffer getSphereVertices(float[] color1, float[] color2, float[] color3,
                                          boolean gradient, int nbColors,
                                          int nbVertices, float angleStep, float radius) {
        float angleA, angleB;
        float cos, sin;
        float r1, r2;
        float h1, h2;

        FloatBuffer vBuf;
        float[][] v = new float[nbVertices][10];

        vBuf = ByteBuffer.allocateDirect(v.length * v[0].length * NB_BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vBuf.position(0);

        float[] colorStep = new float[]{0.0f, 0.0f, 0.0f};
        if (gradient) {
            if (nbColors == 2) {
                colorStep = calculateColorSteps(color1, color2, 1.0f, angleStep);
            } else if (nbColors == 3) {
                colorStep = calculateColorSteps(color1, color2, 2.0f, angleStep);
            }
        }

        float[] color = new float[]{color1[0],color1[1],color1[2]};
        float[] colorPrevious = new float[]{color1[0],color1[1],color1[2]};

        for (angleA = -90.0f; angleA < 90.0f; angleA += angleStep) {
            int n = 0;

            r1 = (float) Math.cos(angleA * Math.PI / 180.0);
            r2 = (float) Math.cos((angleA + angleStep) * Math.PI / 180.0);
            h1 = (float) Math.sin(angleA * Math.PI / 180.0);
            h2 = (float) Math.sin((angleA + angleStep) * Math.PI / 180.0);

            if ((! gradient) && (nbColors > 1)) {
                // Case color rings
                if ((angleA < -85.0f) || (angleA > 85.0f)) {
                    if (nbColors == 3) {
                        color[0] = color3[0];
                        color[1] = color3[1];
                        color[2] = color3[2];
                    } else {
                        color[0] = color1[0];
                        color[1] = color1[1];
                        color[2] = color1[2];
                    }
                } else if (((angleA >= -85.0f) && (angleA < -75.0f)) || ((angleA <= 85.0f) && (angleA > 75.0f))) {
                    color[0] = color2[0];
                    color[1] = color2[1];
                    color[2] = color2[2];
                } else if (((angleA >= -75.0f) && (angleA < -65.0f)) || ((angleA <= 75.0f) && (angleA > 65.0f))) {
                    color[0] = color1[0];
                    color[1] = color1[1];
                    color[2] = color1[2];
                } else if (((angleA >= -65.0f) && (angleA < -55.0f)) || ((angleA <= 65.0f) && (angleA > 55.0f))) {
                    color[0] = color2[0];
                    color[1] = color2[1];
                    color[2] = color2[2];
                } else if (((angleA >= -55.0f) && (angleA < -45.0f)) || ((angleA <= 55.0f) && (angleA > 45.0f))) {
                    color[0] = color1[0];
                    color[1] = color1[1];
                    color[2] = color1[2];
                } else if (((angleA >= -45.0f) && (angleA < -35.0f)) || ((angleA <= 45.0f) && (angleA > 35.0f))) {
                    color[0] = color2[0];
                    color[1] = color2[1];
                    color[2] = color2[2];
                } else if (((angleA >= -35.0f) && (angleA < -25.0f)) || ((angleA <= 35.0f) && (angleA > 25.0f))) {
                    color[0] = color1[0];
                    color[1] = color1[1];
                    color[2] = color1[2];
                } else if (((angleA >= -25.0f) && (angleA < -15.0f)) || ((angleA <= 25.0f) && (angleA > 15.0f))) {
                    color[0] = color2[0];
                    color[1] = color2[1];
                    color[2] = color2[2];
                } else if (((angleA >= -15.0f) && (angleA < -5.0f)) || ((angleA <= 15.0f) && (angleA > 5.0f))) {
                    color[0] = color1[0];
                    color[1] = color1[1];
                    color[2] = color1[2];
                } else {
                    if (nbColors == 3) {
                        color[0] = color3[0];
                        color[1] = color3[1];
                        color[2] = color3[2];
                    } else {
                        color[0] = color2[0];
                        color[1] = color2[1];
                        color[2] = color2[2];
                    }
                }
                colorPrevious = color;
            }

            // Fixed latitude, 360 degrees rotation to traverse a weft
            for (angleB = 0.0f; angleB <= 360.0f; angleB += angleStep) {

                cos = (float) Math.cos(angleB * Math.PI / 180.0);
                sin = (float) Math.sin(angleB * Math.PI / 180.0);

                // coordinates XYZ
                v[n][0] = radius * r2 * cos;
                v[n][1] = radius * h2;
                v[n][2] = radius * r2 * sin;

                // normal XYZ
                v[n][3] = r2 * cos;
                v[n][4] = h2;
                v[n][5] = r2 * sin;

                // color RGBA
                v[n][6] = color[0];
                v[n][7] = color[1];
                v[n][8] = color[2];
                v[n][9] = 1.0f;

                // coordinates XYZ
                v[n + 1][0] = radius * r1 * cos;
                v[n + 1][1] = radius * h1;
                v[n + 1][2] = radius * r1 * sin;

                // normal XYZ
                v[n + 1][3] = r1 * cos;
                v[n + 1][4] = h1;
                v[n + 1][5] = r1 * sin;

                // color RGBA
                v[n + 1][6] = colorPrevious[0];
                v[n + 1][7] = colorPrevious[1];
                v[n + 1][8] = colorPrevious[2];
                v[n + 1][9] = 1.0f;

                vBuf.put(v[n]);
                vBuf.put(v[n + 1]);

                n += 2;
            }

            // Case color gradient between two poles
            if (gradient) {
                colorPrevious = color;
                if ((angleA == 0) && (nbColors == 3)) {
                    colorStep = calculateColorSteps(color2, color3, 2.0f, angleStep);
                }
                if ((angleA >= -85.0f) && (angleA <= 85.0f)) {
                    for (int i = 0; i < 3; i++) {
                        color[i] = color[i] + colorStep[i];
                        if (color[i] < 0.0f)
                            color[i] = 0.0f;
                    }
                }
            }
        }

        vBuf.position(0);
        return vBuf;
    }

    private FloatBuffer getSphereTextureCoordinates(int nbVertices, float angleStep) {
        float angleA, angleB;

        FloatBuffer vBuf;
        float[][] v = new float[nbVertices][2];

        vBuf = ByteBuffer.allocateDirect(v.length * v[0].length * NB_BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vBuf.position(0);

        for (angleA = -90.0f; angleA < 90.0f; angleA += angleStep) {
            int n = 0;
            for (angleB = 0.0f; angleB <= 360.0f; angleB += angleStep) {
                v[n + 1][0] = angleB / 360.0f;
                v[n + 1][1] = 1.0f - ((angleA + 90.0f) / 180.0f);

                v[n][0] = angleB / 360.0f;
                v[n][1] = 1.0f - ((angleA + 90.0f + angleStep) / 180.0f);

                vBuf.put(v[n]);
                vBuf.put(v[n+1]);
                n += 2;
            }
        }

        vBuf.position(0);
        return vBuf;
    }

    /**
     * Calculate the RGBA delta expected between first and last vertices of the sphere
     *
     * @param color1 start color(normalized)
     * @param color2 end color(normalized)
     * @return RGBA steps
     */
    private float[] calculateColorSteps(float[] color1, float[] color2, float speed, float angleStep) {
        float[] colorStep = new float[3];
        for (int i=0; i < 3; i++) {
            colorStep[i] = (color2[i] - color1[i]) * (speed * angleStep / 180);
        }
        return colorStep;
    }

    /**
     * Get back position of the satellite
     * @return updated position which shall be applied to the translation
     */
    private float[] getPositionSatellite(long deltaTime) {
        float[] position = new float[3];

        float angle = getDeltaTotalWithSpeed(deltaTime);

        mSatelliteAngle += angle;
        if (mSatelliteAngle < 0.0f) {
            mSatelliteAngle += 360.0f;
        }
        if (mSatelliteAngle > 360.0f) {
            mSatelliteAngle -= 360.0f;
        }

        float cos = (float) Math.cos(mSatelliteAngle * Math.PI / 180.0);
        float sin = (float) Math.sin(mSatelliteAngle * Math.PI / 180.0);

        float cos2 = (float) Math.cos(SATELLITE_ORBIT_ANGLE * Math.PI / 180.0);
        float sin2 = (float) Math.sin(SATELLITE_ORBIT_ANGLE * Math.PI / 180.0);

        position[0] = SATELLITE_ORBIT_RADIUS * cos * cos2;
        position[1] = SATELLITE_ORBIT_RADIUS * cos * sin2;
        position[2] = SATELLITE_ORBIT_RADIUS * sin;

        return position;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to white.
        GLES20.glClearColor(1f, 1f, 1f, 1f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);

        // Enable depth test (tracks vertex's distance to the viewer)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // LESS (default value), passes if vertex's distance (depth) less than stored one
        GLES20.glDepthFunc(GLES20.GL_LESS);

        // initialize fixed speed used for earth and satellite
        initFixedSpeed(10000.0f,0);
        initFixedSpeed(1500.0f,1);

        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        // Load in the vertex shader.
        mVertexShaderHandle = ShaderHelper.compileVertexShader(ShaderHelper.SHADER_TEXTURE_LIGHT);
        if (mVertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader.
        mFragmentShaderHandle = ShaderHelper.compileFragmentShader(ShaderHelper.SHADER_TEXTURE_LIGHT);
        if (mFragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create and link a program object and store the handle to it.
        mProgramHandle = ShaderHelper.linkProgram(ShaderHelper.SHADER_TEXTURE_LIGHT, mVertexShaderHandle, mFragmentShaderHandle);
        if (mProgramHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        Matrix.setIdentityM(mAccumulatedRotation, 0);

        initDelta();
        mSatelliteAngle = 0.0f;

        mSphere = new Sphere();
        mSphere.initSphere(mProgramHandle, mViewMatrix, mTextureEnabled, mLightEnabled);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Prepare texture unit (planet and satellite)
        int[] textureIds = {R.drawable.planet, R.drawable.logo_st_256};
        mTextureDataHandles = new int[textureIds.length];
        mTextureDataHandles[0] = TextureHelper.loadTexture(mContext, textureIds[0]);
        mTextureDataHandles[1] = TextureHelper.loadTexture(mContext, textureIds[1], 0.2f);

        mPreviousTime = SystemClock.uptimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSphere.updateSphere(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (isStationary()) {
            // do not re-draw the shape if not required
            return;
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        long time = SystemClock.uptimeMillis();

        float[] delta;

        if (mTextureEnabled) {
            float angleInDegrees = getAngleInDegreesFixedSpeed(0);

            // Construct satellite model matrix
            Matrix.setIdentityM(mModelSatelliteMatrix, 0);

            float[] satellitePos = getPositionSatellite(time - mPreviousTime);
            Matrix.translateM(mModelSatelliteMatrix,0, satellitePos[0], satellitePos[1], satellitePos[2]);

            float angleSatelliteInDegrees = getAngleInDegreesFixedSpeed(1);
            Matrix.rotateM(mModelSatelliteMatrix, 0, angleSatelliteInDegrees, 0.0f, 1.0f, 0.0f);

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);

        } else {
            delta = getDeltaAngleWithSpeed(time - mPreviousTime);

            Matrix.setIdentityM(mModelMatrix, 0);

            Matrix.setIdentityM(mCurrentRotation, 0);
            Matrix.rotateM(mCurrentRotation, 0, delta[0], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(mCurrentRotation, 0, delta[1], 1.0f, 0.0f, 0.0f);

            Matrix.multiplyMM(mAccumulatedRotation, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
            Matrix.multiplyMM(mModelMatrix, 0, mAccumulatedRotation, 0, mModelMatrix, 0);
        }

        mPreviousTime = time;

        // Light position (fixed)
        mLightPos[0] = 1.0f;
        mLightPos[1] = 1.0f;
        mLightPos[2] = 0.0f;
        mLightPos[3] = 0.0f;

        if (mTextureEnabled) {
            mSphere.draw(mVerticesNoColor, mTextureCoordinateBuffer, mTextureDataHandles, mLightPos, mModelMatrix, mNbVertices,0);
            if (isColored() && isColorGradient()) {
                mSphere.draw(mVerticesSatelliteColorGradient, mTextureSatelliteCoordinateBuffer, mTextureDataHandles, mLightPos, mModelSatelliteMatrix, mNbVerticesSatellite, 1);
            } else if (isColored() && ! isColorGradient()) {
                mSphere.draw(mVerticesSatelliteColor, mTextureSatelliteCoordinateBuffer, mTextureDataHandles, mLightPos, mModelSatelliteMatrix, mNbVerticesSatellite, 1);
            } else {
                mSphere.draw(mVerticesSatelliteNoColor, mTextureSatelliteCoordinateBuffer, mTextureDataHandles, mLightPos, mModelSatelliteMatrix, mNbVerticesSatellite, 1);
            }
        } else {
            if (isColored() && isColorGradient()) {
                mSphere.draw(mVerticesColorGradient, mTextureCoordinateBuffer, mTextureDataHandles, mLightPos, mModelMatrix, mNbVertices,0);
            } else if (isColored() && ! isColorGradient()) {
                mSphere.draw(mVerticesColor, mTextureCoordinateBuffer, mTextureDataHandles, mLightPos, mModelMatrix, mNbVertices,0);
            } else {
                mSphere.draw(mVerticesNoColor, mTextureCoordinateBuffer, mTextureDataHandles, mLightPos, mModelMatrix, mNbVertices,0);
            }
        }
    }

    @Override
    public void setTextureState(boolean state) {
        if (mSphere != null) {
            mSphere.setTextureState(state);
        }
        mTextureEnabled = state;
    }

    @Override
    public void setLightState(boolean state) {
        if (mSphere != null) {
            mSphere.setLightState(state);
        }
        mLightEnabled = state;
    }

    @Override
    public void cancelRendering() {
        GLES20.glDetachShader(mProgramHandle, mVertexShaderHandle);
        GLES20.glDeleteShader(mVertexShaderHandle);

        GLES20.glDetachShader(mProgramHandle, mFragmentShaderHandle);
        GLES20.glDeleteShader(mFragmentShaderHandle);

        GLES20.glDeleteProgram(mProgramHandle);
    }

    @Override
    public boolean isKineticManaged() {
        return false;
    }

    @Override
    public boolean isLightManaged() {
        return true;
    }

}
