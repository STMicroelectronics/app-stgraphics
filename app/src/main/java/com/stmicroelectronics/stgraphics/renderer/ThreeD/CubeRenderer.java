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

public class CubeRenderer extends Shape3DRenderer{

    /** Store our model data in a float buffer */
    final private int VERTICES_ARRAY_SIZE = 8;
    private final FloatBuffer[] mVerticesArray = new FloatBuffer[VERTICES_ARRAY_SIZE];
    private int mVerticesIndex = 0;

    private final FloatBuffer mNormalsBuffer;
    private final FloatBuffer mNoColorBuffer;
    private final FloatBuffer mColorBuffer;
    private final FloatBuffer mColorGradientBuffer;
    private final FloatBuffer mTextureCoordinateBuffer;

    private final ShortBuffer mDrawOrderBuffer;

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
    private Cube mCube;

    private boolean mTextureEnabled = false;
    private boolean mLightEnabled = false;

    private final float[] mAccumulatedRotation = new float[16];
    private final float[] mCurrentRotation = new float[16];

    public CubeRenderer(Context context) {
        mContext = context;

        float[] colorLight, color1, color2, color3, color4;

        colorLight = Utility.normalizeColor(mContext.getColor(R.color.colorLightBlue));

        color1 = Utility.normalizeColor(mContext.getColor(R.color.colorShape1));
        color2 = Utility.normalizeColor(mContext.getColor(R.color.colorShape2));
        color3 = Utility.normalizeColor(mContext.getColor(R.color.colorShape3));
        color4 = Utility.normalizeColor(mContext.getColor(R.color.colorShape4));

        /* How many bytes per float. */
        int bytesPerFloat = 4;

        final float[] cubeVertices = {
                // X, Y, Z,
                -0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, -0.5f, 0.5f};

        final float[] cubeNormals = {
                // X, Y, Z,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f};

        final float[] cubeColor = {
                // R, G, B, A (front)
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                // R, G, B, A (left)
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                // R, G, B, A (right)
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                // R, G, B, A (back)
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                // R, G, B, A (top)
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                // R, G, B, A (bottom)
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f};

        final float[] cubeColorGradient = {
                // R, G, B, A (front)
                color1[0], color1[1], color1[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color2[0], color2[1], color2[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                // R, G, B, A (left)
                color2[0], color2[1], color2[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                // R, G, B, A (right)
                color2[0], color2[1], color2[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                // R, G, B, A (back)
                color1[0], color1[1], color1[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f,
                color2[0], color2[1], color2[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                // R, G, B, A (top)
                color2[0], color2[1], color2[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color1[0], color1[1], color1[2], 1.0f,
                color2[0], color2[1], color2[2], 1.0f,
                // R, G, B, A (bottom)
                color3[0], color3[1], color3[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color4[0], color4[1], color4[2], 1.0f,
                color3[0], color3[1], color3[2], 1.0f};

        final float[] cubeNoColor = {
                // R, G, B, A (front)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                // R, G, B, A (left)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                // R, G, B, A (right)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                // R, G, B, A (back)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                // R, G, B, A (top)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                // R, G, B, A (bottom)
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f,
                colorLight[0], colorLight[1], colorLight[2], 1.0f};

        final float[] squareTextureCoordinate = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f};

        // square = two triangles
        final short[] cubeDrawOrder = new short[]{0, 1, 2, 1, 3, 2};
        FloatBuffer verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(cubeVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(cubeVertices).position(0);

        mVerticesArray[0] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(cubeVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(cubeVertices,0.05f)).position(0);

        mVerticesArray[1] = verticesBuffer;
        mVerticesArray[7] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(cubeVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(cubeVertices,0.1f)).position(0);

        mVerticesArray[2] = verticesBuffer;
        mVerticesArray[6] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(cubeVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(cubeVertices,0.15f)).position(0);

        mVerticesArray[3] = verticesBuffer;
        mVerticesArray[5] = verticesBuffer;

        verticesBuffer = ByteBuffer.allocateDirect(cubeVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(Utility.zTranslateVertices(cubeVertices,0.2f)).position(0);

        mVerticesArray[4] = verticesBuffer;

        mNormalsBuffer = ByteBuffer.allocateDirect(cubeNormals.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormalsBuffer.put(cubeNormals).position(0);

        mNoColorBuffer = ByteBuffer.allocateDirect(cubeNoColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNoColorBuffer.put(cubeNoColor).position(0);

        mColorBuffer = ByteBuffer.allocateDirect(cubeColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorBuffer.put(cubeColor).position(0);

        mColorGradientBuffer = ByteBuffer.allocateDirect(cubeColorGradient.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorGradientBuffer.put(cubeColorGradient).position(0);

        mTextureCoordinateBuffer = ByteBuffer.allocateDirect(squareTextureCoordinate.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoordinateBuffer.put(squareTextureCoordinate).position(0);

        mDrawOrderBuffer = ByteBuffer.allocateDirect(cubeDrawOrder.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mDrawOrderBuffer.put(cubeDrawOrder).position(0);
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
        final float eyeZ = 2f;

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

        mCube = new Cube();
        mCube.initCube(mProgramHandle, mViewMatrix, mTextureEnabled, mLightEnabled);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Prepare texture unit
        int[] textureIds = {R.drawable.logo_st_256, R.drawable.logo_stm32_256, R.drawable.logo_stm32mp1_256};
        mTextureDataHandles = TextureHelper.loadTextures(mContext, textureIds, textureIds.length);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCube.updateCube(width,height);
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
                mCube.draw(mVerticesArray[mVerticesIndex], mNormalsBuffer, mColorGradientBuffer, mTextureCoordinateBuffer, mTextureDataHandles,
                        mDrawOrderBuffer, mLightPos, mModelMatrix);
            } else {
                mCube.draw(mVerticesArray[mVerticesIndex], mNormalsBuffer, mColorBuffer, mTextureCoordinateBuffer, mTextureDataHandles,
                        mDrawOrderBuffer, mLightPos, mModelMatrix);
            }
        } else {
            mCube.draw(mVerticesArray[mVerticesIndex], mNormalsBuffer, mNoColorBuffer, mTextureCoordinateBuffer, mTextureDataHandles,
                    mDrawOrderBuffer, mLightPos, mModelMatrix);
        }
        updateVerticesIndex();
    }

    @Override
    public void setTextureState(boolean state) {
        if (mCube != null) {
            mCube.setTextureState(state);
        }
        mTextureEnabled = state;
    }

    @Override
    public void setLightState(boolean state) {
        if (mCube != null) {
            mCube.setLightState(state);
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
}
