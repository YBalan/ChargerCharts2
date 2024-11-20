package com.example.chargercharts2.chartbuilders

import android.content.Context
import android.graphics.Color
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.R
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.utils.CustomMarkerView
import com.example.chargercharts2.utils.CustomXValueFormatter
import com.example.chargercharts2.utils.CustomYRightValueFormatter
import com.example.chargercharts2.utils.toEpoch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

open class ChartBuilderBase {
    open fun build(
        context: Context?,
        chart: LineChart,
        csvData: CsvData,
        ignoreZeros: Boolean,
        isDarkTheme: Boolean,
        addSetsIfNotVisible: Boolean = true,
    ): Boolean {
        if (csvData.values.isEmpty()) return false

        val voltage = mutableListOf<Entry>()
        val relay = mutableListOf<Entry>()
        val cycles = mutableListOf<Entry>()

        csvData.values.forEach { csvDataValue ->
            if (!ignoreZeros || csvDataValue.voltage > 0f) {
                val dt = csvDataValue.dateTime.toEpoch().toFloat()

                val voltageEntry = Entry(dt, csvDataValue.voltage)
                voltageEntry.data = csvDataValue
                voltage.add(voltageEntry)

                val relayEntry = Entry(dt, csvData.getValueForRelay(csvDataValue.relay))
                relayEntry.data = csvDataValue
                relay.add(relayEntry)

                csvDataValue.cycle?.let {
                    if(it.value != null) {
                        val cycleEntry = Entry(dt, it.value ?: 0f)
                        cycleEntry.data = csvDataValue
                        cycles.add(cycleEntry)
                    }
                }
            }
        }

        val lineData = chart.data ?: LineData()

        if(addSetsIfNotVisible || csvData.voltageVisible) {
            val dataSetVoltage = LineDataSet(voltage, csvData.voltageLabel)
            dataSetVoltage.isVisible = csvData.voltageVisible
            dataSetVoltage.color = csvData.voltageColor
            dataSetVoltage.setCircleColor(csvData.voltageColor)
            dataSetVoltage.highLightColor = csvData.voltageColor
            //dataSetVoltage.highlightLineWidth = 3f
            lineData.addDataSet(dataSetVoltage)
        }

        if(addSetsIfNotVisible || csvData.relayVisible) {
            val dataSetRelay = LineDataSet(relay, csvData.relayLabel)
            dataSetRelay.isVisible = csvData.relayVisible
            dataSetRelay.color = csvData.relayColor
            dataSetRelay.setCircleColor(csvData.relayColor)
            dataSetRelay.highLightColor = csvData.relayColor
            //dataSetRelay.highlightLineWidth = 3f
            lineData.addDataSet(dataSetRelay)
        }

        if(addSetsIfNotVisible || csvData.cyclesVisible) {
            val dataSetCycles = LineDataSet(cycles, csvData.cyclesLabel)
            dataSetCycles.isVisible = csvData.cyclesVisible
            dataSetCycles.color = csvData.cyclesColor
            dataSetCycles.setCircleColor(csvData.cyclesColor)
            dataSetCycles.highLightColor = csvData.cyclesColor
            //dataSetCycles.highlightLineWidth = 3f
            lineData.addDataSet(dataSetCycles)
        }

        chart.data = lineData

        return true
    }

    fun setChartSettings(context: Context?, chart: LineChart, csvData: CsvData, isDarkTheme: Boolean, xAxisFormat: String, toolTipFormat: String, highlightFormatter: ((Any?, ILineDataSet?) -> String?)? = null){

        //chart.setBackgroundColor(Color.BLACK)

        if(isDarkTheme) {
            chart.axisRight.textColor = Color.WHITE
            chart.axisLeft.textColor = Color.WHITE

            chart.xAxis.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE
        }

        chart.isAutoScaleMinMaxEnabled = true

        chart.xAxis.valueFormatter = CustomXValueFormatter(xAxisFormat)
        chart.axisRight.valueFormatter = CustomYRightValueFormatter(csvData)

        val markerView = CustomMarkerView(context, R.layout.custom_marker_view, chart.data, toolTipFormat, highlightFormatter)
        chart.marker = markerView
        markerView.chartView = chart // For MPAndroidChart 3.0+

        if(!IS_DEBUG_BUILD){
            chart.legend.isEnabled = false
        }

    }
}