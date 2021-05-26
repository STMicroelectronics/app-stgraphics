package com.stmicroelectronics.stgraphics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.stmicroelectronics.stgraphics.renderer.GraphicsRenderer;
import com.stmicroelectronics.stgraphics.utils.Utility;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    GLSurfaceView mSurface;
    SwitchCompat mLightSwitch;
    SwitchCompat mColorsSwitch;
    SwitchCompat mColorGradientSwitch;
    SwitchCompat mTextureSwitch;
    SwitchCompat mKineticSwitch;
    SwitchCompat mDimensionsSwitch;
    Button mSwapShapesButton;
    LinearLayout mGraphicsList;

    private GraphicsRenderer mGraphicsRenderer;

    private Point mCenter;
    private Point mOrigin = new Point(0,0);
    private int mTouchLimitY;

    private boolean mLightEnabled = false;
    private boolean mColorsEnabled = false;
    private boolean mColorGradientEnabled = false;
    private boolean mTextureEnabled = false;
    private boolean mKineticEnabled = false;
    private boolean m3DEnabled = false;

    private boolean mIsPaused = false;

    private int shapeIndex = 0;
    private final String[] mList3DShapes = {GraphicsRenderer.PYRAMID, GraphicsRenderer.CUBE, GraphicsRenderer.SPHERE};
    private final String[] mList2DShapes = {GraphicsRenderer.TRIANGLE, GraphicsRenderer.SQUARE, GraphicsRenderer.CIRCLE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurface = findViewById(R.id.gl_surface);
        mLightSwitch = findViewById(R.id.light_switch);
        mColorsSwitch = findViewById(R.id.color_switch);
        mColorGradientSwitch = findViewById(R.id.color_gradient_switch);
        mTextureSwitch = findViewById(R.id.texture_switch);
        mKineticSwitch = findViewById(R.id.kinetic_switch);
        mDimensionsSwitch = findViewById(R.id.dimensions_switch);
        mSwapShapesButton = findViewById(R.id.shape_button);
        mGraphicsList = findViewById(R.id.graphics_list);

        float dpHeight, dpWidth;
        float density = getResources().getDisplayMetrics().density;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            final WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
            final Rect bounds = metrics.getBounds();
            dpHeight = bounds.height() / density;
            dpWidth  = bounds.width() / density;
        } else {
            final Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
            dpHeight = outMetrics.heightPixels / density;
            dpWidth = outMetrics.widthPixels / density;
        }

        Timber.d("Display width / height: %f x %f", dpWidth, dpHeight);

        mGraphicsList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mGraphicsList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int[] location = new int[2];
                mGraphicsList.getLocationOnScreen(location);
                mTouchLimitY = location[1];
            }
        });

        mLightSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGraphicsRenderer != null) {
                    mLightEnabled = mLightSwitch.isChecked();
                    mGraphicsRenderer.setLightState(mLightEnabled);
                }
            }
        });
        mLightEnabled = mLightSwitch.isChecked();

        mColorsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGraphicsRenderer != null) {
                    mColorsEnabled = mColorsSwitch.isChecked();
                    mGraphicsRenderer.setColorState(mColorsEnabled);
                    if (mColorsEnabled) {
                        mColorGradientSwitch.setVisibility(View.VISIBLE);
                    } else {
                        mColorGradientSwitch.setVisibility(View.INVISIBLE);
                    }
                }

            }
        });
        mColorsEnabled = mColorsSwitch.isChecked();
        if (mColorsEnabled) {
            mColorGradientSwitch.setVisibility(View.VISIBLE);
        } else {
            mColorGradientSwitch.setVisibility(View.INVISIBLE);
        }

        mColorGradientSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColorGradientEnabled = mColorGradientSwitch.isChecked();
                if (mGraphicsRenderer != null) {
                    mGraphicsRenderer.setColorGradientState(mColorGradientEnabled);
                }
            }
        });
        mColorGradientEnabled = mColorGradientSwitch.isChecked();

        mTextureSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGraphicsRenderer != null) {
                    mTextureEnabled = mTextureSwitch.isChecked();
                    mGraphicsRenderer.setTextureState(mTextureEnabled);
                }
            }
        });
        mTextureEnabled = mTextureSwitch.isChecked();

        mKineticSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGraphicsRenderer != null) {
                    mKineticEnabled = mKineticSwitch.isChecked();
                    mGraphicsRenderer.setKineticState(mKineticEnabled);
                }
            }
        });
        mKineticEnabled = mKineticSwitch.isChecked();

        mDimensionsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m3DEnabled = mDimensionsSwitch.isChecked();
                if (mGraphicsRenderer != null) {
                    updateShape();

                    if (mGraphicsRenderer.isKineticManaged()) {
                        mKineticSwitch.setVisibility(View.VISIBLE);
                    } else {
                        mKineticSwitch.setVisibility(View.INVISIBLE);
                    }

                    if (mGraphicsRenderer.isLightManaged()) {
                        mLightSwitch.setVisibility(View.VISIBLE);
                    } else {
                        mLightSwitch.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        m3DEnabled = mDimensionsSwitch.isChecked();

        mSwapShapesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shapeIndex++;
                updateShape();

                if (mGraphicsRenderer.isKineticManaged()) {
                    mKineticSwitch.setVisibility(View.VISIBLE);
                } else {
                    mKineticSwitch.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
            final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

            if (supportsEs2) {
                Timber.i("OpenGLES 2.0 supported");
                mSurface.setEGLContextClientVersion(2);
            } else {
                Toast.makeText(this,"OpenGLES 2.0 not supported, device not compatible",Toast.LENGTH_LONG).show();
                finish();
            }

            if (m3DEnabled) {
                mGraphicsRenderer = new GraphicsRenderer(this, mList3DShapes[shapeIndex]);
            } else {
                mGraphicsRenderer = new GraphicsRenderer(this, mList2DShapes[shapeIndex]);
            }

            mGraphicsRenderer.setColorState(mColorsEnabled);

            if (mGraphicsRenderer.isKineticManaged()) {
                mKineticSwitch.setVisibility(View.VISIBLE);
            } else {
                mKineticSwitch.setVisibility(View.INVISIBLE);
            }

            if (mGraphicsRenderer.isLightManaged()) {
                mLightSwitch.setVisibility(View.VISIBLE);
            } else {
                mLightSwitch.setVisibility(View.INVISIBLE);
            }

            mSurface.setZOrderOnTop(true);
            mSurface.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            mSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
            mSurface.setRenderer(mGraphicsRenderer);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    // used to swap between shapes
    public void updateShape() {
        mSurface.onPause();
        if (m3DEnabled) {
            // 3D shapes selected
            if (shapeIndex >= mList3DShapes.length) {
                shapeIndex = 0;
            }
            mGraphicsRenderer.selectShape(mList3DShapes[shapeIndex]);
            mSwapShapesButton.setText(mList3DShapes[shapeIndex]);
        } else {
            // 2D shapes selected
            if (shapeIndex >= mList2DShapes.length) {
                shapeIndex = 0;
            }
            mGraphicsRenderer.selectShape(mList2DShapes[shapeIndex]);
            mSwapShapesButton.setText(mList2DShapes[shapeIndex]);
        }
        mSurface.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSurface != null) {
            mSurface.onPause();
            mIsPaused = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((mSurface != null) && (mIsPaused)){
            mSurface.onResume();
            mIsPaused = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getY() > mTouchLimitY) {
            return true;
        }

        Point destination = new Point((int) event.getX(), (int) event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mGraphicsRenderer.pause();
                int[] location = new int[2];
                mSurface.getLocationOnScreen(location);
                mCenter = new Point((int)mSurface.getPivotX() + location[0], (int)mSurface.getPivotY() + location[1]);
                break;
            case MotionEvent.ACTION_UP:
                mGraphicsRenderer.resume();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!m3DEnabled) {
                    float angle = Utility.getAngle(mCenter, mOrigin, destination);
                    mGraphicsRenderer.setAngle(mGraphicsRenderer.getAngle() + angle);
                } else {
                    if ((mOrigin.x != 0) || (mOrigin.y != 0)) {
                        float[] delta = Utility.getDelta(mOrigin, destination);
                        mGraphicsRenderer.setDelta(delta);
                    }
                }
                mSurface.requestRender();
                break;
        }

        mOrigin = destination;

        return true;
    }
}
