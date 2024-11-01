package com.example.chargercharts2.utils

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.chargercharts2.databinding.CustomMarkerViewBinding
import com.github.mikephil.charting.formatter.ValueFormatter
import android.view.LayoutInflater
import com.example.chargercharts2.R
import com.example.chargercharts2.models.CsvData

// Custom marker view class
class CustomMarkerView(context: Context?,
                       layoutResource: Int,
                       private val lineData: LineData,
                       dateTimeFormat: String)
    : MarkerView(context, layoutResource) {
    //private val tvContent: TextView = findViewById(R.id.tvContent)
    //private val binding = CustomMarkerViewBinding.bind(this)
    private val binding: CustomMarkerViewBinding = CustomMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            if(highlight != null) {
                val dataSet = lineData.getDataSetByIndex(highlight.dataSetIndex)
                setBackgroundColor(dataSet.color)

                binding.tvContent.text =
                    String.format(
                        Locale.getDefault(), "%s\n%s\nVal: %.1f",
                        dataSet.label,
                        dateTimeFormatter.format(getDateTime(e.x)),
                        e.y
                    ) // Customize the content displayed in the tooltip
            }
        }
        super.refreshContent(e, highlight)
    }
}

class CustomValueFormatter(dateTimeFormat: String)
    : ValueFormatter() {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)

    override fun getFormattedValue(value: Float): String {
        // Convert float (epoch millis) back to LocalDateTime and format it
        return dateTimeFormatter.format(getDateTime(value))
    }
}

fun LineData?.isSetExistsByLabel(label: String, ignoreCase: Boolean = true) : Boolean{
    this ?: return false // Return false if LineData is null
    return this.getDataSetByLabel(label, ignoreCase) != null
}

fun setChartSettings(context: Context?, chart: LineChart, isDarkTheme: Boolean){

    //chart.setBackgroundColor(Color.BLACK)

    if(isDarkTheme) {
        chart.axisRight.textColor = Color.WHITE
        chart.axisLeft.textColor = Color.WHITE

        chart.xAxis.textColor = Color.WHITE
        chart.legend.textColor = Color.WHITE
    }

    chart.xAxis.valueFormatter = CustomValueFormatter(CsvData.DATE_TIME_CHART_FORMAT)
    val markerView = CustomMarkerView(context, R.layout.custom_marker_view, chart.data, CsvData.DATE_TIME_TOOLTIP_FORMAT)
    chart.marker = markerView
    markerView.chartView = chart // For MPAndroidChart 3.0+
}