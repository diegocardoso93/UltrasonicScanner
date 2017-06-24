package br.unisc.sisemb.ultrasonicscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.RectF


/**
 * Created by dieg0 on 16/06/2017.
 */
class ScannerView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint:Paint = Paint()

    var centerX = 0f
    var centerY = 0f
    var MAX_DISTANCE = 200f
    var lineDensity = 0.4f
    var offsetY = 0f
    var angle = 270f
    var distance = 0f

    override fun onDraw(canvas: Canvas) {
        blank(canvas)

        val width = canvas.width.toFloat()
        offsetY = 120f
        val height = width + offsetY
        centerX = width/2f
        centerY = height/2f
        lineDensity = centerX/MAX_DISTANCE

        paint.setARGB(255, 0, 139, 0)
        paint.strokeWidth = 3f
        canvas.drawArc(RectF(0f, offsetY, width, height), 135f, 270f, true, paint)

        drawGuideLines(canvas)

        val dist = distance * lineDensity
        val angleRadians = (Math.PI / 180f) * angle
        val offY = offsetY/2
        val x = centerX + (Math.cos(angleRadians) * dist)
        val y = centerY + offY + (Math.sin(angleRadians) * dist)
        paint.setARGB(255, 0, 0, 0)
        paint.strokeWidth = 6f
        canvas.drawLine(centerX, centerY + offY, x.toFloat(), y.toFloat(), paint)

        paint.setARGB(120, 255, 255, 0)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 8f, paint)

        paint.setARGB(255, 0, 0, 0)
        paint.strokeWidth = 1f
        paint.textSize = 18f
        canvas.drawText("ângulo:", centerX - 60, centerY + 156, paint)
        canvas.drawText("distância:", centerX - 78, centerY + 174, paint)
        canvas.drawText((angle - 270f).toInt().toString()+"º", centerX + 6, centerY + 156, paint)
        canvas.drawText(distance.toInt().toString()+"cm", centerX + 6, centerY + 174, paint)
    }

    fun blank(canvas: Canvas) {
        paint.setARGB(25, 255, 255, 1)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
    }

    fun drawGuideLines(canvas: Canvas) {
        val angles = floatArrayOf(0f, 45f, 135f, 180f, 225f, 270f, 315f)
        paint.setARGB(100, 0, 252, 0)
        paint.strokeWidth = 4f
        for (a in angles) {
            val angleRadians1 = (Math.PI / 180f) * a
            val offY = offsetY/2
            val x = centerX + (Math.cos(angleRadians1) * MAX_DISTANCE * lineDensity)
            val y = centerY + offY + (Math.sin(angleRadians1) * MAX_DISTANCE * lineDensity)
            canvas.drawLine(centerX, centerY + offY, x.toFloat(), y.toFloat(), paint)
        }
    }
}
