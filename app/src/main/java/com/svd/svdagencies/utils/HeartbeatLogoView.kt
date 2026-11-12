package com.svd.svdagencies.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.content.res.AppCompatResources
import com.svd.svdagencies.R
import kotlin.math.sin

/** Shared loading artwork; no animation runs while hidden or detached. */
class HeartbeatLogoView(context: Context) : View(context) {
    private val logo = AppCompatResources.getDrawable(context, R.drawable.logo)!!.mutate()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var fraction = 0f
    private var pulse: ValueAnimator? = null
    val isAnimating: Boolean get() = pulse?.isRunning == true
    private val density get() = resources.displayMetrics.density
    init { contentDescription = "Loading"; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }
    fun startPulse() {
        if (!isAttachedToWindow || !isShown || isAnimating) return
        if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) { fraction = 0.99f; invalidate(); return }
        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { fraction = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    fun stopPulse() { pulse?.cancel(); pulse = null; fraction = 0f; invalidate() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); startPulse() }
    override fun onDetachedFromWindow() { stopPulse(); super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(visible: Boolean) { super.onVisibilityAggregated(visible); if (visible) startPulse() else stopPulse() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = density
        paint.color = Color.WHITE
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 2f, paint)
        val cx = width / 2f
        val cy = 54*d
        val beat = when {
            fraction < .25f -> sin(fraction / .25f * Math.PI).toFloat() * .10f
            fraction < .50f -> sin((fraction - .25f) / .25f * Math.PI).toFloat() * .06f
            else -> 0f
        }
        canvas.save()
        canvas.scale(1f + beat, 1f + beat, cx, cy)
        val size = 76*d
        logo.setBounds((cx-size/2).toInt(), (cy-size/2).toInt(), (cx+size/2).toInt(), (cy+size/2).toInt())
        logo.draw(canvas)
        canvas.restore()
        paint.color = Color.rgb(55, 40, 40)
        paint.textSize = 13*resources.displayMetrics.scaledDensity
        paint.textAlign = Paint.Align.CENTER
        val dots = (fraction * 4).toInt().coerceIn(0, 3)
        canvas.drawText("Loading" + ".".repeat(dots), cx, 117*d, paint)
    }
}

/** Compatibility bridge for existing ProgressBar bindings; presents the shared overlay. */
class LogoProgressBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ProgressBar(context, attrs) {
    init { indeterminateDrawable = null; progressDrawable = null; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(canvas: Canvas) { /* Artwork belongs to the centered shared overlay. */ }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) { setMeasuredDimension(0, 0) }
    override fun onVisibilityAggregated(visible: Boolean) {
        super.onVisibilityAggregated(visible)
        if (visible && isAttachedToWindow) LoadingOverlayManager.show(this) else LoadingOverlayManager.hide(this)
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (isShown) LoadingOverlayManager.show(this) }
    override fun onDetachedFromWindow() { LoadingOverlayManager.hide(this); super.onDetachedFromWindow() }
}
