package ru.androidacademy.droidfactory.features.memesScreen

import CarouselAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import ru.androidacademy.droidfactory.MemsData
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.databinding.MemesScreenFragmentBinding
import ru.androidacademy.droidfactory.domain.CameraSource
import ru.androidacademy.droidfactory.domain.FaceDetectorProcessor
import ru.androidacademy.droidfactory.domain.FaceResultListener
import ru.androidacademy.droidfactory.views.*
import java.io.IOException

const val PERMISSIONS_REQUEST_CODE_CAMERA = 3332

class MemesScreenFragment : Fragment(R.layout.memes_screen_fragment), FaceResultListener {

    private var _binding: MemesScreenFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MemesScreenFragmentViewModel


    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null

    private lateinit var layoutManager: CarouselAdapter.CarouselLayoutManager
    private lateinit var adapter: CarouselAdapter
    private lateinit var snapHelper: SnapHelper
    private var currentItemId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = MemesScreenFragmentBinding.bind(view)

        val viewModelFactory = MemesScreenFragmentViewModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(MemesScreenFragmentViewModel::class.java)
        viewModel.loadingState.observe(this.viewLifecycleOwner, this::setLoading)
        initErrorHandler()

        preview = binding.scannerView
        graphicOverlay = binding.graphicOverlay
    }

    override fun onResume() {
        super.onResume()
        checkCameraPermission()
        startCameraSource()


        layoutManager = CarouselAdapter.CarouselLayoutManager(requireContext())
        adapter = CarouselAdapter()
        snapHelper = PagerSnapHelper()

        viewModel.memes.observe(viewLifecycleOwner, { memList ->
            adapter.bindMems(memList)
        })

        viewModel.loadingState.observe(viewLifecycleOwner, { isLoaded ->
            binding.progressBar.isVisible = isLoaded
        })

        with(binding.rvMemes) {
            setItemViewCacheSize(4)
            layoutManager = this@MemesScreenFragment.layoutManager
            adapter = this@MemesScreenFragment.adapter

            val spacing = resources.getDimensionPixelSize(R.dimen.spacing_4x)
            addItemDecoration(CarouselAdapter.LinearHorizontalSpacingDecoration(spacing))
            addItemDecoration(CarouselAdapter.BoundsOffsetDecoration())

            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                val layoutManagerRef = (layoutManager as CarouselAdapter.CarouselLayoutManager)

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val newCurrentItemId = layoutManagerRef.currentItemId
                    if (newCurrentItemId != currentItemId) {
                        currentItemId = newCurrentItemId
                        binding.memeDescriptions.text = getMemById(currentItemId)?.description
                    }

                    if (layoutManagerRef.findLastCompletelyVisibleItemPosition() == getMemsNumber() - 1) {
                        loadNextPage()
                    }
                }
            })

            snapHelper.attachToRecyclerView(this)
        }
    }

    private fun getMemById(id: Int?): MemsData? {
        for (mem in adapter.mems) {
            if (id == mem.id) {
                return mem
            }
        }
        return null
    }

    private fun getMemsNumber(): Int = adapter.itemCount

    private fun loadNextPage() {
        viewModel.loadNextPage()
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

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
    }

    private fun initErrorHandler() {
        viewModel.error.observe(viewLifecycleOwner, { errorMessage ->
            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT)
                .show()
        })
    }

    private fun createCameraSource() {
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), graphicOverlay)
        }
        cameraSource?.setMachineLearningFrameProcessor(
            FaceDetectorProcessor(
                requireContext(),
                this
            )
        )
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
        val permissionCheck =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (isNeedRequest) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.CAMERA),
                    PERMISSIONS_REQUEST_CODE_CAMERA
                )
            }
        } else {
            createCameraSource()
        }
    }

    override fun onReceiveFaceLike() {
        //TODO нужно кидать сюда айди мема
        viewModel.onLike(currentItemId ?: -1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
