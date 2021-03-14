import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.androidacademy.droidfactory.MemsData
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.databinding.ViewOverlayableImageBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class CarouselAdapter :
    RecyclerView.Adapter<CarouselAdapter.VH>() {

    var mems: MutableList<MemsData> = mutableListOf()
        private set
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

        if (mem.isLiked) {
            vh.overlayableImageView.likeIcon.setImageDrawable(ContextCompat.getDrawable(vh.itemView.context, R.drawable.ic_favorite_icon_active))
        } else {
            vh.overlayableImageView.likeIcon.setImageDrawable(ContextCompat.getDrawable(vh.itemView.context, R.drawable.ic_favorite_icon_empty))

        }

        vh.overlayableImageView.setOnClickListener {
            val rv = vh.overlayableImageView.parent as RecyclerView
            rv.smoothScrollToCenteredPosition(position)
        }
    }

    fun bindMems(newMems: List<MemsData>) {
        mems.addAll(newMems)
        notifyDataSetChanged()
    }

    fun updateMem(updatedMem: MemsData) {
        for (i in mems.indices) {
            if (updatedMem.id == mems[i].id) {
                mems.removeAt(i)
                mems.add(i, updatedMem)
                notifyItemChanged(i)
                return
            }
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

    class CarouselLayoutManager(
        context: Context,
        private val minScaleDistanceFactor: Float = 1.5f,
        private val scaleDownBy: Float = 0.5f,
    ) : LinearLayoutManager(context, HORIZONTAL, false) {

        private val prominentThreshold =
            context.resources.getDimensionPixelSize(R.dimen.spacing_18x)

        var currentItemId: Int? = 0

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

                if (child.isActivated) currentItemId = (child as OverlayableImageView).mem?.id
            }
        }

        override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
            // Since we're scaling down items, we need to pre-load more of them offscreen.
            // The value is sort of empirical: the more we scale down, the more extra space we need.
            return (width / (1 - scaleDownBy)).roundToInt()
        }
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

        var likeIcon: ImageView = binding.likeIcon

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
                            CenterCrop(),
                            RoundedCorners(resources.getDimensionPixelSize(R.dimen.spacing_2x))
                        )
                        .into(binding.imageView)

                }
            }



        init {
            layoutTransition = LayoutTransition() // android:animateLayoutChanges="true"
            isActivated = false
        }

        override fun setActivated(activated: Boolean) {
            val isChanging = activated != isActivated
            super.setActivated(activated)

//            if (isChanging) {
//                // Switch between VISIBLE and INVISIBLE
//                binding.sendButton.isInvisible = !activated
//            }
        }
    }
}