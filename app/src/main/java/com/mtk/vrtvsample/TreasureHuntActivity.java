/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mtk.vrtvsample;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.mtk.vrtvsample.video.GLMediaPlayer;
import com.mtk.vrtvsample.video.renderers.DefaultVideoRenderer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Google VR sample application.
 * </p><p>
 * The TreasureHunt scene consists of a planar ground grid and a floating
 * "treasure" cube. When the user looks at the cube, the cube will turn gold.
 * While gold, the user can activate the Carboard trigger, which will in turn
 * randomly reposition the cube.
 */
public class TreasureHuntActivity extends GvrActivity implements GvrView.StereoRenderer {

  private static final String TAG = "TreasureHuntActivity";

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100.0f;

  private static final float CAMERA_Z = 0.01f;

  private float[] camera;
  private float[] view;
  private float[] headView;
  private float[] modelViewProjection;
  private float[] modelView;
  private float[] modelVideo;

  private Vibrator vibrator;

  private GLMediaPlayer glMediaPlayer = null;
  //    private DefaultVideoRenderer videoRenderer;
  private DefaultVideoRenderer videoRenderer;



  /**
   * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
   *
   * @param label Label to report in case of error.
   */
  private static void checkGLError(String label) {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      Log.e(TAG, label + ": glError " + error);
      throw new RuntimeException(label + ": glError " + error);
    }
  }

  /**
   * Sets the view to our GvrView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeGvrView();

    camera = new float[16];
    view = new float[16];
    modelViewProjection = new float[16];
    modelView = new float[16];
    modelVideo = new float[16];
    headView = new float[16];
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    //Video
    videoRenderer  = new DefaultVideoRenderer(this);
    glMediaPlayer = new GLMediaPlayer(this, videoRenderer, true);
    try{
           glMediaPlayer.setDataSource("http://patpet.liondev.tw/a.mp4");

    }catch (Exception e){
      e.printStackTrace();
    }
  }

  public void initializeGvrView() {
    setContentView(R.layout.common_ui);

    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

    gvrView.setRenderer(this);
    gvrView.setTransitionViewEnabled(true);
    gvrView.setOnCardboardBackButtonListener(
            new Runnable() {
              @Override
              public void run() {
                onBackPressed();
              }
            });
    setGvrView(gvrView);
  }

  @Override
  public void onPause() {
    super.onPause();
    glMediaPlayer.pause();
  }

  @Override
  public void onResume() {
    super.onResume();
    glMediaPlayer.resume();
  }

  @Override
  public void onRendererShutdown() {
    Log.i(TAG, "onRendererShutdown");
  }

  @Override
  public void onSurfaceChanged(int width, int height) {
    Log.i(TAG, "onSurfaceChanged");
      videoRenderer.onSurfaceChanged(width, height);
  }

  /**
   * Creates the buffers we use to store information about the 3D world.
   *
   * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
   * Hence we use ByteBuffers.
   *
   * @param config The EGL configuration used when creating the surface.
   */
  @Override
  public void onSurfaceCreated(EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.


    Matrix.setIdentityM(modelVideo, 0);
    //Matrix.setRotateEulerM(modelVideo, 0, 0, 0, 90);
    Matrix.translateM(modelVideo, 0, 0, 0, 0);

    videoRenderer.onSurfaceCreated(config);

    checkGLError("onSurfaceCreated");
  }

  /**
   * Prepares OpenGL ES before we draw a frame.
   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    // Build the camera matrix and apply it to the ModelView.
    Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

    headTransform.getHeadView(headView, 0);

    checkGLError("onReadyToDraw");
  }

  /**
   * Draws a frame for an eye.
   *
   * @param eye The eye to render. Includes all required transformations.
   */
  @Override
  public void onDrawEye(Eye eye) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    checkGLError("colorParam");

    // Apply the eye transformation to the camera.
    Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);
    float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

    Matrix.multiplyMM(modelView, 0, view, 0, modelVideo, 0);
    Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    videoRenderer.onDrawFrame(modelViewProjection);
  }

  @Override
  public void onFinishFrame(Viewport viewport) {}

  /**
   * Called when the Cardboard trigger is pulled.
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");

    // Always give user feedback.
    vibrator.vibrate(50);
  }
}
