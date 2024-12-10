package com.example.chargercharts2.chartbuilders

import android.content.Context
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue
import com.github.mikephil.charting.charts.LineChart

class LifeTimeChartBuilder : ChartBuilderBase() {
    override fun build(
        context: Context?,
        chart: LineChart,
        csvData: CsvData,
        ignoreZeros: Boolean,
        isDarkTheme: Boolean,
        addSetsIfNotVisible: Boolean,
        checkValueVisibility: Boolean
    ): Boolean {
        if (super.buildOld(context, chart, csvData, ignoreZeros, isDarkTheme,
                addSetsIfNotVisible = false, checkValueVisibility)) {

            //axisLeft.axisMinimum = 0f
            chart.axisRight.isEnabled = true
            chart.description.isEnabled = false
            chart.xAxis.labelRotationAngle = 45f
            //chart.xAxis.granularity = 60F
            //chart.xAxis.isGranularityEnabled = true

            setChartSettings(context, chart, csvData, isDarkTheme, CsvData.DATE_TIME_UDP_CHART_FORMAT,
                CsvData.DATE_TIME_TOOLTIP_FORMAT) { data, ds -> CsvDataValue.highlightValueFormatter(data, ds?.label) }

            return true
        }
        return false
    }
}