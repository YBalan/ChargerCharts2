package com.example.chargercharts2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import  com.example.chargercharts2.utils.*

class HomeViewModel : ViewModel() {
    val dataSets: LiveData<Map<String, List<Pair<Float, Float>>>> = UdpListener.dataSets
}
