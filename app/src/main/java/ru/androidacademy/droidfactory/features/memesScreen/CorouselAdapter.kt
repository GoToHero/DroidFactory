import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import ru.androidacademy.droidfactory.MemsData
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.features.memesScreen.MemesScreenFragment
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

        return VH(MemesScreenFragment.OverlayableImageView(parent.context))
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

    fun bindMems(newMems: List<MemsData>) {
        mems.addAll(newMems)
        notifyDataSetChanged()
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

    class VH(val overlayableImageView: MemesScreenFragment.OverlayableImageView) :
        RecyclerView.ViewHolder(overlayableImageView)
}