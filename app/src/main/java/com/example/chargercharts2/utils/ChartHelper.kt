package com.example.chargercharts2.utils

import android.content.Context
import android.widget.TextView
import com.example.chargercharts2.models.CsvData
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.time.format.DateTimeFormatter
import java.util.Locale

// Custom marker view class
private class CustomMarkerView(context: Context, layoutResource: Int, tvContent1: TextView) : MarkerView(context, layoutResource) {
    //private val tvContent: TextView = findViewById(R.id.tvContent)
    private val tvContent: TextView = tvContent1
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(CsvData.dateTimeToolTipFormat)

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        tvContent.text =
            String.format(Locale.getDefault(), "DT: %s\nVal: %.1f",
                dateTimeFormatter.format(com.example.chargercharts2.utils.getDateTime(e.x)),
                e.y
            ) // Customize the content displayed in the tooltip
        super.refreshContent(e, highlight)
    }
}