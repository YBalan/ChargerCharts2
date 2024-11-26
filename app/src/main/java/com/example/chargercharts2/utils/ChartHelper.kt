package com.example.chargercharts2.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.chargercharts2.databinding.CustomMarkerViewBinding
import com.github.mikephil.charting.formatter.ValueFormatter
import android.view.LayoutInflater
import com.example.chargercharts2.models.CsvData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF

data class ColorSchema(
    val voltageColor: Int,
    val relayColor: Int,
    val cyclesColor: Int,)

val UDPSenderColorSchemasDark = listOf<ColorSchema>(
    ColorSchema(Color.RED, Color.GRAY, Color.WHITE),
    ColorSchema(Color.BLUE, Color.GRAY, Color.WHITE),
    ColorSchema(Color.GREEN,Color.GRAY, Color.WHITE),
    ColorSchema(Color.MAGENTA,Color.GRAY, Color.WHITE),
    ColorSchema(Color.CYAN,Color.GRAY, Color.WHITE),
    ColorSchema(Color.YELLOW,Color.GRAY, Color.WHITE),
    ColorSchema(Color.DKGRAY,Color.GRAY, Color.WHITE),
    ColorSchema(Color.LTGRAY, Color.GRAY, Color.WHITE),
)
val UDPSenderColorSchemasWhite = listOf<ColorSchema>(
    ColorSchema(Color.RED, Color.GRAY, Color.RED),
    ColorSchema(Color.BLUE, Color.GRAY, Color.RED),
    ColorSchema(Color.GREEN,Color.GRAY, Color.RED),
    ColorSchema(Color.MAGENTA,Color.GRAY, Color.RED),
    ColorSchema(Color.CYAN,Color.GRAY, Color.RED),
    ColorSchema(Color.YELLOW,Color.GRAY, Color.RED),
    ColorSchema(Color.DKGRAY,Color.GRAY, Color.RED),
    ColorSchema(Color.LTGRAY, Color.GRAY, Color.RED),
)

fun getColorSchema(setNumber: Int, isDarkTheme: Boolean): ColorSchema{
    return if(isDarkTheme)
        UDPSenderColorSchemasDark[setNumber % UDPSenderColorSchemasDark.size]
    else UDPSenderColorSchemasWhite[setNumber % UDPSenderColorSchemasWhite.size]
}

// Custom marker view class
@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context?,
                       layoutResource: Int,
                       private val lineData: LineData,
                       dateTimeFormat: String,
                       private val customFormatter: ((Any?, ILineDataSet?) -> String?)? = null)
    : MarkerView(context, layoutResource) {

    private val binding: CustomMarkerViewBinding = CustomMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            highlight?.let {
                val dataSet = lineData.getDataSetByIndex(highlight.dataSetIndex)
                if (dataSet.isVisible) {
                    setBackgroundColor(dataSet.color)

                    val content = customFormatter?.invoke(e.data, dataSet)
                    binding.tvContent.text = content

                    if (content.isNullOrEmpty()) {
                        binding.tvContent.text =
                            String.format(
                                Locale.getDefault(),
                                "%s\n%s\nVal: %.1f",
                                dataSet.label,
                                dateTimeFormatter.format(getDateTime(e.x)),
                                e.y
                            ) // Customize the content displayed in the tooltip
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

class CustomYRightValueFormatter(val csvData: CsvData?)
    : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return csvData?.getYRightValue(value) ?: "%.2f".format(value)
    }
}



fun LineData?.isSetExistsByLabel(label: String, ignoreCase: Boolean = true) : Boolean{
    this ?: return false // Return false if LineData is null
    return this.getDataSetByLabel(label, ignoreCase) != null
}

fun LineChart.hideHighlight(){
    this.highlightValue(null)
}

fun LineChart.recalculateYAxis(margin: Float = 0.1f) {
    val allVisibleEntries = this.data.dataSets
        .filter { it.isVisible } // Include only visible data sets
        .flatMap  { dataSet ->
            (0 until dataSet.entryCount).map { dataSet.getEntryForIndex(it) }
        } // Collect all entries from visible data sets

    if (allVisibleEntries.isNotEmpty()) {
        val minY = allVisibleEntries.minOf { it.y }
        val maxY = allVisibleEntries.maxOf { it.y }

        this.axisLeft.axisMinimum = if(minY - margin < 0f) 0f else minY - margin
        this.axisLeft.axisMaximum = maxY + margin

        this.axisRight.axisMinimum = if(minY - margin < 0f) 0f else minY - margin
        this.axisRight.axisMaximum = maxY + margin
    } else {
        // Reset axis if no data is visible
        this.axisLeft.resetAxisMaximum()
        this.axisLeft.resetAxisMinimum()

        this.axisRight.resetAxisMaximum()
        this.axisRight.resetAxisMinimum()
    }
}