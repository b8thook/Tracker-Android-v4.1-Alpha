package co.neatfolk.triptracker

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * SwipeBar — swipe-to-confirm track.
 *
 * Layout:
 *   [✕]  ←─────────[●]─────────→  [✓]
 *
 * Drag thumb all the way right → onEndTrip()
 * Drag thumb all the way left  → onCancelTrip()
 * Release before threshold     → thumb snaps back to centre
 *
 * v4.0-alpha fix: threshold raised to 0.88 and cancel requires deliberate
 * full-left drag — accidental half-swipes always snap back to centre.
 */
class SwipeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onEndTrip:    (() -> Unit)? = null
    var onCancelTrip: (() -> Unit)? = null

    // Thumb position: 0.0 = full left, 0.5 = centre, 1.0 = full right
    private var thumbPos = 0.5f
    private var triggered = false

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A3A27")
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.FILL
    }
    private val cancelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626")
        textAlign = Paint.Align.LEFT
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#16A34A")
        textAlign = Paint.Align.RIGHT
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3D6B52")
        textAlign = Paint.Align.CENTER
        textSize = 14f
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trackRect = RectF()
    private val thumbRadius get() = height / 2f - 4f

    // v4.0-alpha: raised from 0.82 to 0.88 — requires more deliberate swipe
    // Cancel (left) requires equally deliberate swipe to 1 - 0.88 = 0.12
    private val TRIGGER_THRESHOLD = 0.88f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        trackRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cornerR = h / 2f

        canvas.drawRoundRect(trackRect, cornerR, cornerR, trackPaint)

        if (thumbPos != 0.5f) {
            val alpha = (abs(thumbPos - 0.5f) * 2f * 200).toInt().coerceIn(0, 200)
            if (thumbPos > 0.5f) {
                fillPaint.color = Color.argb(alpha, 22, 163, 74)
                val fillRect = RectF(0f, 0f, thumbX(), h)
                canvas.drawRoundRect(fillRect, cornerR, cornerR, fillPaint)
            } else {
                fillPaint.color = Color.argb(alpha, 220, 38, 38)
                val fillRect = RectF(thumbX(), 0f, w, h)
                canvas.drawRoundRect(fillRect, cornerR, cornerR, fillPaint)
            }
        }

        val textY = h / 2f - (cancelPaint.descent() + cancelPaint.ascent()) / 2f
        canvas.drawText("✕", 16f, textY, cancelPaint)
        canvas.drawText("✓", w - 16f, textY, endPaint)

        if (abs(thumbPos - 0.5f) < 0.15f) {
            canvas.drawText("← swipe →", w / 2f, textY, hintPaint)
        }

        canvas.drawCircle(thumbX(), h / 2f, thumbRadius, thumbPaint)
    }

    private fun thumbX(): Float {
        val usable = width - thumbRadius * 2 - 8f
        return thumbRadius + 4f + usable * thumbPos
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (triggered) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true

            MotionEvent.ACTION_MOVE -> {
                val usable = width - (thumbRadius * 2f) - 8f
                thumbPos = ((event.x - (thumbRadius + 4f)) / usable)
                    .coerceIn(0f, 1f)

                if (thumbPos >= TRIGGER_THRESHOLD) {
                    triggered = true
                    animateThumbTo(1f) { onEndTrip?.invoke(); reset() }
                } else if (thumbPos <= (1f - TRIGGER_THRESHOLD)) {
                    triggered = true
                    animateThumbTo(0f) { onCancelTrip?.invoke(); reset() }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!triggered) snapToCenter()
                return true
            }
        }
        return false
    }

    private fun snapToCenter() {
        animateThumbTo(0.5f, null)
    }

    private fun animateThumbTo(target: Float, onEnd: (() -> Unit)?) {
        val animator = ObjectAnimator.ofFloat(this, "thumbPosition", thumbPos, target)
        animator.duration = 200L
        animator.interpolator = DecelerateInterpolator()
        if (onEnd != null) {
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
        }
        animator.start()
    }

    fun setThumbPosition(pos: Float) {
        thumbPos = pos
        invalidate()
    }

    fun getThumbPosition() = thumbPos

    fun reset() {
        triggered = false
        thumbPos = 0.5f
        invalidate()
    }
}
