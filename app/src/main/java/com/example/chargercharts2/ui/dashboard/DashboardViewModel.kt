package com.example.chargercharts2.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.Cycle
import com.example.chargercharts2.utils.getFileNameFromUri

class DashboardViewModel : ViewModel() {

    private val _csvChartData = MutableLiveData<CsvData?>()
    val csvChartData: LiveData<CsvData?> get() = _csvChartData

    private val _fileName = MutableLiveData<String>()
    val fileName: LiveData<String> get() = _fileName

    fun parseCsvFile(context: Context, fileUri: Uri) {
        _csvChartData.postValue(CsvData.parseCsvFile(context, fileUri))
        _fileName.postValue(getFileNameFromUri(context, fileUri) ?: "File Name")
    }

    fun clear(){
        _csvChartData.postValue(CsvData(0.0f, 0.0f, emptyList(), mutableListOf()))
    }

    fun isEmpty() : Boolean{
        return _csvChartData.value?.values?.isEmpty() != false
    }
}
