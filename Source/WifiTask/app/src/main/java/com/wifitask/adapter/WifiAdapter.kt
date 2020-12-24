package com.wifitask.adapter

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.wifitask.R

class WifiAdapter :
    RecyclerView.Adapter<WifiAdapter.ViewHolder>() {

    private var scanResultList: List<ScanResult> = ArrayList()
    private var mSelectedItem = -1

    fun setData(scanResultList: List<ScanResult>) {
        this.scanResultList = scanResultList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.wifi_list_item, parent, false)
        return ViewHolder(v)
    }

    fun getSelectedItem(): ScanResult {
        return scanResultList[mSelectedItem]
    }

    override fun onBindViewHolder(holder: WifiAdapter.ViewHolder, position: Int) {
        holder.bindItems(scanResultList[position], position, mSelectedItem)
    }

    override fun getItemCount(): Int {
        return scanResultList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(aMSetting: ScanResult, position: Int, selectedPosition: Int) {
            val radioButton = itemView.findViewById<RadioButton>(R.id.radioButton)
            radioButton.tag = aMSetting.SSID
            radioButton.text = aMSetting.SSID

            if ((selectedPosition == -1 && position == 0)) {
                mSelectedItem = position
                radioButton.isChecked = true
            } else
                radioButton.isChecked = selectedPosition == position


            radioButton.setOnClickListener {
                mSelectedItem = adapterPosition
                notifyDataSetChanged()
            }
        }
    }
}