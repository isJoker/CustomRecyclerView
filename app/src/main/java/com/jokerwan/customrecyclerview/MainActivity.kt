package com.jokerwan.customrecyclerview

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.setAdapter(adapter)
    }

    private val adapter = object : RecyclerView.Companion.Adapter {

        override fun onCreateViewHolder(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val itemViewType = getItemViewType(position)
            return if (itemViewType == 0) {
                this@MainActivity.layoutInflater.inflate(R.layout.item_head, parent, false)
            } else {
                this@MainActivity.layoutInflater.inflate(R.layout.item_text, parent, false)
            }
        }

        override fun onBinderViewHolder(position: Int, convertView: View, parent: ViewGroup): View {
            Log.d("wjc","onBinderViewHolder---->type" + getItemViewType(position))
            if (getItemViewType(position) == 2){
                val textView = convertView.findViewById(R.id.tv_text) as TextView
                textView.text = "手撸RecyclerView $position"
            }
            return convertView
        }

        override fun getItemViewType(row: Int): Int {
            return if (row == 0) {
                0
            } else {
                2
            }
        }

        override fun getCount(): Int {
            return 30000
        }

        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun getHeight(index: Int): Int {
            return 200
        }
    }
}
