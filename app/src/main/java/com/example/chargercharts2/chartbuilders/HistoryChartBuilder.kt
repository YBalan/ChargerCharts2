package com.example.chargercharts2.chartbuilders

import android.content.Context
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvData.Companion.DATE_TIME_CSV_CHART_FORMAT
import com.example.chargercharts2.models.CsvData.Companion.DATE_TIME_TOOLTIP_FORMAT
import com.example.chargercharts2.models.CsvDataValue
import com.example.chargercharts2.utils.toEpoch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData

class HistoryChartBuilder : ChartBuilderBase() {
    override fun build(
        context: Context?,
        chart: LineChart,
        csvData: CsvData,
        ignoreZeros: Boolean,
        isDarkTheme: Boolean,
        addSetsIfNotVisible: Boolean,
        checkValueVisibility: Boolean
    ): Boolean {
        chart.data = LineData()
        if (super.build(context, chart, csvData, ignoreZeros, isDarkTheme,
                addSetsIfNotVisible = true, checkValueVisibility)) {

            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            val desiredLabelsCount = 48
            var granularity =
                ((csvData.values.last().dateTime.toEpoch() - csvData.values.first().dateTime.toEpoch()) / desiredLabelsCount).toFloat()

            xAxis.granularity = granularity // in milliseconds
            xAxis.isGranularityEnabled = granularity > 0f
            xAxis.labelRotationAngle = -45f // Rotate labels for better visibility

            //chart.viewPortHandler.setMaximumScaleX(5f) // Allow max zoom level of 5x
            //chart.viewPortHandler.setMinimumScaleX(1f) // Allow minimum zoom level of 1x

            chart.description.isEnabled = false

            setChartSettings(
                context, chart, csvData, isDarkTheme, DATE_TIME_CSV_CHART_FORMAT,
                DATE_TIME_TOOLTIP_FORMAT
            ) { data, ds -> CsvDataValue.highlightValueFormatter(data, null) }

            chart.invalidate() // Refresh the chart

            return true
        }

        return false
    }
}

