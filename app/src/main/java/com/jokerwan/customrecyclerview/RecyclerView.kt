package com.jokerwan.customrecyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import java.util.*

/**
 * Created by JokerWan on 2019-11-03.
 * Function: 手撸RecyclerView实现一个简单的可在有限窗口展示千万级item的列表
 */
class RecyclerView(context: Context?, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private var adapter: Adapter? = null
    //当前显示的View
    private var viewList: MutableList<View?> = ArrayList()
    //当前滑动的y值
    private var currentY: Int = 0
    //行数
    private var rowCount: Int = 0
    //view的第一行  是占内容的几行
    private var firstRow: Int = 0
    //y偏移量
    private var mScrollY: Int = 0
    //初始化  第一屏最慢
    private var needRelayout: Boolean = true
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    //item  高度
    private lateinit var heights: IntArray
    // 回收池
    internal var recycler: Recycler? = null
    //最小滑动距离
    private var touchSlop: Int = 0

    init {
        val configuration = ViewConfiguration.get(context)
        this.touchSlop = configuration.scaledTouchSlop
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val h: Int
        adapter?.let {
            heights = IntArray(rowCount)
            this.rowCount = it.getCount()
            for (i in heights.indices) {
                heights[i] = it.getHeight(i)
            }
        }

        // 数据的高度
        val tmpH = sumArray(heights, 0, heights.size)
        // 取最小值
        h = heightSize.coerceAtMost(tmpH)
        setMeasuredDimension(widthSize, h)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (needRelayout || changed) {
            needRelayout = false
            viewList.clear()
            removeAllViews()

            adapter.let {
                // 摆放
                mWidth = r - l
                mHeight = b - t
                val left = 0
                var top = 0
                var right: Int
                var bottom: Int

                var i = 0
                while (i < rowCount && top < mHeight) {
                    right = mWidth
                    bottom = top + heights[i]
                    // 生成一个View
                    val view = makeAndStep(i, left, top, right, bottom)
                    viewList.add(view)
                    //循环摆放
                    top = bottom
                    i++
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                // 移动的距离   y方向
                val y2 = event.rawY.toInt()
                // 上滑正  下滑负
                val diffY = currentY - y2
                // 画布移动  并不影响子控件的位置，所以需要移动子控件
                scrollBy(0, diffY)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        var intercept = false
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                currentY = ev.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val y2 = kotlin.math.abs(currentY - ev.rawY.toInt())
                if (y2 > touchSlop) {
                    intercept = true
                }
            }
        }
        return intercept
    }

    override fun scrollBy(x: Int, y: Int) {
        // mScrollY表示 第一个可见Item的左上顶点 距离屏幕的左上顶点的距离
        mScrollY += y
        mScrollY = scrollBounds(mScrollY)

        if (mScrollY > 0) {
            // 上滑正  下滑负  边界值
            while (mScrollY > heights[firstRow]) {
                // 1 上滑移除  2 上划加载  3下滑移除  4 下滑加载
                viewList.removeAt(0)?.let { removeView(it) }
                mScrollY -= heights[firstRow]
                firstRow++
            }
            while (getFillHeight() < mHeight) {
                val addLast = firstRow + viewList.size
                val view = obtainView(addLast, mWidth, heights[addLast])
                viewList.add(viewList.size, view)
            }

        } else if (mScrollY < 0) {
            // 下滑加载
            while (mScrollY < 0) {
                val firstAddRow = firstRow - 1
                val view = obtainView(firstAddRow, mWidth, heights[firstAddRow])
                viewList.add(0, view)
                firstRow--
                mScrollY += heights[firstAddRow]
            }

            // 下滑移除
            while (sumArray(
                    heights,
                    firstRow,
                    viewList.size
                ) - mScrollY - heights[firstRow + viewList.size - 1] >= mHeight
            ) {
                viewList.removeAt(viewList.size - 1)?.let { removeView(it) }
            }
        }

        repositionViews()
    }

    private fun scrollBounds(scrollY: Int): Int {
        // 上滑
        return if (scrollY > 0) {
            scrollY.coerceAtMost(sumArray(heights, firstRow, heights.size - firstRow) - mHeight)
        } else {
            // 极限值  会取零  非极限值的情况下
            scrollY.coerceAtLeast(-sumArray(heights, 0, firstRow))
        }
    }

    // 重新摆放子控件
    private fun repositionViews() {
        val left = 0
        var top: Int
        val right = mWidth
        var bottom: Int
        var i = firstRow
        top = -mScrollY
        for (view in viewList) {
            bottom = top + heights[i++]
            view?.layout(left, top, right, bottom)
            top = bottom
        }
    }

    // 获取当前所有条目的高度累计
    private fun getFillHeight(): Int {
        // 数据的高度 -mScrollY
        return sumArray(heights, firstRow, viewList.size) - mScrollY
    }

    public fun getAdapter() = adapter

    /**
     * 设置适配器
     */
    public fun setAdapter(adapter: Adapter?) {
        this.adapter = adapter
        adapter?.run {
            recycler = Recycler(getViewTypeCount())
            mScrollY = 0
            firstRow = 0
            needRelayout = true
            requestLayout()//1  onMeasure   2  onLayout
        }
    }

    private fun sumArray(array: IntArray, firstIndex: Int, size: Int): Int {
        var count = size
        var sum = 0
        count += firstIndex
        for (i in firstIndex until count) {
            sum += array[i]
        }
        return sum
    }

    private fun makeAndStep(row: Int, left: Int, top: Int, right: Int, bottom: Int): View? {
        val view = obtainView(row, right - left, bottom - top)
        view?.layout(left, top, right, bottom)
        return view
    }

    private fun obtainView(row: Int, width: Int, height: Int): View? {
        // key type
        val itemType = adapter?.getItemViewType(row)
        // 取不到
        val recycleView = itemType?.let { recycler?.get(it) }
        val view: View?
        if (recycleView == null) {
            view = adapter?.onCreateViewHolder(row, recycleView, this)
            recycleView?.let { adapter?.onBinderViewHolder(row, it, this) }
            if (view == null) {
                throw RuntimeException("onCreateViewHolder  必须填充布局")
            }
        } else {
            view = adapter?.onBinderViewHolder(row, recycleView, this)
        }
        view?.setTag(R.id.tag_type_view, itemType)
        view?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        addView(view, 0)
        return view
    }

    override fun removeView(view: View) {
        super.removeView(view)
        val key = view.getTag(R.id.tag_type_view) as Int
        recycler?.put(view, key)
    }

    companion object {
        interface Adapter {
            fun onCreateViewHolder(position: Int, convertView: View?, parent: ViewGroup): View
            fun onBinderViewHolder(position: Int, convertView: View, parent: ViewGroup): View
            fun getItemViewType(row: Int): Int
            fun getCount(): Int
            //Item的类型数量
            fun getViewTypeCount(): Int

            fun getHeight(index: Int): Int
        }
    }
}

