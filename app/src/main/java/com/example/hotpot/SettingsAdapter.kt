package com.example.hotpot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    private val items: List<SettingItem>,
    private val listener: OnSettingsItemClickListener
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, listener)
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val title: TextView = view.findViewById(R.id.setting_title)
        private val chevron: ImageView = view.findViewById(R.id.chevron)

        fun bind(item: SettingItem, listener: OnSettingsItemClickListener) {
            icon.setImageResource(item.icon)
            title.text = item.title
            // The chevron icon can be set here if different for items, otherwise, it's already defined in XML

            itemView.setOnClickListener {
                listener.onItemClick(item)
            }
        }
    }
}
