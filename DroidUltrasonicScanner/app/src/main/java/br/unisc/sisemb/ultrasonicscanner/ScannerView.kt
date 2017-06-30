package br.unisc.sisemb.ultrasonicscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.RectF
import java.util.*


/**
 * Created by dieg0 on 16/06/2017.
 */
class ScannerView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint: Paint = Paint()

    var centerX = 0f
    var centerY = 0f
    var MAX_DISTANCE = 250f
    var lineDensity = 0.4f
    var offsetY = 20f
    val offsetX = 80f
    var angle = 270f
    var distance = 0f
    var graphPoints = arrayListOf<Float>()

    init {
        val rand = Random()
        for (i in 0..360){
            var x = 30f
            graphPoints.add(i, rand.nextInt(250).toFloat())
        }
    }

    fun insertGraphPoint(distance: Float) {
        graphPoints.removeAt(0)
        graphPoints.add(distance)
    }

    override fun onDraw(canvas: Canvas) {
        background(canvas)

        val width = 300f
        offsetY = 0f
        val height = width + offsetY
        centerX = width/2f
        centerY = height/2f
        lineDensity = centerX/MAX_DISTANCE
        centerX += offsetX

        paint.setARGB(255, 0, 139, 0)
        paint.strokeWidth = 3f
        canvas.drawArc(RectF(offsetX, offsetY, offsetX + width, height), 135f, 270f, true, paint)

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
        canvas.drawText("ângulo:", centerX - 60, centerY + 96, paint)
        canvas.drawText("distância:", centerX - 78, centerY + 114, paint)
        canvas.drawText((angle - 270f).toInt().toString()+"º", centerX + 6, centerY + 96, paint)
        canvas.drawText(distance.toInt().toString()+"cm", centerX + 6, centerY + 114, paint)

        canvas.drawLine(40f,550f,400f,550f,paint)
        canvas.drawLine(40f,300f,40f,550f,paint)

        canvas.drawText("pacotes", 180f, 570f, paint)
        canvas.drawText("dx", 10f, 318f, paint)
        canvas.drawText("cm", 10f, 330f, paint)

        // update graph
        for (i in 0..359) {
            paint.setARGB(255, 0, 0, 0)
            paint.strokeWidth = 3f
            canvas.drawPoint(40f + i, 550f - (250f * graphPoints.get(i))/MAX_DISTANCE,paint)
            if (i>0 && i<361) {
                paint.strokeWidth = 1f
                paint.setARGB(50, 30, 30, 255)
                canvas.drawLine(40f + i, 550f - (250f * graphPoints.get(i - 1))/MAX_DISTANCE, 40f + i, 550f - (250f * graphPoints.get(i))/MAX_DISTANCE, paint)
            }
        }
    }

    fun background(canvas: Canvas) {
        paint.setARGB(25, 255, 255, 1)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
    }

    /*  \  |  /
     *   \ | /
     *____\|/_____
     *    / \
     *   /   \
     */
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
