package ru.androidacademy.droidfactory.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import ru.androidacademy.droidfactory.domain.CameraSource
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart
 *
 * Вью для отображения картинки с камеры
 */
class CameraSourcePreview(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val surfaceView: SurfaceView
    private var startRequested = false
    private var surfaceAvailable = false
    private var cameraSource: CameraSource? = null
    private var overlay: GraphicOverlay? = null

    private fun start(cameraSource: CameraSource) {
        this.cameraSource = cameraSource
        if (this.cameraSource != null) {
            startRequested = true
            startIfReady()
        }
    }

    fun start(cameraSource: CameraSource, overlay: GraphicOverlay?) {
        this.overlay = overlay
        start(cameraSource)
    }

    fun stop() {
        if (cameraSource != null) {
            cameraSource?.stop()
        }
    }

    fun release() {
        if (cameraSource != null) {
            cameraSource?.release()
            cameraSource = null
        }
        surfaceView.holder.surface.release()
    }

    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraSource?.start(surfaceView.holder)
            requestLayout()
            if (overlay != null) {
                val size = cameraSource?.previewSize
                val min = min(size?.width ?: 0, size?.height ?: 0)
                val max = max(size?.width ?: 0, size?.height ?: 0)
                val isImageFlipped = cameraSource?.cameraFacing == CameraSource.CAMERA_FACING_FRONT
                if (isPortraitMode()) {
                    // Swap width and height sizes when in portrait, since it will be rotated by 90 degrees.
                    // The camera preview and the image being processed have the same size.
                    overlay?.setImageSourceInfo(min, max, isImageFlipped)
                } else {
                    overlay?.setImageSourceInfo(max, min, isImageFlipped)
                }
                overlay?.clear()
            }
            startRequested = false
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var width = 320
        var height = 240
        if (cameraSource != null) {
            val size = cameraSource?.previewSize
            if (size != null) {
                width = size.width
                height = size.height
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            val tmp = width
            width = height
            height = tmp
        }
        val previewAspectRatio = width.toFloat() / height
        val layoutWidth = right - left
        val layoutHeight = bottom - top
        val layoutAspectRatio = layoutWidth.toFloat() / layoutHeight
        if (previewAspectRatio > layoutAspectRatio) {
            // The preview input is wider than the layout area. Fit the layout height and crop
            // the preview input horizontally while keep the center.
            val horizontalOffset = (previewAspectRatio * layoutHeight - layoutWidth).toInt() / 2
            surfaceView.layout(-horizontalOffset, 0, layoutWidth + horizontalOffset, layoutHeight)
        } else {
            // The preview input is taller than the layout area. Fit the layout width and crop the preview
            // input vertically while keep the center.
            val verticalOffset = (layoutWidth / previewAspectRatio - layoutHeight).toInt() / 2
            surfaceView.layout(0, -verticalOffset, layoutWidth, layoutHeight + verticalOffset)
        }
    }

    private fun isPortraitMode(): Boolean {
        val orientation = context.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true
        }
        Log.d(TAG, "isPortraitMode returning false by default")
        return false
    }

    companion object {
        private const val TAG = "Preview"
    }

    init {
        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }
}