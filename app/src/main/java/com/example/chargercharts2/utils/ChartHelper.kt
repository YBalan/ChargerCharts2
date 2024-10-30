package com.example.chargercharts2.utils

import android.content.Context
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.chargercharts2.databinding.CustomMarkerViewBinding
import com.github.mikephil.charting.formatter.ValueFormatter
import android.view.LayoutInflater

// Custom marker view class
class CustomMarkerView(context: Context, layoutResource: Int, dateTimeFormat: String)
    : MarkerView(context, layoutResource) {
    //private val tvContent: TextView = findViewById(R.id.tvContent)
    //private val binding = CustomMarkerViewBinding.bind(this)
    private val binding: CustomMarkerViewBinding = CustomMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            binding.tvContent.text =
                String.format(
                    Locale.getDefault(), "DT: %s\nVal: %.1f",
                    dateTimeFormatter.format(com.example.chargercharts2.utils.getDateTime(e.x)),
                    e.y
                ) // Customize the content displayed in the tooltip
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