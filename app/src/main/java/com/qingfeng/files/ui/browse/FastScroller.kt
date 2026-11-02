package com.qingfeng.files.ui.browse

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qingfeng.files.util.AppTheme

/**
 * 快速滚动条。
 *
 * 借鉴点：参照物有自定义 `AceVerticalViewScroller`
 * （自绘 View，带 getScrollProgressCalculator()），
 * 不是用 RecyclerView 自带的滚动条。
 *
 * 我们做一个够用的版本：右侧竖条 + 拖动跳转 + 拖动时显示位置。
 */
class FastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var recyclerView: RecyclerView? = null

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.primary
        alpha = 120
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppTheme.primary
        alpha = 220
    }

    private var dragging = false
    private var thumbY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = recyclerView ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                dragging = true
                thumbY = event.y.coerceIn(0f, height.toFloat())
                val ratio = thumbY / height
                val count = rv.adapter?.itemCount ?: 0
                if (count > 0) (rv.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset((ratio * count).toInt().coerceIn(0, count - 1), 0)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
                return true
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rv = recyclerView ?: return
        val count = rv.adapter?.itemCount ?: 0
        if (count <= 0) return

        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        val visible = lm.childCount.coerceAtLeast(1)

        val trackH = height.toFloat()
        val thumbH = (trackH * visible / count).coerceAtLeast(40f)
        val y = ((trackH - thumbH) * first.toFloat() / (count - visible).coerceAtLeast(1))
            .coerceIn(0f, trackH - thumbH)

        val paint = if (dragging) activePaint else barPaint
        canvas.drawRoundRect(
            width - 12f, y, width - 4f, y + thumbH, 4f, 4f, paint
        )
    }
}
