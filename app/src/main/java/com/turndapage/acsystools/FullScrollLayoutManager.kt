package com.turndapage.acsystools

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.max

class FullScrollLayoutManager(context: Context?, val scrollview: HorizontalScrollView) : LinearLayoutManager(context) {
    private var offset = 0
    private var maxOffset = 0
    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        val n = childCount
        offset = 0
        maxOffset = 0
        val ownWidth = width
        for (i in 0 until n) {
            val view: View? = getChildAt(i)
            val x: Int = view?.right ?: 0
            if (x > ownWidth) maxOffset = max(maxOffset, x - ownWidth)
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(horizontal: Int, recycler: Recycler, state: RecyclerView.State): Int {
        var dx = horizontal
        if (dx < 0) {
            if (-dx > offset) dx = -offset
        } else if (dx > 0) {
            if (dx + offset > maxOffset) dx = maxOffset - offset
        }
        offsetChildrenHorizontal(-dx)
        offset += dx
        scrollview.scrollX = offset
        return dx
    }
}