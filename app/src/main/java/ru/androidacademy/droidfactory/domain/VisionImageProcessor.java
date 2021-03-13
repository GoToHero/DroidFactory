package ru.androidacademy.droidfactory.domain;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

import ru.androidacademy.droidfactory.data.FrameMetadata;
import ru.androidacademy.droidfactory.views.GraphicOverlay;

/**
 * https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart
 * <p>
 * An interface to process the images with different vision detectors and custom image models.
 */
public interface VisionImageProcessor {

  /**
   * Processes a bitmap image.
   */
  void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

  /**
   * Processes ByteBuffer image data, e.g. used for Camera1 live preview case.
   */
  void processByteBuffer(
          ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
          throws Exception;

  /**
   * Stops the underlying machine learning model and release resources.
   */
  void stop();
}
