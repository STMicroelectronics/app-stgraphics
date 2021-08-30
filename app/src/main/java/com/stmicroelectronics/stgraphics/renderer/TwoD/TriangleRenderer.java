package com.stmicroelectronics.stgraphics.renderer.TwoD;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL surface renderer for Triangle
 */
public class TriangleRenderer extends Shape2DRenderer {

    /** Store our model data in a float buffer. */
    private final FloatBuffer mVerticesBuffer;
    private final FloatBuffer mNoColorBuffer;
    private final FloatBuffer mColorBuffer;
    private final FloatBuffer mColorGradientBuffer;
    private final FloatBuffer mTextureCoordinateBuffer;

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

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    private int mProgramHandle;
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;

    private final Context mContext;
    private final Triangle mTriangle;

    private boolean mTextureEnabled = false;

    public TriangleRenderer(Context context) {
        mContext = context;

        float[] colorLight, color1, color2, color3;

        colorLight = Utility.normalizeColor(mContext.getColor(R.color.colorLightBlue));

        color1 = Utility.normalizeColor(mContext.getColor(R.color.colorShape1));
        color2 = Utility.normalizeColor(mContext.getColor(R.color.colorShape4));
        color3 = Utility.normalizeColor(mContext.getColor(R.color.colorShape3));

        /* How many bytes per float. */
        int bytesPerFloat = 4;

        /* Equilateral Triangle
        * h = (square_root(3) / 2) *  a */
        float[] triangleVertices = {
                // X, Y, Z
                -0.85f, -0.4907477f, 0.0f,
                0.85f, -0.4907477f, 0.0f,
                0.0f, 0.9814955f, 0.0f};

        float[] triangleNoColor = {
                // R, G, B, A
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3]};

        float[] triangleColor = {
                // R, G, B, A
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3]};

        float[] triangleColorGradient = {
                // R, G, B, A
                color1[0], color1[1], color1[2], color1[3],
                color2[0], color2[1], color2[2], color2[3],
                color3[0], color3[1], color3[2], color3[3]};

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

        mVerticesBuffer = ByteBuffer.allocateDirect(triangleVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesBuffer.put(triangleVertices).position(0);

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

        mTriangle = new Triangle();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to white.
        GLES20.glClearColor(1f, 1f, 1f, 1f);

        initTime();

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

        // Set the view matrix (represent the camera position)
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        // Load in the vertex shader.
        mVertexShaderHandle = ShaderHelper.compileVertexShader(ShaderHelper.SHADER_TEXTURE);
        if (mVertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader.
        mFragmentShaderHandle = ShaderHelper.compileFragmentShader(ShaderHelper.SHADER_TEXTURE);
        if (mFragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create and link a program object and store the handle to it.
        mProgramHandle = ShaderHelper.linkProgram(ShaderHelper.SHADER_TEXTURE, mVertexShaderHandle, mFragmentShaderHandle);
        if (mProgramHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        mTriangle.initTriangle(mProgramHandle, mViewMatrix, mTextureEnabled);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Prepare texture unit (resize required for logo case)
        mTextureDataHandle = TextureHelper.loadTexture(mContext, R.drawable.logo_st_256, 0.25f);
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
        return false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mTriangle.updateTriangle(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        float angleInDegrees = getAngleInDegrees();

        // Draw the triangle facing straight on.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);

        if (isColored()) {
            if (isColorGradient()) {
                mTriangle.drawTriangle(mVerticesBuffer, mColorGradientBuffer, mTextureCoordinateBuffer,
                        mTextureDataHandle, mModelMatrix);
            } else {
                mTriangle.drawTriangle(mVerticesBuffer, mColorBuffer, mTextureCoordinateBuffer,
                        mTextureDataHandle, mModelMatrix);
            }
        } else {
            mTriangle.drawTriangle(mVerticesBuffer, mNoColorBuffer, mTextureCoordinateBuffer,
                    mTextureDataHandle, mModelMatrix);
        }
    }

    @Override
    public void setTextureState(boolean state) {
        if (mTriangle != null) {
            mTriangle.setTextureState(state);
        }
        mTextureEnabled = state;
    }
}
