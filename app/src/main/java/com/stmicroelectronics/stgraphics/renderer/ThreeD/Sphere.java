package com.stmicroelectronics.stgraphics.renderer.ThreeD;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

/**
 * Draw Sphere (GLES 2.0)
 */
class Sphere {
    private boolean mTextureEnabled;
    private boolean mLightEnabled;

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private final float[] mMVPMatrix = new float[16];

    /** This will be used to pass in model the transformation matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in model the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in model the position information. */
    private int mPositionHandle;

    /** This will be used to pass in model the normal information. */
    private int mNormalHandle;

    /** This will be used to pass in model the color information. */
    private int mColorHandle;

    /** This will be used to pass in model the texture information. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model the texture state information. */
    private int mTextStateUniformHandle;

    /** This will be used to pass in model the texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** This will be used to pass in model the light coordinate information. */
    private int mLightPosUniformHandle;

    /** This will be used to pass in model the light state information. */
    private int mLightStateUniformHandle;

    private final float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private final float[] mLightPos = new float[4];

    /**
     * Initialize Sphere parameters
     *
     * @param programHandle linked vertex + fragment shader program
     * @param viewMatrix view matrix
     */
    void initSphere(int programHandle, float[] viewMatrix, boolean texture, boolean light) {
        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVMatrix");

        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");

        mTextureUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_Texture");
        mTextStateUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_TextState");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoordinate");

        mLightPosUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos");
        mLightStateUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_LightState");

        mViewMatrix = viewMatrix;
        mTextureEnabled = texture;
        mLightEnabled = light;
    }

    /**
     * Update Sphere parameters depending on surface parameters
     *
     * @param width GLSurface width
     * @param height GLSurface height
     */
    void updateSphere(int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        final float ratio = (float) width / height;

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.

        // Case frustum projection (preferred for close scene)
        //   left The minimum range of the x-axis
        //   right The maximum range of the x-axis
        //   bottom The minimum range of the y-axis
        //   top The maximum range of the y-axis
        //   near The distance to the near plane (projection)
        //   far The distance to the far plane (projection)
        // Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 3.0f);

        // Case orthogonal projection (preferred for distant scene as planet)
        //   left The minimum range of the x-axis
        //   right The maximum range of the x-axis
        //   bottom The minimum range of the y-axis
        //   top The maximum range of the y-axis
        //   near The minimum range of the z-axis
        //   far The maximum range of the z-axis
        Matrix.orthoM(mProjectionMatrix,0, -ratio, ratio,-1.0f,1.0f,-3.0f,3.0f);
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
     * Give light state (used to enable light in shader)
     *
     * @param state The light state (true = texture enabled)
     */
    void setLightState(boolean state) {
        mLightEnabled = state;
    }

    /**
     * Draws a sphere from the given vertex data.
     *
     * @param aVerticesBuffer The buffer containing vertices and associated colors.
     * @param aTextureCoordinateBuffer The buffer containing the texture coordinates data.
     * @param textureDataHandle The handle associated with the texture handle.
     * @param lightPos The light position in eye space.
     * @param modelMatrix The model matrix (rotation data).
     * @param count The number of vertices which shall be drawn.
     * @param index The selected texture.
     */
    void draw(final FloatBuffer aVerticesBuffer, final FloatBuffer aTextureCoordinateBuffer,
              final int[] textureDataHandle, float[] lightPos, float[] modelMatrix,
              int count, int index){

        int bytesPerFloat = 4;

        // Pass in the position information
        aVerticesBuffer.position(0);
        /* Size of the position data in elements. */
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                10 * bytesPerFloat, aVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the normal information
        aVerticesBuffer.position(3);
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false,
                10 * bytesPerFloat, aVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // Pass in the color information
        aVerticesBuffer.position(6);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false,
                10 * bytesPerFloat, aVerticesBuffer);
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle[index]);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        if (mLightEnabled) {
            if (mTextureEnabled) {
                // Light not taken into account in this condition
                GLES20.glUniform1i(mLightStateUniformHandle, 0);
            } else {
                GLES20.glUniform1i(mLightStateUniformHandle, 1);
            }
        } else {
            GLES20.glUniform1i(mLightStateUniformHandle, 0);
        }

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMV(mLightPos, 0, mViewMatrix, 0, lightPos, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosUniformHandle, mLightPos[0], mLightPos[1], mLightPos[2]);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0, count);
    }

}
