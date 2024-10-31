package com.example.chargercharts2.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.utils.getFileNameFromUri
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.BufferedReader
import java.io.InputStreamReader

class DashboardViewModel : ViewModel() {

    private val _csvChartData = MutableLiveData<CsvData?>()
    val csvChartData: LiveData<CsvData?> get() = _csvChartData

    private val _fileName = MutableLiveData<String>()
    val fileName: LiveData<String> get() = _fileName

    fun parseCsvFile(context: Context, fileUri: Uri) {
        _csvChartData.postValue(com.example.chargercharts2.models.parseCsvFile(context, fileUri))
        _fileName.postValue(getFileNameFromUri(context, fileUri) ?: "File Name")
    }
}
