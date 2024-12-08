package com.example.hotpot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox

class CheckboxSpinnerAdapter(context: Context, items: List<String>) :
    ArrayAdapter<String>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
        checkBox.text = getItem(position)
        // Optionally, handle checkbox changes
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Handle checkbox change
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as CheckBox
        view.text = getItem(position)
        // Important: Don't handle checkbox changes here; instead, manage the state elsewhere
        return view
    }

}
