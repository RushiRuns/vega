package com.vega.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.vega.R
import com.vega.ui.theme.ThemeManager

class SegmentedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tickCount = 14
    private var filledTicks = 0
    private var currentAnimator: ValueAnimator? = null

    private val tickWidth = resources.getDimension(R.dimen.segmented_tick_width)
    private val tickHeight = resources.getDimension(R.dimen.segmented_tick_height)
    private val tickGap = resources.getDimension(R.dimen.segmented_tick_gap)
    private val tickRadius = dpToPx(1.5f)

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ThemeManager.accentGreen(context)
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ThemeManager.surfaceElevated(context)
    }

    private var tickRects = Array(tickCount) { RectF() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateTickRects(h)
    }

    private fun recalculateTickRects(h: Int) {
        if (tickRects.size != tickCount) {
            tickRects = Array(tickCount) { RectF() }
        }
        val startY = (h - tickHeight) / 2f
        var startX = 0f
        for (i in 0 until tickCount) {
            tickRects[i].set(startX, startY, startX + tickWidth, startY + tickHeight)
            startX += tickWidth + tickGap
        }
    }

    fun setTickCount(count: Int) {
        if (tickCount != count && count > 0) {
            tickCount = count
            recalculateTickRects(height)
            invalidate()
        }
    }

    fun setProgress(completedCount: Int, totalCount: Int, animate: Boolean = true) {
        val targetTicks = if (totalCount > 0) {
            ((completedCount.toFloat() / totalCount.toFloat()) * tickCount).toInt().coerceIn(0, tickCount)
        } else {
            0
        }

        currentAnimator?.cancel()

        if (animate && isAttachedToWindow) {
            val startTicks = filledTicks
            currentAnimator = ValueAnimator.ofInt(startTicks, targetTicks).apply {
                duration = ((targetTicks - startTicks).coerceAtLeast(1) * 40L).coerceIn(150L, 600L)
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    filledTicks = anim.animatedValue as Int
                    invalidate()
                }
                start()
            }
        } else {
            filledTicks = targetTicks
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until tickCount) {
            val paint = if (i < filledTicks) filledPaint else emptyPaint
            canvas.drawRoundRect(tickRects[i], tickRadius, tickRadius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimator?.cancel()
        currentAnimator = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = ((tickWidth + tickGap) * tickCount - tickGap).toInt()
        val desiredHeight = tickHeight.toInt() + dpToPx(4f).toInt()
        
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
