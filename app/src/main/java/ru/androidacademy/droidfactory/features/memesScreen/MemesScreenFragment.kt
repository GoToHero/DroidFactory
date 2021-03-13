package ru.androidacademy.droidfactory.features.memesScreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.databinding.MemesScreenFragmentBinding
import ru.androidacademy.droidfactory.domain.CameraSource
import ru.androidacademy.droidfactory.domain.FaceDetectorProcessor
import ru.androidacademy.droidfactory.domain.FaceResultListener
import ru.androidacademy.droidfactory.views.CameraSourcePreview
import ru.androidacademy.droidfactory.views.GraphicOverlay
import java.io.IOException

const val PERMISSIONS_REQUEST_CODE_CAMERA = 3332

class MemesScreenFragment : Fragment(R.layout.memes_screen_fragment), FaceResultListener {

    private var _binding: MemesScreenFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MemesScreenFragmentViewModel

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = MemesScreenFragmentBinding.bind(view)

        val viewModelFactory = MemesScreenFragmentViewModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(MemesScreenFragmentViewModel::class.java)

        preview = binding.scannerView
        graphicOverlay = binding.graphicOverlay
    }

    override fun onResume() {
        super.onResume()
        checkCameraPermission()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    override fun onDestroyView() {
        _binding = null
        if (cameraSource != null) {
            cameraSource?.release()
        }
        super.onDestroyView()
    }

    private fun createCameraSource() {
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), graphicOverlay)
        }
        cameraSource?.setMachineLearningFrameProcessor(FaceDetectorProcessor(requireContext(), this))
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

    private fun checkCameraPermission(isNeedRequest: Boolean = true) {
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (isNeedRequest) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA),
                    PERMISSIONS_REQUEST_CODE_CAMERA)
            }
        } else {
            createCameraSource()
        }
    }

    override fun onReceiveFaceLike() {
        viewModel.onLike()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    createCameraSource()
                } else {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                }
            }
        }
    }
}