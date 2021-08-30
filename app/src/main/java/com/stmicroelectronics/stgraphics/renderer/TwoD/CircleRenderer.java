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
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL surface renderer for Circle
 */
public class CircleRenderer extends Shape2DRenderer {

    /** Circle radius ratio value (radius depends on the GLSurface area */
    final static float CIRCLE_RADIUS_RATIO = 0.6f;

    /** Store our model data in a float buffers */
    private final FloatBuffer mVerticesBuffer;
    private final FloatBuffer mNoColorBuffer;
    private final FloatBuffer mColorBuffer;
    private final FloatBuffer mColorGradientBuffer;
    private final FloatBuffer mTextureCoordinateBuffer;

    private final ShortBuffer mDrawOrderBuffer;

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world
     * space to eye space; it positions things relative to our eye.
     */
    private final float[] mViewMatrix = new float[16];

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each
     * model can be thought of being located at the center of the universe) to world space.
     */
    private final float[] mModelMatrix = new float[16];

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    private int mProgramHandle;
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;

    private final Context mContext;
    private Circle mCircle;

    private boolean mTextureEnabled = false;

    public CircleRenderer(Context context) {
        mContext = context;

        float[] colorLight, color1, color2, color3, color4;

        colorLight = Utility.normalizeColor(mContext.getColor(R.color.colorLightBlue));

        color1 = Utility.normalizeColor(mContext.getColor(R.color.colorShape1));
        color2 = Utility.normalizeColor(mContext.getColor(R.color.colorShape2));
        color3 = Utility.normalizeColor(mContext.getColor(R.color.colorShape3));
        color4 = Utility.normalizeColor(mContext.getColor(R.color.colorShape4));

        // How many bytes per float
        int bytesPerFloat = 4;

        // Circle is in the middle of a Square
        final float[] squareVertices = {
                // X, Y, Z
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f};

        final float[] squareColorGradient = {
                // R, G, B, A
                color1[0], color1[1], color1[2], color1[3],
                color3[0], color3[1], color3[2], color3[3],
                color2[0], color2[1], color2[2], color2[3],
                color4[0], color4[1], color4[2], color4[3]};

        final float[] squareColor = {
                // R, G, B, A
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3],
                color3[0], color3[1], color3[2], color3[3]};

        final float[] squareNoColor = {
                // R, G, B, A
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3],
                colorLight[0], colorLight[1], colorLight[2], colorLight[3]};

        final float[] squareTextureCoordinate = {
                // X, Y
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f};

        // square = two triangles
        final short[] squareDrawOrder = new short[]{0, 1, 2, 0, 2, 3};

        mVerticesBuffer = ByteBuffer.allocateDirect(squareVertices.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesBuffer.put(squareVertices).position(0);

        mNoColorBuffer = ByteBuffer.allocateDirect(squareNoColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNoColorBuffer.put(squareNoColor).position(0);

        mColorBuffer = ByteBuffer.allocateDirect(squareColor.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorBuffer.put(squareColor).position(0);

        mColorGradientBuffer = ByteBuffer.allocateDirect(squareColorGradient.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorGradientBuffer.put(squareColorGradient).position(0);

        mTextureCoordinateBuffer = ByteBuffer.allocateDirect(squareTextureCoordinate.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoordinateBuffer.put(squareTextureCoordinate).position(0);

        mDrawOrderBuffer = ByteBuffer.allocateDirect(squareDrawOrder.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mDrawOrderBuffer.put(squareDrawOrder).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to white.
        GLES20.glClearColor(1f, 1f, 1f, 1f);

        // initialize time value
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

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        // Load in the vertex shader.
        mVertexShaderHandle = ShaderHelper.compileVertexShader(ShaderHelper.SHADER_TEXTURE_CIRCLE);
        if (mVertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader.
        mFragmentShaderHandle = ShaderHelper.compileFragmentShader(ShaderHelper.SHADER_TEXTURE_CIRCLE);
        if (mFragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create and link a program object and store the handle to it.
        mProgramHandle = ShaderHelper.linkProgram(ShaderHelper.SHADER_TEXTURE_CIRCLE,
                mVertexShaderHandle, mFragmentShaderHandle);
        if (mProgramHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        mCircle = new Circle();
        mCircle.initCircle(mProgramHandle, mViewMatrix, mTextureEnabled);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Prepare texture
        mTextureDataHandle = TextureHelper.loadTexture(mContext, R.drawable.logo_st_256, 0.4f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCircle.updateCircle(width,height);
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
                mCircle.draw(mVerticesBuffer, mColorGradientBuffer, mTextureCoordinateBuffer, mTextureDataHandle,
                        mDrawOrderBuffer, mModelMatrix);
            } else {
                mCircle.draw(mVerticesBuffer, mColorBuffer, mTextureCoordinateBuffer, mTextureDataHandle,
                        mDrawOrderBuffer, mModelMatrix);
            }
        } else {
            mCircle.draw(mVerticesBuffer, mNoColorBuffer, mTextureCoordinateBuffer, mTextureDataHandle,
                    mDrawOrderBuffer, mModelMatrix);
        }
    }

    @Override
    public void setTextureState(boolean state) {
        if (mCircle != null) {
            mCircle.setTextureState(state);
        }
        mTextureEnabled = state;
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
}
