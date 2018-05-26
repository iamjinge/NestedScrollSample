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

        val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(parentWidthMeasureSpec,
                paddingLeft + paddingRight, lp.width)

        val childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun measureChildWithMargins(child: View, parentWidthMeasureSpec: Int, widthUsed: Int,
                                         parentHeightMeasureSpec: Int, heightUsed: Int) {
        val lp = child.layoutParams as ViewGroup.MarginLayoutParams

        val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(parentWidthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width)
        val childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                lp.topMargin + lp.bottomMargin, View.MeasureSpec.UNSPECIFIED)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vtev = MotionEvent.obtain(event)
        Log.d(TAG, "before offset ${event.y}")
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            nestedOffset[1] = 0
        }
        vtev.offsetLocation(0f, nestedOffset[1].toFloat())
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPos[0] = event.x
                lastPos[1] = event.y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                isDrag = false
            }

            MotionEvent.ACTION_MOVE -> {
                val y = event.y.toInt()
                val ly = lastPos[1]
                var deltaY: Int = (ly - y).toInt()
                Log.d(TAG, "last ${ly}, touch $y, move $deltaY")
//                scrollBy(0, deltaY)

                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
//                    deltaY -= scrollConsumed[1]
//                    vtev.offsetLocation(0f, scrollOffset[1].toFloat())
//                    nestedOffset[1] += scrollOffset[1]
                }
                if (!isDrag && Math.abs(deltaY) > touchSlop) {
                    isDrag = true
                    if (deltaY > 0) {
                        deltaY -= touchSlop
                    } else {
                        deltaY += touchSlop
                    }
                }
                if (isDrag) {
                    lastPos[1] = y.toFloat() //- scrollOffset[1]
                    val oldY = scrollY
                    var consumY = deltaY
                    if (oldY + deltaY < 0) {
                        consumY = 0 - oldY
                    }
                    if (oldY + deltaY > getScrollRange()) {
                        consumY = getScrollRange() - oldY
                    }
                    scrollBy(0, consumY)
                    val scrolled = scrollY - oldY
//                    Log.d(TAG, "oldy $oldY, delta $deltaY, height $height ${getChildAt(0).height}, consume $consumY, scroll $scrolled")
                    if (dispatchNestedScroll(0, scrolled, 0, deltaY - scrolled, scrollOffset)) {
                        lastPos[1] -= scrollOffset[1].toFloat()
                        vtev.offsetLocation(0f, scrollOffset[1].toFloat())
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

    fun getScrollRange(): Int {
        var scrollRange = 0;
        if (childCount > 0) {
            var child = getChildAt(0)
            scrollRange = Math.max(0, child.height - (height - paddingBottom - paddingTop))
        }
        return scrollRange
    }
}
