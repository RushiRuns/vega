package com.vega.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.vega.R

class CircularCheckboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var checked = false
    var isSelectionMode = false
        set(value) {
            field = value
            invalidate()
        }

    private var fillProgress = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
        color = ContextCompat.getColor(context, R.color.vega_on_surface)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.vega_primary)
    }

    private val checkmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dpToPx(2f)
        color = ContextCompat.getColor(context, R.color.vega_background)
    }

    private val circleBounds = RectF()
    private val checkmarkPath = Path()
    private val dstPath = Path()
    private val pathMeasure = PathMeasure()

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    private val springAnimProperty = object : FloatPropertyCompat<CircularCheckboxView>("fillProgress") {
        override fun getValue(view: CircularCheckboxView): Float {
            return view.fillProgress
        }

        override fun setValue(view: CircularCheckboxView, value: Float) {
            view.fillProgress = value
        }
    }

    private val springAnimation = SpringAnimation(this, springAnimProperty).apply {
        spring = SpringForce().apply {
            stiffness = 400f
            dampingRatio = 0.8f
        }
    }

    init {
        setOnClickListener {
            toggle(animate = true)
        }
    }

    fun isChecked(): Boolean = checked

    fun setChecked(isChecked: Boolean, animate: Boolean = true) {
        if (checked == isChecked) return
        checked = isChecked

        val targetProgress = if (checked) 1f else 0f
        if (animate && isAttachedToWindow) {
            springAnimation.animateToFinalPosition(targetProgress)
        } else {
            springAnimation.cancel()
            fillProgress = targetProgress
        }
    }

    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        onCheckedChangeListener = listener
    }

    fun toggle(animate: Boolean = true) {
        val newState = !checked
        setChecked(newState, animate)
        onCheckedChangeListener?.invoke(newState)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeOffset = strokePaint.strokeWidth / 2f
        val padding = dpToPx(2f)
        val size = minOf(w, h).toFloat()
        
        val left = (w - size) / 2f + strokeOffset + padding
        val top = (h - size) / 2f + strokeOffset + padding
        val right = (w + size) / 2f - strokeOffset - padding
        val bottom = (h + size) / 2f - strokeOffset - padding
        
        circleBounds.set(left, top, right, bottom)

        // Build checkmark path
        checkmarkPath.reset()
        val cW = circleBounds.width()
        val cH = circleBounds.height()
        val cX = circleBounds.left
        val cY = circleBounds.top

        checkmarkPath.moveTo(cX + cW * 0.28f, cY + cH * 0.50f)
        checkmarkPath.lineTo(cX + cW * 0.44f, cY + cH * 0.66f)
        checkmarkPath.lineTo(cX + cW * 0.72f, cY + cH * 0.36f)

        pathMeasure.setPath(checkmarkPath, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = circleBounds.width() / 2f
        val cx = circleBounds.centerX()
        val cy = circleBounds.centerY()

        if (fillProgress > 0f || isSelectionMode) {
            val fillRadius = if (isSelectionMode && checked) radius else radius * fillProgress
            canvas.drawCircle(cx, cy, fillRadius, fillPaint)
        }

        if (fillProgress < 1f && !checked) {
            canvas.drawCircle(cx, cy, radius, strokePaint)
        }

        if (fillProgress > 0f) {
            dstPath.reset()
            val pathLength = pathMeasure.length
            pathMeasure.getSegment(0f, pathLength * fillProgress, dstPath, true)
            canvas.drawPath(dstPath, checkmarkPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
