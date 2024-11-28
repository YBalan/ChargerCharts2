package com.example.chargercharts2.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.analytics.DetectCycles
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.utils.UdpListener
import com.example.chargercharts2.utils.getFileNameFromUri

class DashboardViewModel : ViewModel() {

    private val _csvChartData = MutableLiveData<CsvData?>()
    val csvChartData: LiveData<CsvData?> get() = _csvChartData

    private val _fileName = MutableLiveData<String>()
    val fileName: LiveData<String> get() = _fileName

    fun parseCsvFile(context: Context, fileUri: Uri) : Boolean {
        val csvData = CsvData.parseCsvFile(context, fileUri)
        DetectCycles.analyzeSimple(csvData, windowSize = 3, ignoreZeros = true)
        _csvChartData.postValue(csvData)
        _fileName.postValue(getFileNameFromUri(context, fileUri) ?: "File Name")

        val isFilled = !csvData.values.isEmpty()
        _isFilled.postValue(isFilled)
        return isFilled
    }

    fun clear(){
        _csvChartData.postValue(CsvData())
        _isFilled.postValue(false)
        _fileName.postValue("")
    }

    fun isEmpty() : Boolean{
        return _csvChartData.value?.values?.isEmpty() != false
    }

    private val _isFilled = MutableLiveData<Boolean>()
    val isFilled: LiveData<Boolean> get() = _isFilled
}
