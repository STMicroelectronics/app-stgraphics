package com.stmicroelectronics.stgraphics.renderer.ThreeD;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.stmicroelectronics.stgraphics.R;
import com.stmicroelectronics.stgraphics.utils.ShaderHelper;
import com.stmicroelectronics.stgraphics.utils.TextureHelper;
import com.stmicroelectronics.stgraphics.utils.Utility;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PyramidRenderer extends Shape3DRenderer {

    /** Store our model data in a float buffer */
    // private final FloatBuffer mVerticesBuffer;
    final private int VERTICES_ARRAY_SIZE = 8;
    private final FloatBuffer[] mVerticesArray = new FloatBuffer[VERTICES_ARRAY_SIZE];
    private int mVerticesIndex = 0;

    private final FloatBuffer mNormalsBuffer;
    private final FloatBuffer mNoColorBuffer;
    private final FloatBuffer mColorBuffer;
    private final FloatBuffer mColorGradientBuffer;
    private final FloatBuffer mTextureCoordinateBuffer;

    private final FloatBuffer mBaseVerticesBuffer;
    private final FloatBuffer mBaseNormalsBuffer;
    private final FloatBuffer mBaseColorBuffer;
    private final FloatBuffer mBaseNoColorBuffer;

    private final ShortBuffer mBaseDrawOrderBuffer;

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

    /**
     * Store the light position.
     */
    private final float[] mLightPos = new float[4];

    /** This is a handle to our texture data. */
    private int[] mTextureDataHandles;

    private int mProgramHandle;
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;

    private final Context mContext;
    private final Pyramid mPyramid;

    private boolean mTextureEnabled = false;
    private boolean mLightEnabled = false;

    private final float[] mAccumulatedRotation = new float[16];
    private final float[] mCurrentRotation = new float[16];

    public PyramidRenderer(Context context) {
        mContext = context;

        float[] colorLight, color1, color3, color4;

        colorLight = Utility.normalizeColor(mContext.getColor(R.color.colorLightBlue));

        color1 = Utility.normalizeColor(mContext.getColor(R.color.colorShape1));
        color3 = Utility.normalizeColor(mContext.getColor(R.color.colorShape3));
        color4 = Utility.normalizeColor(mContext.getColor(R.color.colorShape4));

        /* How many bytes per float. */
        int bytesPerFloat = 4;

        // Create pyramid side (triangles) buffers

        /* Equilateral Triangle
         * h = (square_root(3) / 2) *  a
         * z (pyramid case) = 1/3 * h */
        float[] triangleVertices = {
                // X, Y, Z (a = 2 * 0.7f)
                -0.7f, -0.4041452f, 0.7f,
                0.7f, -0.4041452f, 0.7f,
                0.0f, 0.8082904f, 0.0f};

        float[] triangleNormals = {
                // X, Y, Z
                0.0f, 0.57735f, 1.0f,
                0.0f, 0.57735f, 1.0f,
                0.0f, 0.57735f, 1.0f};

        float[] triangleColor = {
                // R, G, B, A (front)
                color1[0], color1[1], color1[2], color1[3],
                color1[0], color1[1], color1[2], color1[3],
                color1[0], color1[1], color1[2], color1[3],
                // R, G, B, A (left)
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                // R, G, B, A (back)
                color1[0], color1[1], color1[2], color1[3],
                color1[0], color1[1], color1[2], color1[3],
                color1[0], color1[1], color1[2], color1[3],
                // R, G, B, A (right)
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3]};

        float[] triangleColorGradient = {
                // R, G, B, A (front)
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color3[0], color3[1], color3[2], color3[3],
                // R, G, B, A (left)
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color3[0], color3[1], color3[2], color3[3],
                // R, G, B, A (back)
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color3[0], color3[1], color3[2], color3[3],
                // R, G, B, A (right)
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color3[0], color3[1], color3[2], color3[3]};

        float[] triangleNoColor = {
                // R, G, B, A (front)
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                // R, G, B, A (left)
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                // R, G, B, A (back)
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                // R, G, B, A (right)
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3]};

        /*
         * Texture size = square of 1.0f
         * h(triangle) = 3/2 of the square half size (3/2 * 0.5f = 0.75f)
         * Coordinates are then (target center of the texture = 2/3 of the triangle height):
         * (0.5f - (h / square_root(3)), 0.75)
         * (0.5f + (h / square_root(3)), 0.75)
         * (0.5f, 0.0f)
         */
        final float[] triangleTextureCoordinate = {
                // X, Y
                0.0669873f, 0.75f,
                0.9330127f, 0.75f,
                0.5f, 0.0f};

        FloatBuffer verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(triangleVertices).position(0);

        mVerticesArray[0] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(triangleVertices,0.05f)).position(0);

        mVerticesArray[1] = verticesBuffer;
        mVerticesArray[7] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(triangleVertices,0.1f)).position(0);

        mVerticesArray[2] = verticesBuffer;
        mVerticesArray[6] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(triangleVertices,0.15f)).position(0);

        mVerticesArray[3] = verticesBuffer;
        mVerticesArray[5] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(triangleVertices,0.2f)).position(0);

        mVerticesArray[4] = verticesBuffer;

        mNormalsBuffer = ByteBuffer.allocateDirect(triangleNormals.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormalsBuffer.put(triangleNormals).position(0);

        mNoColorBuffer = ByteBuffer.allocateDirect(triangleNoColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNoColorBuffer.put(triangleNoColor).position(0);

        mColorBuffer = ByteBuffer.allocateDirect(triangleColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorBuffer.put(triangleColor).position(0);

        mColorGradientBuffer = ByteBuffer.allocateDirect(triangleColorGradient.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorGradientBuffer.put(triangleColorGradient).position(0);

        mTextureCoordinateBuffer = ByteBuffer.allocateDirect(triangleTextureCoordinate.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoordinateBuffer.put(triangleTextureCoordinate).position(0);

        // Create pyramid base (square) buffers

        final float[] squareVertices = {
                // X, Y, Z
                -0.7f, -0.4041452f, 0.7f,
                -0.7f, -0.4041452f, -0.7f,
                0.7f, -0.4041452f, -0.7f,
                0.7f, -0.4041452f, 0.7f};

        final float[] squareNormals = {
                // X, Y, Z
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f};

        final float[] squareColor = {
                // R, G, B, A
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3],
                color4[0], color4[1], color4[2], color4[3]};

        final float[] squareNoColor = {
                // R, G, B, A
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3]};

        final short[] squareDrawOrder = new short[]{0, 1, 2, 0, 2, 3};

        mBaseVerticesBuffer = ByteBuffer.allocateDirect(squareVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBaseVerticesBuffer.put(squareVertices).position(0);

        mBaseNormalsBuffer = ByteBuffer.allocateDirect(squareNormals.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBaseNormalsBuffer.put(squareNormals).position(0);

        mBaseColorBuffer = ByteBuffer.allocateDirect(squareColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBaseColorBuffer.put(squareColor).position(0);

        mBaseNoColorBuffer = ByteBuffer.allocateDirect(squareNoColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBaseNoColorBuffer.put(squareNoColor).position(0);

        mBaseDrawOrderBuffer = ByteBuffer.allocateDirect(squareDrawOrder.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mBaseDrawOrderBuffer.put(squareDrawOrder).position(0);

        mPyramid = new Pyramid();
    }

    /**
     * Update vertices index (loop)
     */
    private void updateVerticesIndex() {
        if (isStarted()) {
            if (ismKineticEnable() || mVerticesIndex != 0) {
                mVerticesIndex++;
                if (mVerticesIndex >= VERTICES_ARRAY_SIZE) {
                    mVerticesIndex = 0;
                }
            }
        }
    }

    @Override
    public void setTextureState(boolean state) {
        if (mPyramid != null) {
            mPyramid.setTextureState(state);
        }
        mTextureEnabled = state;
    }

    @Override
    public void setLightState(boolean state) {
        if (mPyramid != null) {
            mPyramid.setLightState(state);
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
        return true;
    }

    @Override
    public boolean isLightManaged() {
        return true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to white.
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Here culling is not enabled to avoid removing back faces.
        // GLES20.glEnable(GLES20.GL_CULL_FACE);
        // GLES20.glCullFace(GLES20.GL_BACK);
        // GLES20.glFrontFace(GLES20.GL_CCW);

        // Enable depth test (tracks vertex's distance to the viewer)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // LESS (default value), passes if vertex's distance (depth) less than stored one
        GLES20.glDepthFunc(GLES20.GL_LESS);

        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 2.1f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -4.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
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

        mPyramid.initPyramid(mProgramHandle, mViewMatrix, mTextureEnabled, mLightEnabled);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Prepare texture unit
        int[] textureIds = {R.drawable.logo_st_256, R.drawable.logo_stm32mp1_256};

        mTextureDataHandles = TextureHelper.loadTextures(mContext, textureIds, textureIds.length, 0.25f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mPyramid.updatePyramid(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (isStationary()) {
            return;
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        float[] delta = getDeltaAngle();

        Matrix.setIdentityM(mModelMatrix, 0);

        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, - delta[0], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(mCurrentRotation, 0, - delta[1], 1.0f, 0.0f, 0.0f);

        Matrix.multiplyMM(mAccumulatedRotation, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
        Matrix.multiplyMM(mModelMatrix, 0, mAccumulatedRotation, 0, mModelMatrix, 0);

        // Light position (fixed)
        mLightPos[0] = 1.0f;
        mLightPos[1] = 1.0f;
        mLightPos[2] = 0.0f;
        mLightPos[3] = 0.0f;

        if (isColored()) {
            if (isColorGradient()) {
                mPyramid.drawSide(mVerticesArray[mVerticesIndex], mNormalsBuffer, mColorGradientBuffer, mTextureCoordinateBuffer,
                        mTextureDataHandles, mLightPos, mModelMatrix);
            } else {
                mPyramid.drawSide(mVerticesArray[mVerticesIndex], mNormalsBuffer, mColorBuffer, mTextureCoordinateBuffer,
                        mTextureDataHandles, mLightPos, mModelMatrix);
            }
            mPyramid.drawBase(mBaseVerticesBuffer, mBaseNormalsBuffer, mBaseColorBuffer, mBaseDrawOrderBuffer, mLightPos, mModelMatrix);
        } else {
            mPyramid.drawSide(mVerticesArray[mVerticesIndex], mNormalsBuffer, mNoColorBuffer, mTextureCoordinateBuffer,
                    mTextureDataHandles,  mLightPos, mModelMatrix);
            mPyramid.drawBase(mBaseVerticesBuffer, mBaseNormalsBuffer, mBaseNoColorBuffer, mBaseDrawOrderBuffer, mLightPos, mModelMatrix);
        }
        updateVerticesIndex();
    }
}
