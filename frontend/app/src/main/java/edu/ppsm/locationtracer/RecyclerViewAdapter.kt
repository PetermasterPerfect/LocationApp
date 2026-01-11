package edu.ppsm.locationtracer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.ppsm.locationtracer.RecyclerViewAdapter.ViewHolder

class RecyclerViewAdapter(context: Context?, private var dataSet: List<Point>) :
    RecyclerView.Adapter<ViewHolder>() {

    interface IItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    private var mClickListener: IItemClickListener? = null
    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var textNum: TextView
        var textTime: TextView
        var textLonLat: TextView

        init {
            textNum = itemView.findViewById(R.id.num)
            textTime = itemView.findViewById(R.id.textTime)
            textLonLat = itemView.findViewById(R.id.lonlat)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            mClickListener?.onItemClick(view, adapterPosition)
        }
    }

    fun setClickListener(itemClickListener: IItemClickListener?) {
        mClickListener = itemClickListener
    }
    private val mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textNum.text = (position+1).toString()
        viewHolder.textTime.text = dataSet[position].time
        viewHolder.textLonLat.text = String.format("%.2f", dataSet[position].lon) + " - " + String.format("%.2f", dataSet[position].lat)
    }

    override fun getItemCount() = dataSet.size
}