package com.example.chargercharts2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue
import  com.example.chargercharts2.utils.*

class HomeViewModel : ViewModel() {
    val dataSets: LiveData<Map<String, List<CsvDataValue>>> = UdpListener.dataSets
    val removedEntry: LiveData<CsvDataValue> = UdpListener.removedEntry

    //val addedEntry : LiveData<CsvDataValue> = UdpListener.addedEntry
    val csvDataMap = mutableMapOf<String, CsvData>()
    val isStarted: LiveData<Boolean> = UdpListener.isListening

    fun clear() {
        csvDataMap.clear()
    }
}
