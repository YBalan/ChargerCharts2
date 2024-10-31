package com.example.chargercharts2.models

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.chargercharts2.R
import com.example.chargercharts2.utils.CustomMarkerView
import com.example.chargercharts2.utils.getDateTime
import com.example.chargercharts2.utils.setChartSettings
import com.example.chargercharts2.utils.toEpoch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.opencsv.CSVReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

data class CsvData(
    var maxV: Float,
    var minV: Float,
    val values: List<CsvDataValue>,
    var voltageLabel: String = "Voltage",
    var voltageVisible: Boolean = true,
    var relayLabel: String = "Relay",
    var relayVisible: Boolean = true,
){
    companion object {
        const val DATE_TIME_CHART_FORMAT: String = "MM-dd-yy HH:mm"
        const val DATE_TIME_CSV_FORMAT: String = "uuuu-MM-dd HH:mm:ss"
        const val DATE_TIME_TOOLTIP_FORMAT: String = "HH:mm:ss MM-dd-yy"
    }
}

data class CsvDataValue(
    val dateTime: LocalDateTime,
    val voltage: Float,
    val relay: Float,
)

val chooseValue: (Boolean, Float, Float) -> Float = { condition, trueValue, falseValue ->
    if (condition) trueValue else falseValue
}

val chooseValueInt: (Boolean, Int, Int) -> Int = { condition, trueValue, falseValue ->
    if (condition) trueValue else falseValue
}

fun parseCsvFile(context: Context, uri: Uri): CsvData {
    val resultList = mutableListOf<CsvDataValue>()
    val result = CsvData(0.0f, 0.0f, resultList)

    val rows = try {
        // Open input stream from the content resolver
        val inputStream = context.contentResolver.openInputStream(uri) ?: return result

        // Read CSV using OpenCSV
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvReader = CSVReader(reader)
        csvReader.readAll()
    }catch (e: FileSystemException){
        e.printStackTrace()
        return result
    }

    // Skip the header (first row)
    val header = rows.firstOrNull() ?: return result
    if(header.count() >= 3){
        result.voltageLabel = readHeader(header[1]) ?: result.voltageLabel
        result.relayLabel = readHeader(header[2]) ?: result.relayLabel
    }

    // Set the expected DateTime format
    val dateTimeFormatter = DateTimeFormatter.ofPattern(CsvData.DATE_TIME_CSV_FORMAT)
        .withResolverStyle(ResolverStyle.STRICT)

    var rowIndex = 1
    // Iterate over the CSV rows, starting from the second row
    for (row in rows.drop(rowIndex)) {
        // Parse DateTime from the first column
        var dateTimeString = row[0].trim()
        val firstDigitIdx = dateTimeString.indexOfFirst { c -> c.isDigit() }
        if(firstDigitIdx >= 0)
            dateTimeString = dateTimeString.substring(firstDigitIdx)
        try {
            val dateTime = try { LocalDateTime.parse(dateTimeString, dateTimeFormatter)
            }catch (e: DateTimeParseException) {
                Log.e("parseCsvFile", "RowIndex: $rowIndex text: '$dateTimeString'")
                e.printStackTrace()
                continue
            }

            //Log.i("parseCsvFile", "$dateTimeString -> $dateTime")

            // Parse other columns (assuming integers for Value1 and Value2)
            val voltage = row[1].trim().toFloat()
            val relay = row[2].trim().toFloat()

            result.maxV = chooseValue(result.maxV < voltage, voltage, result.maxV)
            result.minV = chooseValue(result.minV == 0.0f || result.minV > voltage, voltage, result.minV)

            // Add to the result list
            resultList.add(CsvDataValue(dateTime, voltage, relay))
        }
        catch (e: NumberFormatException) {
            Log.e("parseCsvFile", "RowIndex: $rowIndex text: '$dateTimeString'")
            // Handle parsing errors for value1 or value2
            e.printStackTrace()
            continue
        }
        rowIndex++
    }

    return result
}

fun readHeader(value: String) : String?{
    var result: String? = null
    try{
        if (value.isEmpty()) return null
        value.toFloat()
    }
    catch (_: NumberFormatException){
        result = value
    }
    return result
}

fun plotCsvData(context: Context?, chart: LineChart, data: CsvData) : Boolean {
    if(data.values.isEmpty()) return false

    val voltage = data.values.map { csvData ->
        Entry(csvData.dateTime.toEpoch().toFloat(), csvData.voltage) // X = epoch millis, Y = voltage
    }

    val relayOffset = 0.1f
    val relay = data.values.map { csvData ->
        Entry(csvData.dateTime.toEpoch().toFloat(),
            chooseValue(csvData.relay > 0.0f, data.maxV + relayOffset,
                chooseValue(data.minV - relayOffset > 0f, data.minV - relayOffset, 0f))) // X = epoch millis, Y = relay
    }

    val dataSetVoltage = LineDataSet(voltage, data.voltageLabel)
    dataSetVoltage.isVisible = data.voltageVisible
    dataSetVoltage.color = Color.BLUE
    dataSetVoltage.setCircleColor(Color.BLUE)

    val dataSetRelay = LineDataSet(relay, data.relayLabel)
    dataSetRelay.isVisible = data.relayVisible
    dataSetRelay.color = Color.GRAY
    dataSetRelay.setCircleColor(Color.GRAY)

    val xAxis = chart.xAxis
    xAxis.position = XAxis.XAxisPosition.BOTTOM

    val desiredLabelsCount = 48
    var granularity =
        ((data.values.last().dateTime.toEpoch() - data.values.first().dateTime.toEpoch()) / desiredLabelsCount).toFloat()

    xAxis.granularity = granularity // in milliseconds
    xAxis.isGranularityEnabled = granularity > 0f
    xAxis.labelRotationAngle = -45f // Rotate labels for better visibility

    chart.viewPortHandler.setMaximumScaleX(5f) // Allow max zoom level of 5x
    chart.viewPortHandler.setMinimumScaleX(1f) // Allow minimum zoom level of 1x

    val lineData = LineData(dataSetVoltage, dataSetRelay)

    chart.data = lineData
    chart.description.isEnabled = false

    setChartSettings(context, chart)

    chart.invalidate() // Refresh the chart

    return true
}