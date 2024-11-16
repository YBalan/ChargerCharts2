package com.example.chargercharts2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.models.CsvDataValue
import  com.example.chargercharts2.utils.*
import com.github.mikephil.charting.data.LineDataSet

class HomeViewModel : ViewModel() {
    val dataSets: LiveData<Map<String, List<CsvDataValue>>> = UdpListener.dataSets
    val removedEntry : LiveData<CsvDataValue> = UdpListener.removedEntry
    val dataSetsMap = mutableMapOf<String, LineDataSet>()

    fun clear(){
        dataSetsMap.clear()
    }
}
