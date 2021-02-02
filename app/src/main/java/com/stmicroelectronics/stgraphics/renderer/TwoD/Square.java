package com.stmicroelectronics.stgraphics.renderer.TwoD;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Draw Square (GLES 2.0)
 */
class Square {

    private boolean mTextureEnabled;

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private final float[] mMVPMatrix = new float[16];

    /** This will be used to pass in model the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in model the position information. */
    private int mPositionHandle;

    /** This will be used to pass in model the color information. */
    private int mColorHandle;

    /** This will be used to pass in model the texture information. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model the texture state information. */
    private int mTextStateUniformHandle;

    /** This will be used to pass in model the texture coordinate information. */
    private int mTextureCoordinateHandle;

    private final float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    /**
     * Initialize Square parameters
     *
     * @param programHandle linked vertex + fragment shader program
     * @param viewMatrix view matrix
     * @param texture initial texture state (true = texture enabled in shader)
     */
    void initSquare(int programHandle, float[] viewMatrix, boolean texture) {
        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");

        mTextureUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_Texture");
        mTextStateUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_TextState");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoordinate");

        mTextureEnabled = texture;

        mViewMatrix = viewMatrix;
    }

    /**
     * Update Circle parameters depending on surface parameters
     *
     * @param width GLSurface width
     * @param height GLSurface height
     */
    void updateSquare(int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 10.0f);
    }

    /**
     * Give Texture state (used to enable texture in shader)
     *
     * @param state The texture state (true = texture enabled)
     */
    void setTextureState(boolean state) {
        mTextureEnabled = state;
    }

    /**
     * Draws a square from the given vertex data.
     *
     * @param aVerticesBuffer The buffer containing the vertex data.
     * @param aColorBuffer The buffer containing the color data.
     * @param aTextureCoordinateBuffer The buffer containing the texture coordinates data.
     * @param textureDataHandle The handle associated with the texture handle.
     * @param drawOrderBuffer The buffer containing vertices draw order.
     * @param modelMatrix The model matrix (rotation data).
     */
    void draw(final FloatBuffer aVerticesBuffer, final FloatBuffer aColorBuffer,
              final FloatBuffer aTextureCoordinateBuffer, final int textureDataHandle,
              final ShortBuffer drawOrderBuffer, float[] modelMatrix){

        // Pass in the vertices attributes (X,Y,Z)
        aVerticesBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                0, aVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color attributes (R,G,B)
        aColorBuffer.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false,
                0, aColorBuffer);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the texture coordinates (X,Y)
        aTextureCoordinateBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false,
                0, aTextureCoordinateBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // Pass in the texture state (on or off)
        if (mTextureEnabled) {
            GLES20.glUniform1i(mTextStateUniformHandle, 1);
        } else {
            GLES20.glUniform1i(mTextStateUniformHandle, 0);
        }

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, modelMatrix, 0);

        // This multiplies the model-view matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw following order buffer (6 elements)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,6,GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer);
    }

}
