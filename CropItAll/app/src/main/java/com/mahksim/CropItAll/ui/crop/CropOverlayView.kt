package com.mahksim.CropItAll.ui.crop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnCropRectChangedListener {
        fun onCropRectChanged(viewRect: RectF, bitmapRect: Rect)
    }

    private var listener: OnCropRectChangedListener? = null
    fun setOnCropRectChangedListener(listener: OnCropRectChangedListener) {
        this.listener = listener
    }

    private val cropRect = RectF()
    private var bitmapWidth = 0
    private var bitmapHeight = 0

    // === Кисти ===
    private val dimPaint = Paint().apply { color = Color.parseColor("#80000000"); style = Paint.Style.FILL }
    private val borderOuterPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 5f * resources.displayMetrics.density; isAntiAlias = true }
    private val borderInnerPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f * resources.displayMetrics.density; isAntiAlias = true }
    private val handleOuterPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val handleInnerPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }

    private var handleRadius: Float = 24f
    private var touchZone: Float = 48f
    private var minSize: Float = 70f * resources.displayMetrics.density

    private enum class DragMode { NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT }
    private var currentMode = DragMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // При изменении размера View (например, поворот) мы не сбрасываем рамку,
        // а просто пересчитываем радиус кружков
        recalculateHandleRadius()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropRect.isEmpty) return // Если рамка еще не задана, ничего не рисуем

        // 1. Затемнение
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)

        // 2. Рамка
        canvas.drawRect(cropRect, borderOuterPaint)
        canvas.drawRect(cropRect, borderInnerPaint)

        // 3. Круги
        canvas.save()
        val clipPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRect(cropRect, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val drawHandle = { cx: Float, cy: Float ->
            canvas.drawCircle(cx, cy, handleRadius + 2f, handleOuterPaint)
            canvas.drawCircle(cx, cy, handleRadius, handleInnerPaint)
        }
        drawHandle(cropRect.left, cropRect.top); drawHandle(cropRect.right, cropRect.top)
        drawHandle(cropRect.left, cropRect.bottom); drawHandle(cropRect.right, cropRect.bottom)
        val midX = cropRect.centerX(); val midY = cropRect.centerY()
        drawHandle(midX, cropRect.top); drawHandle(midX, cropRect.bottom)
        drawHandle(cropRect.left, midY); drawHandle(cropRect.right, midY)
        canvas.restore()
    }

    private fun recalculateHandleRadius() {
        val density = resources.displayMetrics.density
        val minRadius = 9f * density
        val maxRadius = 15f * density
        val tenPercent = minOf(cropRect.width(), cropRect.height()) * 0.1f
        handleRadius = tenPercent.coerceIn(minRadius, maxRadius)
        touchZone = handleRadius * 2f
    }

    fun setBitmapSize(w: Int, h: Int) {
        this.bitmapWidth = w
        this.bitmapHeight = h
    }

    /**
     * Вычисляет рамку по умолчанию (по границам картинки с отступом).
     * Вызывается из Activity, если нет сохраненной рамки.
     */
    fun calculateDefaultRect() {
        if (bitmapWidth == 0 || bitmapHeight == 0 || width == 0 || height == 0) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // Вычисляем, как картинка вписалась в экран (fitCenter)
        val scale = min(viewW / bitmapWidth, viewH / bitmapHeight)
        val displayedW = bitmapWidth * scale
        val displayedH = bitmapHeight * scale

        val left = (viewW - displayedW) / 2f
        val top = (viewH - displayedH) / 2f
        val right = left + displayedW
        val bottom = top + displayedH

        // Отступ внутрь, чтобы кружки не обрезались краями экрана (24dp)
        val inset = 15f * resources.displayMetrics.density

        // Проверяем, чтобы рамка не стала меньше минимального размера
        if ((right - left) > inset * 2 + minSize && (bottom - top) > inset * 2 + minSize) {
            cropRect.set(left + inset, top + inset, right - inset, bottom - inset)
        } else {
            // Если картинка очень маленькая, берем просто её границы
            cropRect.set(left, top, right, bottom)
        }

        recalculateHandleRadius()
        notifyRectChanged()
        invalidate()
    }

    fun setInitialRect(left: Float, top: Float, right: Float, bottom: Float) {
        cropRect.set(left, top, right, bottom)
        recalculateHandleRadius()
        notifyRectChanged()
        invalidate()
    }

    private fun notifyRectChanged() {
        if (bitmapWidth > 0 && bitmapHeight > 0 && !cropRect.isEmpty) {
            listener?.onCropRectChanged(RectF(cropRect), convertToBitmapRect())
        }
    }

    private fun convertToBitmapRect(): Rect {
        if (bitmapWidth == 0 || bitmapHeight == 0) return Rect()
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val scale = min(viewW / bitmapWidth, viewH / bitmapHeight)
        val displayedWidth = bitmapWidth * scale
        val displayedHeight = bitmapHeight * scale
        val offsetX = (viewW - displayedWidth) / 2f
        val offsetY = (viewH - displayedHeight) / 2f

        val bitmapLeft = ((cropRect.left - offsetX) / scale).toInt()
        val bitmapTop = ((cropRect.top - offsetY) / scale).toInt()
        val bitmapRight = ((cropRect.right - offsetX) / scale).toInt()
        val bitmapBottom = ((cropRect.bottom - offsetY) / scale).toInt()

        val clampedLeft = bitmapLeft.coerceIn(0, bitmapWidth)
        val clampedTop = bitmapTop.coerceIn(0, bitmapHeight)
        val clampedRight = bitmapRight.coerceIn(clampedLeft, bitmapWidth)
        val clampedBottom = bitmapBottom.coerceIn(clampedTop, bitmapHeight)

        return Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentMode = detectMode(x, y)
                lastTouchX = x; lastTouchY = y
                return currentMode != DragMode.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentMode == DragMode.NONE) return false
                updateCropRect(x - lastTouchX, y - lastTouchY)
                lastTouchX = x; lastTouchY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentMode = DragMode.NONE
                notifyRectChanged()
                return true
            }
        }
        return false
    }

    private fun detectMode(x: Float, y: Float): DragMode {
        if (isNear(x, y, cropRect.left, cropRect.top)) return DragMode.TOP_LEFT
        if (isNear(x, y, cropRect.right, cropRect.top)) return DragMode.TOP_RIGHT
        if (isNear(x, y, cropRect.left, cropRect.bottom)) return DragMode.BOTTOM_LEFT
        if (isNear(x, y, cropRect.right, cropRect.bottom)) return DragMode.BOTTOM_RIGHT
        val midX = cropRect.centerX(); val midY = cropRect.centerY()
        if (isNear(x, y, midX, cropRect.top)) return DragMode.TOP
        if (isNear(x, y, midX, cropRect.bottom)) return DragMode.BOTTOM
        if (isNear(x, y, cropRect.left, midY)) return DragMode.LEFT
        if (isNear(x, y, cropRect.right, midY)) return DragMode.RIGHT
        if (cropRect.contains(x, y)) return DragMode.CENTER
        return DragMode.NONE
    }

    private fun isNear(x: Float, y: Float, targetX: Float, targetY: Float): Boolean {
        return abs(x - targetX) < touchZone && abs(y - targetY) < touchZone
    }

    private fun updateCropRect(dx: Float, dy: Float) {
        val newRect = RectF(cropRect)
        when (currentMode) {
            DragMode.CENTER -> {
                newRect.offset(dx, dy)
                if (newRect.left < 0) newRect.offsetTo(0f, newRect.top)
                if (newRect.top < 0) newRect.offsetTo(newRect.left, 0f)
                if (newRect.right > width) newRect.offsetTo(width - newRect.width(), newRect.top)
                if (newRect.bottom > height) newRect.offsetTo(newRect.left, height - newRect.height())
            }
            DragMode.TOP_LEFT -> {
                newRect.left = (newRect.left + dx).coerceIn(0f, newRect.right - minSize)
                newRect.top = (newRect.top + dy).coerceIn(0f, newRect.bottom - minSize)
            }
            DragMode.TOP_RIGHT -> {
                newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, width.toFloat())
                newRect.top = (newRect.top + dy).coerceIn(0f, newRect.bottom - minSize)
            }
            DragMode.BOTTOM_LEFT -> {
                newRect.left = (newRect.left + dx).coerceIn(0f, newRect.right - minSize)
                newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, height.toFloat())
            }
            DragMode.BOTTOM_RIGHT -> {
                newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, width.toFloat())
                newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, height.toFloat())
            }
            DragMode.TOP -> newRect.top = (newRect.top + dy).coerceIn(0f, newRect.bottom - minSize)
            DragMode.BOTTOM -> newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, height.toFloat())
            DragMode.LEFT -> newRect.left = (newRect.left + dx).coerceIn(0f, newRect.right - minSize)
            DragMode.RIGHT -> newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, width.toFloat())
            DragMode.NONE -> return
        }
        cropRect.set(newRect)
        recalculateHandleRadius()
    }
}