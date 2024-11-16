package com.example.chargercharts2.utils

import android.annotation.SuppressLint
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
import com.github.mikephil.charting.utils.MPPointF

// Custom marker view class
@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context?,
                       layoutResource: Int,
                       private val lineData: LineData,
                       dateTimeFormat: String,
                       private val customFormatter: ((Any?) -> String?)? = null)
    : MarkerView(context, layoutResource) {

    private val binding: CustomMarkerViewBinding = CustomMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            highlight?.let {
                val dataSet = lineData.getDataSetByIndex(highlight.dataSetIndex)
                if(dataSet.isVisible) {
                    setBackgroundColor(dataSet.color)

                    customFormatter?.invoke(e.data)?.let {
                        binding.tvContent.text = it
                    }

                    if(customFormatter == null)
                    {
                        binding.tvContent.text =
                            String.format(
                                Locale.getDefault(),
                                "%s\n%s\nVal: %.1f",
                                dataSet.label,
                                dateTimeFormatter.format(getDateTime(e.x)),
                                e.y) // Customize the content displayed in the tooltip
                    }
                }
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Position the marker appropriately
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}

class CustomXValueFormatter(dateTimeFormat: String)
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

fun setChartSettings(context: Context?, chart: LineChart, isDarkTheme: Boolean, xAxisFormat: String, toolTipFormat: String, customFormatter: ((Any?) -> String?)? = null){

    //chart.setBackgroundColor(Color.BLACK)

    if(isDarkTheme) {
        chart.axisRight.textColor = Color.WHITE
        chart.axisLeft.textColor = Color.WHITE

        chart.xAxis.textColor = Color.WHITE
        chart.legend.textColor = Color.WHITE
    }

    chart.isAutoScaleMinMaxEnabled = true

    chart.xAxis.valueFormatter = CustomXValueFormatter(xAxisFormat)
    val markerView = CustomMarkerView(context, R.layout.custom_marker_view, chart.data, toolTipFormat, customFormatter)
    chart.marker = markerView
    markerView.chartView = chart // For MPAndroidChart 3.0+
}