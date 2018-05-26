package com.iamjinge.nestedscroll

import android.content.Context
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Created by Elton on 2018/5/20.
 */
class ChildView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

    private val touchSlop: Int
    private val lastPos: FloatArray = FloatArray(2)
    private val nestedOffset: IntArray = IntArray(2)
    private val scrollOffset: IntArray = IntArray(2)
    private val scrollConsumed: IntArray = IntArray(2)
    private var isDrag = false

    init {
        isNestedScrollingEnabled = true
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private val TAG = "ChildView"

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        super.setNestedScrollingEnabled(enabled)
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return childHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        return childHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return childHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int,
                                      dxUnconsumed: Int, dyUnconsumed: Int,
                                      offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?,
                                         offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy,
                consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float,
                                     consumed: Boolean): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    }

    override fun measureChild(child: View, parentWidthMeasureSpec: Int,
                              parentHeightMeasureSpec: Int) {
        val lp = child.layoutParams

        val childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED)

        val childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun measureChildWithMargins(child: View, parentWidthMeasureSpec: Int, widthUsed: Int,
                                         parentHeightMeasureSpec: Int, heightUsed: Int) {
        val lp = child.layoutParams as ViewGroup.MarginLayoutParams

        val childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                lp.leftMargin + lp.rightMargin, View.MeasureSpec.UNSPECIFIED)
        val childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                lp.topMargin + lp.bottomMargin, View.MeasureSpec.UNSPECIFIED)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vtev = MotionEvent.obtain(event)
        Log.d(TAG, "before offset ${event.y}")
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            nestedOffset[0] = 0
            nestedOffset[1] = 0
        }
        vtev.offsetLocation(nestedOffset[0].toFloat(), nestedOffset[1].toFloat())
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPos[0] = event.x
                lastPos[1] = event.y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL or ViewCompat.SCROLL_AXIS_HORIZONTAL)
                isDrag = false
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x.toInt()
                val y = event.y.toInt()
                var deltaY: Int = (lastPos[1] - y).toInt()
                var deltaX: Int = (lastPos[0] - x).toInt()

                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
//                    deltaY -= scrollConsumed[1]
//                    vtev.offsetLocation(0f, scrollOffset[1].toFloat())
//                    nestedOffset[1] += scrollOffset[1]
                }
                if (!isDrag && Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()) > touchSlop) {
                    isDrag = true
//                    if (deltaY > 0) {
//                        deltaY -= touchSlop
//                    } else {
//                        deltaY += touchSlop
//                    }
                }
                if (isDrag) {
                    lastPos[0] = x.toFloat()
                    lastPos[1] = y.toFloat() //- scrollOffset[1]
                    val oldX = scrollX
                    val oldY = scrollY
                    var consumedX = deltaX
                    var consumedY = deltaY
                    if (oldX + deltaX < 0) {
                        consumedX = 0 - oldX
                    }
                    if (oldX + deltaX > getScrollRange()[0]) {
                        consumedX = getScrollRange()[0] - oldX
                    }
                    if (oldY + deltaY < 0) {
                        consumedY = 0 - oldY
                    }
                    if (oldY + deltaY > getScrollRange()[1]) {
                        consumedY = getScrollRange()[1] - oldY
                    }
                    scrollBy(consumedX, consumedY)
                    val scrolledX = scrollX - oldX
                    val scrolledY = scrollY - oldY

                    if (dispatchNestedScroll(scrolledX, scrolledY,
                                    deltaX - scrolledX, deltaY - scrolledY,
                                    scrollOffset)) {
                        lastPos[0] -= scrollOffset[0].toFloat()
                        lastPos[1] -= scrollOffset[1].toFloat()
                        vtev.offsetLocation(scrollOffset[0].toFloat(), scrollOffset[1].toFloat())
                        nestedOffset[0] += scrollOffset[0]
                        nestedOffset[1] += scrollOffset[1]
                    }
//                    scrollBy(0, scrollOffset[1])
                    Log.d(TAG, "lastPos ${lastPos[1]} offset ${scrollOffset[1]}, nested ${nestedOffset[1]}")
                }
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                stopNestedScroll()
                isDrag = false
            }
        }
        vtev.recycle()
        return true
    }

    fun tryScroll(detalX: Int, detalY: Int,
                  scrollX: Int, scrollY: Int,
                  scrollRangeX: Int, scrollRangeY: Int) {
        val canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent()
        val canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent()

    }

    fun getScrollRange(): IntArray {
        var scrollRange = IntArray(2)
        if (childCount > 0) {
            var child = getChildAt(0)
            scrollRange[0] = Math.max(0, child.width - (width - paddingBottom - paddingTop))
            scrollRange[1] = Math.max(0, child.height - (height - paddingBottom - paddingTop))
        }
        return scrollRange
    }
}
