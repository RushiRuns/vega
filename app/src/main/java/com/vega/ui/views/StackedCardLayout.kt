package com.vega.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.vega.R

class StackedCardLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Pre-allocated objects to prevent allocations during dispatchDraw
    private val phantomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vega_surface)
        alpha = (255 * 0.6f).toInt()
    }
    private val phantomRect = RectF()
    private val cornerRadius = resources.getDimension(R.dimen.card_corner_radius)
    private val peekOffset = resources.getDimension(R.dimen.nba_peek_offset)

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // RectF updated only when size changes, not per frame
        phantomRect.set(peekOffset, 0f, w.toFloat() - peekOffset, h.toFloat() - peekOffset)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (childCount > 0 && visibility == VISIBLE) {
            canvas.drawRoundRect(phantomRect, cornerRadius, cornerRadius, phantomPaint)
        }
        super.dispatchDraw(canvas)
    }
}
