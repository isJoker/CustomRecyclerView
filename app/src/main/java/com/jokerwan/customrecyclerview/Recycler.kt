package com.jokerwan.customrecyclerview

import android.view.View
import java.util.*

/**
 * Created by JokerWan on 2019-11-03.
 * Function: 回收池
 */
class Recycler(typeNumber: Int) {

    // Stack数组
//    private var views: Array<Stack<View>?> = arrayOfNulls(typeNumber)
    private var views: HashMap<Int,Stack<View>?> = hashMapOf()

    init {
        for (i in 0 until typeNumber) {
            views[i] = Stack()
        }
    }

    fun put(view: View, type: Int) {
        views[type]?.push(view)
    }

    operator fun get(type: Int): View? {
        return try {
            views[type]?.pop()
        } catch (e: Exception) {
            null
        }
    }

}