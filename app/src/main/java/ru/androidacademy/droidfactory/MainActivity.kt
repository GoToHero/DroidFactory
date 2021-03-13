package ru.androidacademy.droidfactory

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.androidacademy.droidfactory.domain.CameraSource
import ru.androidacademy.droidfactory.domain.FaceDetectorProcessor
import ru.androidacademy.droidfactory.domain.FaceResultListener
import ru.androidacademy.droidfactory.views.CameraSourcePreview
import ru.androidacademy.droidfactory.views.GraphicOverlay
import java.io.IOException

class MainActivity : AppCompatActivity(), FaceResultListener {

    //TODO надо будет перенести в мейн фрагмент
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null

    private fun createCameraSource() {
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
        }
        cameraSource?.setMachineLearningFrameProcessor(FaceDetectorProcessor(this, this))
    }

    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                preview?.start(cameraSource!!, graphicOverlay)
            } catch (e: IOException) {
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onReceiveBarcode() {
        TODO("Not yet implemented")
    }
}