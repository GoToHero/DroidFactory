package ru.androidacademy.droidfactory.domain

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.GuardedBy
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import ru.androidacademy.droidfactory.data.FrameMetadata
import ru.androidacademy.droidfactory.views.CameraImageGraphic
import ru.androidacademy.droidfactory.views.GraphicOverlay
import java.nio.ByteBuffer

/**
 * https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart
 *
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor {

    private var activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private var isShutdown = false

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    // -----------------Code for processing single still image----------------------------------------
    override fun processBitmap(bitmap: Bitmap?, graphicOverlay: GraphicOverlay) {
        requestDetectInImage(
                InputImage.fromBitmap(bitmap, 0),
                graphicOverlay, /* originalCameraImage= */
                null
        )
    }

    // -----------------Code for processing live preview frame from Camera1 API-----------------------
    @Synchronized
    override fun processByteBuffer(
            data: ByteBuffer?,
            frameMetadata: FrameMetadata?,
            graphicOverlay: GraphicOverlay
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay)
        }
    }

    @Synchronized
    private fun processLatestImage(graphicOverlay: GraphicOverlay) {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null && !isShutdown) {
            processImage(processingImage, processingMetaData, graphicOverlay)
        }
    }

    private fun processImage(
            data: ByteBuffer?,
            frameMetadata: FrameMetadata?,
            graphicOverlay: GraphicOverlay
    ) {
        // If live viewport is on (that is the underneath surface view takes care of the camera preview
        // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
//        val bitmap = BitmapUtils.getBitmap(data, frameMetadata)
        requestDetectInImage(
                InputImage.fromByteBuffer(
                        data,
                        frameMetadata?.width ?: 0,
                        frameMetadata?.height ?: 0,
                        frameMetadata?.rotation ?: 0,
                        InputImage.IMAGE_FORMAT_NV21
                ),
                graphicOverlay,
                null
        ).addOnSuccessListener() { processLatestImage(graphicOverlay) }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
            image: InputImage,
            graphicOverlay: GraphicOverlay,
            originalCameraImage: Bitmap?
    ): Task<T> {
        return detectInImage(image)
                .addOnSuccessListener() { results: T ->
                    graphicOverlay.clear()
                    if (originalCameraImage != null) {
                        graphicOverlay.add(
                                CameraImageGraphic(
                                        graphicOverlay,
                                        originalCameraImage
                                )
                        )
                    }
                    this@VisionProcessorBase.onSuccess(results, graphicOverlay)
                    graphicOverlay.postInvalidate()
                }
                .addOnFailureListener() { e: Exception ->
                    graphicOverlay.clear()
                    graphicOverlay.postInvalidate()
                    Toast.makeText(
                            graphicOverlay.context,
                            "Failed to process.\nError: " +
                                    e.localizedMessage +
                                    "\nCause: " +
                                    e.cause,
                            Toast.LENGTH_LONG
                    )
                            .show()
                    e.printStackTrace()
                    this@VisionProcessorBase.onFailure(e)
                }
    }

    override fun stop() {
        isShutdown = true
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)
}
