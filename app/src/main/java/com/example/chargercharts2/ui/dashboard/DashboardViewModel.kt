package com.example.chargercharts2.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chargercharts2.analytics.DetectCycles
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue
import com.example.chargercharts2.utils.getFileNameFromUri
import kotlinx.coroutines.*

class DashboardViewModel : ViewModel() {

    private val _csvChartData = MutableLiveData<CsvData?>()
    val csvChartData: LiveData<CsvData?> get() = _csvChartData

    private val _addedEntry = MutableLiveData<CsvDataValue>()
    val addedEntry: LiveData<CsvDataValue> get() = _addedEntry

    private val _fileName = MutableLiveData<String>()
    val fileName: LiveData<String> get() = _fileName

    private var timeLapsJob: Job? = null

    val timeLapsIntervalDefault = 500L
    val timeLapsIntervalMax = 3000L
    val timeLapsIntervalMin = 50L
    val timeLapsIntervalStep = 50L
    var timeLapsInterval: Long = timeLapsIntervalDefault

    fun onVolumeUpPressed() {
        timeLapsInterval -= timeLapsIntervalStep
        if(timeLapsInterval < timeLapsIntervalMin) timeLapsInterval = timeLapsIntervalMin
    }

    fun onVolumeDownPressed() {
        timeLapsInterval += timeLapsIntervalStep
        if(timeLapsInterval > timeLapsIntervalMax) timeLapsInterval = timeLapsIntervalMax
    }

    fun parseCsvFile(context: Context, fileUri: Uri, showTimeLaps: Boolean) : Boolean {
        val csvData = CsvData.parseCsvFile(context, fileUri)
        val isFilled = !csvData.values.isEmpty()
        if(isFilled){
            val fileName = getFileNameFromUri(context, fileUri) ?: "File Name"
            if(showTimeLaps){
                _fileName.postValue("(${timeLapsInterval}ms.) $fileName")
                DetectCycles.analyzeSimple(csvData, windowSize = 3, ignoreZeros = true)
                startTimeLaps(csvData, fileName)
            }else {
                _fileName.postValue(fileName)
                DetectCycles.analyzeSimple(csvData, windowSize = 3, ignoreZeros = true)
                _csvChartData.postValue(csvData)
            }
        }
        _isFilled.postValue(isFilled)
        return isFilled
    }

    fun startTimeLaps1(csvData: CsvData, fileName: String){
        val newCsvData = CsvData(cyclesVisible = true)
        timeLapsJob = CoroutineScope(Dispatchers.Main).launch {
            csvData.values.forEach{ csvDataValue ->
                newCsvData.addValue(csvDataValue)
                _csvChartData.postValue(newCsvData)
                _fileName.postValue("(${timeLapsInterval}ms.) $fileName")
                delay(timeLapsInterval)
            }
        }
    }

    fun startTimeLaps2(csvData: CsvData, fileName: String){
        timeLapsJob = CoroutineScope(Dispatchers.Main).launch {
            csvData.cyclesVisible = true
            for (i in 0 until csvData.values.size){
                //val values = csvData.values.slice(0 until i + 1 ).toMutableList()
                //val values = csvData.values.take(i + 1).toMutableList()
                //var newCsvData: CsvData? = CsvData(csvData.maxV, csvData.minV, values, cyclesVisible = true)
                csvData.values[i].visible = true
                _csvChartData.postValue(csvData)
                _fileName.postValue("(${timeLapsInterval}ms.) $fileName")
                //newCsvData = null
                delay(timeLapsInterval)
            }
        }
    }

    fun startTimeLaps(csvData: CsvData, fileName: String){
        _csvChartData.postValue(CsvData(csvData.maxV, csvData.minV, mutableListOf(csvData.values[0]), cyclesVisible = true))
        timeLapsJob = CoroutineScope(Dispatchers.Main).launch {
            csvData.values.drop(1).forEach{ csvDataValue ->
                csvDataValue.visible = true
                _addedEntry.postValue(csvDataValue)
                _fileName.postValue("(${timeLapsInterval}ms.) $fileName")
                delay(timeLapsInterval)
            }
        }
    }

    fun stopTimeLaps(){
        timeLapsJob?.cancel()
        timeLapsJob = null
        _isFilled.postValue(false)
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
