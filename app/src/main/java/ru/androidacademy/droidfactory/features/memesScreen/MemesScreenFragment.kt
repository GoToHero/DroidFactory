package ru.androidacademy.droidfactory.features.memesScreen

import android.Manifest
import android.animation.LayoutTransition
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.runBlocking
import ru.androidacademy.droidfactory.MemsData
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.Repository
import ru.androidacademy.droidfactory.databinding.MemesScreenFragmentBinding
import ru.androidacademy.droidfactory.databinding.ViewOverlayableImageBinding
import ru.androidacademy.droidfactory.domain.CameraSource
import ru.androidacademy.droidfactory.domain.FaceDetectorProcessor
import ru.androidacademy.droidfactory.domain.FaceResultListener
import ru.androidacademy.droidfactory.views.CameraSourcePreview
import ru.androidacademy.droidfactory.views.GraphicOverlay
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

const val PERMISSIONS_REQUEST_CODE_CAMERA = 3332

class MemesScreenFragment : Fragment(R.layout.memes_screen_fragment), FaceResultListener {

    private var _binding: MemesScreenFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MemesScreenFragmentViewModel

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null

    //TODO
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: CarouselAdapter
    private lateinit var snapHelper: SnapHelper
    private lateinit var mems: List<MemsData>

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

        //TODO
//        viewModel.viewModelScope.launch {
//            mems = when (Repository.initialize()) {
//                is MemsResources.Success -> Repository.initialize().data!!
//                is MemsResources.Error -> Repository.initialize().data!!
//                is MemsResources.Loading -> TODO()
//            }
//
//            mems = Repository.initialize().data
//        }

        runBlocking { mems = Repository.initialize().data!! }
        Log.d("WTF", mems.joinToString("\n"))


        layoutManager = CarouselLayoutManager(requireContext())
        adapter = CarouselAdapter(mems)
        snapHelper = PagerSnapHelper()

