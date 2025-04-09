package com.enrique_john_wayne_m.cupfe_scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        color = 0x99000000.toInt() // semi-transparent black
    }

    private val boxRect = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Centered scan area
        val boxSize = 860
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2
        val right = left + boxSize
        val bottom = top + boxSize
        boxRect.set(left, top, right, bottom)

        // Top
        canvas.drawRect(0f, 0f, width.toFloat(), top.toFloat(), overlayPaint)
        // Bottom
        canvas.drawRect(0f, bottom.toFloat(), width.toFloat(), height.toFloat(), overlayPaint)
        // Left
        canvas.drawRect(0f, top.toFloat(), left.toFloat(), bottom.toFloat(), overlayPaint)
        // Right
        canvas.drawRect(right.toFloat(), top.toFloat(), width.toFloat(), bottom.toFloat(), overlayPaint)
    }
}