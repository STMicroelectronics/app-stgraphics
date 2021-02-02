package com.stmicroelectronics.stgraphics.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Helper used to manage textures
 */
public class TextureHelper {

    /**
     * Load texture in the texture unit 0
     *
     * @param context current application context
     * @param resourceId required drawable resource (id)
     * @return texture handle
     */
    public static int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    /**
     * Load several textures in the texture unit 0
     *
     * @param context current application context
     * @param resourceIds required drawable resources (id)
     * @param nbTextures number of textures which shall be loaded
     * @return texture handle
     */
    public static int[] loadTextures(final Context context, final int[] resourceIds, int nbTextures)
    {
        final int[] textureHandle = new int[nbTextures];

        GLES20.glGenTextures(nbTextures, textureHandle, 0);

        for (int i = 0; i < nbTextures; i++) {

            if (textureHandle[i] != 0) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceIds[i], options);

                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[i]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }

            if (textureHandle[i] == 0) {
                throw new RuntimeException("Error loading texture.");
            }
        }
        return textureHandle;
    }


    /**
     * Load a rescaled texture in the texture unit 0
     *
     * @param context current application context
     * @param resourceId required drawable resource (id)
     * @param resize rescale ratio (shall be < 1.0f)
     * @return texture handle
     */
    public static int loadTexture(final Context context, final int resourceId, float resize)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = centerBitmap(BitmapFactory.decodeResource(context.getResources(), resourceId, options), resize);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    /**
     * Load several rescaled textures in the texture unit 0
     *
     * @param context current application context
     * @param resourceIds required drawable resources (id)
     * @param nbTextures number of textures which shall be loaded
     * @param resize rescale ratio (shall be < 1.0f)
     * @return texture handle
     */
    public static int[] loadTextures(final Context context, final int[] resourceIds, int nbTextures , float resize)
    {
        final int[] textureHandle = new int[nbTextures];

        GLES20.glGenTextures(nbTextures, textureHandle, 0);

        for (int i = 0; i < nbTextures; i++) {

            if (textureHandle[i] != 0) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                final Bitmap bitmap = centerBitmap(BitmapFactory.decodeResource(context.getResources(), resourceIds[i], options), resize);

                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[i]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }

            if (textureHandle[i] == 0) {
                throw new RuntimeException("Error loading texture.");
            }
        }
        return textureHandle;
    }

    /**
     * Rescale received bitmap and center it in another bitmap of same size than the original one
     *
     * @param source bitmap which shall be rescaled
     * @param resize rescale ratio (shall be < 1.0f)
     * @return bitmap created
     */
    private static Bitmap centerBitmap(Bitmap source, float resize) {
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(bitmap);

        int left = (canvas.getWidth() - (int)(source.getWidth() * resize)) / 2;
        int top = (canvas.getHeight() - (int)(source.getHeight() * resize)) / 2;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(resize, resize, left, top);
        canvas.setMatrix(scaleMatrix);

        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(source, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        source.recycle();
        return bitmap;
    }

}
