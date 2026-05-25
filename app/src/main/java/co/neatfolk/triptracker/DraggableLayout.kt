package co.neatfolk.triptracker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import kotlin.math.abs

/**
 * DraggableLayout v4
 *
 * Drag is ONLY triggered from the grip handle zone (top strip).
 * The rest of the pill (action button, swipe bar) handles its own touches.
 *
 * Grip zone = top GRIP_ZONE_HEIGHT dp of the view.
 * Any touch outside the grip zone passes directly to children.
 */
class DraggableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onDragDelta:   ((dx: Float, dy: Float) -> Unit)? = null
    var onDragToClose: (() -> Unit)? = null
    var inCloseZone:   Boolean = false

    private var draggingFromGrip = false
    private var lastRawX = 0f
    private var lastRawY = 0f

    companion object {
        // Height of grip handle zone in dp — touches here trigger drag
        private const val GRIP_ZONE_DP = 24f
    }

    private val gripZonePx: Float get() =
        GRIP_ZONE_DP * context.resources.displayMetrics.density

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Only intercept touches in the grip zone (top strip)
        if (ev.action == MotionEvent.ACTION_DOWN) {
            draggingFromGrip = ev.y <= gripZonePx
        }
        return draggingFromGrip
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!draggingFromGrip) return false

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = ev.rawX; lastRawY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - lastRawX
                val dy = ev.rawY - lastRawY
                onDragDelta?.invoke(dx, dy)
                lastRawX = ev.rawX; lastRawY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                if (inCloseZone) onDragToClose?.invoke()
                draggingFromGrip = false
            }
            MotionEvent.ACTION_CANCEL -> {
                draggingFromGrip = false
            }
        }
        return true
    }
}
