package com.example.spor_tfg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import java.lang.Math.*
import kotlin.math.atan2

class VEDrawView : RelativeLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var arrowPaint: Paint = Paint()
    private var polPaint: Paint = Paint()
    private var circlePaint: Paint = Paint()
    lateinit var mPath: Path
    var strokeWidth = 10
    var currentColor = Color.WHITE

    // Arrows
    val arrowLines: ArrayList<Line> = ArrayList()
    val arrowHeads: ArrayList<Stroke> = ArrayList<Stroke>()

    // Polygons
    val polygons: HashMap<Int, ArrayList<Polygon>> = HashMap()
    var polygonNo: Int = 0
    var polygonInserted: Boolean = false
    var polPath: Path = Path()

    // Touch coordinates
    var startX = 0f
    var startY = 0f
    var mX = 0f
    var mY = 0f

    // Frame editing flags
    var arrowFlag: Boolean = false
    var polygonFlag: Boolean = false

    fun init(height: Int = 0, width: Int = 0) {
        // TODO("Maybe needs height and width in order to work")
        arrowPaint.color = Color.WHITE
        arrowPaint.strokeWidth = 10f
        /*mPaint.color = 0x101010
        mPaint.maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.SOLID)
        mPaint.strokeWidth = 10f*/
    }

    fun setArrowVal(flag: Boolean) {
        arrowFlag = flag
    }

    fun setPolygonVal(flag: Boolean) {
        polygonFlag = flag
    }

    fun resetView() {
        arrowLines.clear()
        arrowHeads.clear()
        polygons.clear()
        polygonNo = 0
    }

    private fun drawArrowHead() {
        val anglerad: Float

        // values to change for other appearance *CHANGE THESE FOR OTHER SIZE ARROWHEADS*
        val radius: Float = 110f
        val angle: Float = 90f

        // some angle calculations
        anglerad = ((PI * angle / 180.0f).toFloat())
        val lineAngle: Float = atan2(mY - startY, mX - startX) as Float

        // tha triangle
        val triangle = Path()
        triangle.fillType = Path.FillType.EVEN_ODD
        triangle.moveTo(mX, mY)
        triangle.lineTo(
            (mX - radius * kotlin.math.cos(lineAngle - anglerad / 2.0)).toFloat(),
            (mY - radius * kotlin.math.sin(lineAngle - anglerad / 2.0)).toFloat()
        )
        triangle.lineTo(
            (mX - radius * kotlin.math.cos(lineAngle + anglerad / 2.0)).toFloat(),
            (mY - radius * kotlin.math.sin(lineAngle + anglerad / 2.0)).toFloat()
        )
        triangle.close()
        arrowHeads.add(Stroke(currentColor, strokeWidth, triangle))
    }

    private fun touchStart() {
        mPath = Path()
        // finally remove any curve
        // or line from the path
        // mPath.reset()

        // this methods sets the starting
        // point of the line being drawn
        mPath.moveTo(mX, mY)
    }

    private fun touchMove() {
        mPath.lineTo(mX, mY)
    }

    private fun touchUp() {
        mPath.lineTo(mX, mY)
        arrowHeads.add(Stroke(currentColor, strokeWidth, mPath))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                startX = mX
                startY = mY
                touchStart()
                if (polygonFlag) {
                    polygonInserted = true
                    // Check if its the first shape we are inserting
                    val polygonsAL: ArrayList<Polygon>? = polygons[polygonNo]
                    // If it is initialize array list
                    if (polygonsAL.isNullOrEmpty()) polygons[polygonNo] = ArrayList()
                    polygons[polygonNo]!!.add(Polygon(mX, mY))
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                mX = event.x
                mY = event.y
                touchMove()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                mX = event.x
                mY = event.y
                // touchUp()
                if (arrowFlag) {
                    drawLine()
                    drawArrowHead()
                }
                invalidate()
            }
        }
        return true
    }

    private fun drawLine() {
        val line: Line = Line(startX, startY, mX, mY)
        arrowLines.add(line)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        // now, we iterate over the list of paths
        // and draw each path on the canvas

        val itLines: Iterator<Line> = arrowLines.iterator()
        val itArrowHeads: Iterator<Stroke> = arrowHeads.iterator()

        while (itLines.hasNext() && itArrowHeads.hasNext()) {
            // Draw arrow lines
            val line: Line = itLines.next()
            canvas.drawLine(line.from_x, line.from_y, line.to_x, line.to_y, arrowPaint)

            // Draw arrow heads
            val stroke: Stroke = itArrowHeads.next()
            arrowPaint.color = stroke.color
            arrowPaint.strokeWidth = stroke.strokeWidth.toFloat()
            canvas.drawPath(stroke.path, arrowPaint)
        }

        for ((idx, polygon) in polygons) {
            println("\n$polygon\n")
            polygon.forEachIndexed { index, point ->
                // Draw paths
                if (index == 0) polPath.moveTo(point.x, point.y)
                else polPath.lineTo(point.x, point.y)
                // polPaint.blendMode = BlendMode.SRC_ATOP
                polPaint.style = Paint.Style.FILL
                polPaint.color = Color.WHITE
                polPaint.alpha = 100
                polPaint.maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.INNER)
                polPaint.isAntiAlias = true
                canvas.drawPath(polPath, polPaint)

                // Draw circles
                circlePaint.style = Paint.Style.STROKE
                circlePaint.isAntiAlias = true
                circlePaint.strokeWidth = 10f
                circlePaint.alpha = 100
                circlePaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, 130f, circlePaint)
            }
            polPath.reset()
        }
    }


}