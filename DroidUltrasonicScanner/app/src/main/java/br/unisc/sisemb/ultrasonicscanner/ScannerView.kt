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
        offsetY = (canvas.height/2f) - width/2f
        val height = width + offsetY
        centerX = width/2f
        centerY = height/2f
        lineDensity = centerX/MAX_DISTANCE

        paint.setARGB(255, 0, 139, 0)
        paint.strokeWidth = 3f
        val rectF = RectF(0f, offsetY, width, height)
        canvas.drawArc(rectF, 135f, 270f, true, paint)

        drawGuideLines(canvas)

        var dist = distance * lineDensity
        var angleRadians = (Math.PI / 180f) * angle
        var offY = offsetY/2
        var x = centerX + (Math.cos(angleRadians) * dist)
        var y = centerY + offY + (Math.sin(angleRadians) * dist)
        paint.setARGB(255, 0, 0, 0)
        paint.strokeWidth = 6f
        canvas.drawLine(centerX, centerY + offY, x.toFloat(), y.toFloat(), paint)

        paint.setARGB(120, 255, 255, 0)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 8f, paint)

        paint.setARGB(255, 0, 0, 0)
        paint.strokeWidth = 1f
        paint.textSize = 18f
        canvas.drawText("ângulo:", centerX - 60, centerY + 126 * lineDensity, paint)
        canvas.drawText("distância:", centerX - 78, centerY + 144 * lineDensity, paint)
        canvas.drawText((angle - 270f).toString()+"º", centerX + 6, centerY + 126 * lineDensity, paint)
        canvas.drawText(distance.toString()+"cm", centerX + 6, centerY + 144 * lineDensity, paint)
    }

    fun blank(canvas: Canvas) {
        paint.setARGB(25, 255, 255, 1)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
    }

    fun drawGuideLines(canvas: Canvas) {
        var angles = floatArrayOf(0f,45f,135f,180f,225f,270f,315f)
        paint.setARGB(100, 0, 252, 0)
        paint.strokeWidth = 4f
        for (a in angles) {
            var angleRadians1 = (Math.PI / 180f) * a
            var offY = offsetY/2
            var x = centerX + (Math.cos(angleRadians1) * MAX_DISTANCE * lineDensity)
            var y = centerY + offY + (Math.sin(angleRadians1) * MAX_DISTANCE * lineDensity)
            canvas.drawLine(centerX, centerY + offY, x.toFloat(), y.toFloat(), paint)
        }
    }
}
