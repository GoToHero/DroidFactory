/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.androidacademy.droidfactory.domain

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import ru.androidacademy.droidfactory.views.GraphicOverlay
import java.util.Locale

/** Face Detector Demo.  */
class FaceDetectorProcessor(context: Context, val listener: FaceResultListener) :
  VisionProcessorBase<List<Face>>(context) {

  private val detector: FaceDetector

  init {
    val options = FaceDetectorOptions.Builder()
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    detector = FaceDetection.getClient(options)

  }

  override fun stop() {
    super.stop()
    detector.close()
  }

  override fun detectInImage(image: InputImage): Task<List<Face>> {
    return detector.process(image)
  }

  override fun onSuccess(faces: List<Face>, graphicOverlay: GraphicOverlay) {
    for (face in faces) {
//      graphicOverlay.add(FaceGraphic(graphicOverlay, face))
      resolveSmile(face.smilingProbability)
    }
  }

  private fun resolveSmile(smileValue: Float) {
    if (smileValue >= 0.60f) {
      listener.onReceiveFaceLike()
    }
  }

  override fun onFailure(e: Exception) {
    Log.e(TAG, "Face detection failed $e")
  }

  companion object {
    private const val TAG = "FaceDetectorProcessor"
    private const val MANUAL_TESTING_LOG = "LogTagForTest"
  }
}
