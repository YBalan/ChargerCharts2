package com.example.chargercharts2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import  com.example.chargercharts2.utils.*
import com.github.mikephil.charting.data.LineDataSet

class HomeViewModel : ViewModel() {
    val dataSets: LiveData<Map<String, List<Pair<Float, Float>>>> = UdpListener.dataSets
    val removedEntry : LiveData<Pair<Float, Float>> = UdpListener.removedEntry
    val dataSetsMap = mutableMapOf<String, LineDataSet>()

    fun clear(){
        dataSetsMap.clear()
    }
}