        with(binding.rvMemes) {
            setItemViewCacheSize(4)
            layoutManager = this@MemesScreenFragment.layoutManager
            adapter = this@MemesScreenFragment.adapter

            val spacing = resources.getDimensionPixelSize(R.dimen.spacing_4x)
            addItemDecoration(LinearHorizontalSpacingDecoration(spacing))
            addItemDecoration(BoundsOffsetDecoration())

            snapHelper.attachToRecyclerView(this)
        }
    }

    class CarouselLayoutManager(
        context: Context,
        private val minScaleDistanceFactor: Float = 1.5f,
        private val scaleDownBy: Float = 0.5f,
    ) : LinearLayoutManager(context, HORIZONTAL, false) {

        private val prominentThreshold =
            context.resources.getDimensionPixelSize(R.dimen.spacing_18x)

        override fun onLayoutCompleted(state: RecyclerView.State?) =
            super.onLayoutCompleted(state).also { scaleChildren() }

        override fun scrollHorizontallyBy(
            dx: Int,
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State
        ) = super.scrollHorizontallyBy(dx, recycler, state).also {
            if (orientation == HORIZONTAL) scaleChildren()
        }

        private fun scaleChildren() {
            val containerCenter = width / 2f

            // Any view further than this threshold will be fully scaled down
            val scaleDistanceThreshold = minScaleDistanceFactor * containerCenter

            var translationXForward = 0f

            for (i in 0 until childCount) {
                val child = getChildAt(i)!!

                val childCenter = (child.left + child.right) / 2f
                val distanceToCenter = abs(childCenter - containerCenter)

                child.isActivated = distanceToCenter < prominentThreshold

                val scaleDownAmount = (distanceToCenter / scaleDistanceThreshold).coerceAtMost(1f)
                val scale = 1f - scaleDownBy * scaleDownAmount

                child.scaleX = scale
                child.scaleY = scale

                val translationDirection = if (childCenter > containerCenter) -1 else 1
                val translationXFromScale = translationDirection * child.width * (1 - scale) / 2f
                child.translationX = translationXFromScale + translationXForward

                translationXForward = 0f

                if (translationXFromScale > 0 && i >= 1) {
                    // Edit previous child
                    getChildAt(i - 1)!!.translationX += 2 * translationXFromScale

                } else if (translationXFromScale < 0) {
                    // Pass on to next child
                    translationXForward = 2 * translationXFromScale
                }
            }
        }

        override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
            // Since we're scaling down items, we need to pre-load more of them offscreen.
            // The value is sort of empirical: the more we scale down, the more extra space we need.
            return (width / (1 - scaleDownBy)).roundToInt()
        }
    }


    class CarouselAdapter(private val mems: List<MemsData>) :
        RecyclerView.Adapter<CarouselAdapter.VH>() {

        private var hasInitParentDimensions = false
        private var maxImageWidth: Int = 0
        private var maxImageHeight: Int = 0
        private var maxImageAspectRatio: Float = 1f

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            // At this point [parent] has been measured and has valid width & height
            if (!hasInitParentDimensions) {
                //TODO поиграться с размерами
                maxImageWidth =
                    parent.width - 2 * parent.resources.getDimensionPixelSize(R.dimen.spacing_18x)
                maxImageHeight = parent.height
                maxImageAspectRatio = maxImageWidth.toFloat() / maxImageHeight.toFloat()
                hasInitParentDimensions = true
            }

            return VH(OverlayableImageView(parent.context))
        }

        override fun onBindViewHolder(vh: VH, position: Int) {
            val mem = mems[position]

            // Change aspect ratio
            val imageAspectRatio = mem.aspectRatio
            val targetImageWidth: Int = if (imageAspectRatio < maxImageAspectRatio) {
                // Tall image: height = max
                (maxImageHeight * imageAspectRatio).roundToInt()
            } else {
                // Wide image: width = max
                maxImageWidth
            }
            vh.overlayableImageView.layoutParams = RecyclerView.LayoutParams(
                targetImageWidth,
                RecyclerView.LayoutParams.MATCH_PARENT
            )

            // Load image
            vh.overlayableImageView.mem = mem

            vh.overlayableImageView.setOnClickListener {
                val rv = vh.overlayableImageView.parent as RecyclerView
                rv.smoothScrollToCenteredPosition(position)
            }
        }

        private fun RecyclerView.smoothScrollToCenteredPosition(position: Int) {
            val smoothScroller = object : LinearSmoothScroller(context) {
                override fun calculateDxToMakeVisible(view: View?, snapPreference: Int): Int {
                    val dxToStart = super.calculateDxToMakeVisible(view, SNAP_TO_START)
                    val dxToEnd = super.calculateDxToMakeVisible(view, SNAP_TO_END)

                    return (dxToStart + dxToEnd) / 2
                }
            }

            smoothScroller.targetPosition = position
            layoutManager?.startSmoothScroll(smoothScroller)
        }

        override fun getItemCount(): Int = mems.size

        class VH(val overlayableImageView: OverlayableImageView) :
            RecyclerView.ViewHolder(overlayableImageView)
    }

    class LinearHorizontalSpacingDecoration(@Px private val innerSpacing: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)

            val itemPosition = parent.getChildAdapterPosition(view)

            outRect.left = if (itemPosition == 0) 0 else innerSpacing / 2
            outRect.right = if (itemPosition == state.itemCount - 1) 0 else innerSpacing / 2
        }
    }

    class BoundsOffsetDecoration : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)

            val itemPosition = parent.getChildAdapterPosition(view)

            // It is crucial to refer to layoutParams.width (view.width is 0 at this time)!
            val itemWidth = view.layoutParams.width
            val offset = (parent.width - itemWidth) / 2

            if (itemPosition == 0) {
                outRect.left = offset
            } else if (itemPosition == state.itemCount - 1) {
                outRect.right = offset
            }
        }
    }

    class OverlayableImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ConstraintLayout(context, attrs, defStyleAttr) {
        private val binding =
            ViewOverlayableImageBinding.inflate(LayoutInflater.from(context), this, true)

        var mem: MemsData? = null
            set(value) {
                field = value
                value?.let {
                    Log.d("WTF", it.gifURL)

                    val imgUri = it.gifURL.toUri().buildUpon().scheme("https").build()
                    Glide.with(binding.imageView)
                        .asGif()
                        .load(imgUri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .transform(
                            FitCenter(),
                            RoundedCorners(resources.getDimensionPixelSize(R.dimen.spacing_2x))
                        )
                        .into(binding.imageView)

                }
            }


        init {
            layoutTransition = LayoutTransition() // android:animateLayoutChanges="true"
            isActivated = false
        }
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
        viewModel.onLike()
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
