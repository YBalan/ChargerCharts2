package com.example.chargercharts2.chartbuilders

import android.content.Context
import android.graphics.Color
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.R
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue
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
        checkValueVisibility: Boolean = false,
    ): Boolean {
        if (csvData.values.isEmpty()) return false

        val voltage = mutableListOf<Entry>()
        val relay = mutableListOf<Entry>()
        val cycles = mutableListOf<Entry>()

        csvData.values.forEach { csvDataValue ->
            if (!ignoreZeros || csvDataValue.voltage > 0f) {
                if(!checkValueVisibility || csvDataValue.visible) {
                    val dt = csvDataValue.dateTime.toEpoch().toFloat()

                    val voltageEntry = Entry(dt, csvDataValue.voltage)
                    voltageEntry.data = csvDataValue
                    voltage.add(voltageEntry)

                    val relayEntry = Entry(dt, csvData.getValueForRelay(csvDataValue.relay))
                    relayEntry.data = csvDataValue
                    relay.add(relayEntry)

                    csvDataValue.cycle?.let { cycle ->
                        cycle.value?.let { _ ->
                            val cycleEntry = Entry(dt, csvData.getValueForCycle(cycle.type) ?: 0f)
                            cycleEntry.data = csvDataValue
                            cycles.add(cycleEntry)
                        }
                    }
                }
            }
        }

        val lineData = chart.data ?: LineData()

        if(addSetsIfNotVisible || csvData.voltageVisible) {
            val dataSetVoltage = LineDataSet(voltage, csvData.voltageLabel).apply {
                isVisible = csvData.voltageVisible
                color = csvData.voltageColor
                setCircleColor(csvData.voltageColor)
                highLightColor = csvData.voltageColor
                //highlightLineWidth = 3f
                valueFormatter = CustomYRightValueFormatter(csvData, shortValue = false)
            }
            lineData.addDataSet(dataSetVoltage)
        }

        if(addSetsIfNotVisible || csvData.relayVisible) {
            val dataSetRelay = LineDataSet(relay, csvData.relayLabel).apply {
                isVisible = csvData.relayVisible
                color = csvData.relayColor
                setCircleColor(csvData.relayColor)
                highLightColor = csvData.relayColor
                //highlightLineWidth = 3f
                valueFormatter = CustomYRightValueFormatter(csvData, shortValue = false)
            }
            lineData.addDataSet(dataSetRelay)
        }

        if(addSetsIfNotVisible || csvData.cyclesVisible) {
            val dataSetCycles = LineDataSet(cycles, csvData.cyclesLabel).apply {
                isVisible = csvData.cyclesVisible
                color = csvData.cyclesColor
                setCircleColor(csvData.cyclesColor)
                highLightColor = csvData.cyclesColor
                //highlightLineWidth = 3f
                valueFormatter = CustomYRightValueFormatter(csvData, shortValue = true)
            }
            lineData.addDataSet(dataSetCycles)
        }

        chart.data = lineData

        return true
    }

    fun addValue(context: Context?, chart: LineChart, csvData: CsvData, csvDataValue: CsvDataValue, ignoreZeros: Boolean,
                 checkValueVisibility: Boolean = false){
        if (!ignoreZeros || csvDataValue.voltage > 0f) {
            if(!checkValueVisibility || csvDataValue.visible) {
                val dt = csvDataValue.dateTime.toEpoch().toFloat()

                val voltageEntry = Entry(dt, csvDataValue.voltage)
                voltageEntry.data = csvDataValue
                addEntry(chart, csvData.voltageLabel, voltageEntry, csvData.voltageVisible, csvData.voltageColor, csvData, shortValue = false)

                val relayEntry = Entry(dt, csvData.getValueForRelay(csvDataValue.relay))
                relayEntry.data = csvDataValue
                addEntry(chart, csvData.relayLabel, relayEntry, csvData.relayVisible, csvData.relayColor, csvData, shortValue = false)

                csvDataValue.cycle?.let { cycle ->
                    cycle.value?.let { _ ->
                        val cycleEntry = Entry(dt, csvData.getValueForCycle(cycle.type) ?: 0f)
                        cycleEntry.data = csvDataValue
                        addEntry(chart, csvData.cyclesLabel, cycleEntry, csvData.cyclesVisible, csvData.cyclesColor, csvData, shortValue = true)
                    }
                }
            }
        }
    }

    private fun addEntry(
        chart: LineChart,
        label: String,
        entry: Entry,
        isVisible: Boolean,
        color: Int,
        csvData: CsvData,
        shortValue: Boolean
    ) {
        var lineDataSet = chart.data?.getDataSetByLabel(label, true)
        if (lineDataSet != null) {
            lineDataSet.addEntry(entry)
        } else {
            lineDataSet = LineDataSet(listOf(entry), label).apply {
                this.isVisible = isVisible
                this.color = color
                setCircleColor(color)
                highLightColor = color
                //highlightLineWidth = 3f
                valueFormatter = CustomYRightValueFormatter(csvData, shortValue)
            }
            if(chart.data == null) chart.data = LineData()
            chart.data.addDataSet(lineDataSet)
        }
    }

    fun setChartSettings(context: Context?, chart: LineChart, csvData: CsvData, isDarkTheme: Boolean, xAxisFormat: String, toolTipFormat: String, highlightFormatter: ((Any?, ILineDataSet?) -> String?)? = null){

        //chart.setBackgroundColor(Color.BLACK)

        if(isDarkTheme) {
            chart.axisRight.textColor = Color.WHITE
            chart.axisLeft.textColor = Color.WHITE

            chart.xAxis.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE

            chart.data.dataSets.forEach { ds ->
                ds.valueTextColor = Color.WHITE
            }
        }

        chart.isAutoScaleMinMaxEnabled = true

        chart.xAxis.valueFormatter = CustomXValueFormatter(xAxisFormat)
        chart.axisRight.valueFormatter = CustomYRightValueFormatter(csvData, shortValue = false)

        val markerView = CustomMarkerView(context, R.layout.custom_marker_view, chart.data, toolTipFormat, highlightFormatter)
        chart.marker = markerView
        markerView.chartView = chart // For MPAndroidChart 3.0+

        if(!IS_DEBUG_BUILD){
            chart.legend.isEnabled = false
        }
    }
}