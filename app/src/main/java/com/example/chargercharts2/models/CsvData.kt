package com.example.chargercharts2.models

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.chargercharts2.utils.chooseValue
import com.example.chargercharts2.utils.setChartSettings
import com.example.chargercharts2.utils.toEpoch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.opencsv.CSVReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlin.math.abs

enum class CycleType
{
    Unknown,
    Charging,
    Discharging,
    Floating,
}

data class CsvDataValue(
    val dateTime: LocalDateTime,
    val voltage: Float,
    val relay: Float,
    var cycleType: CycleType = CycleType.Unknown,
    var cycleValue: Float = 0.0f,
)

data class Cycle(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val type: CycleType,
    val value: Float,
)

data class CsvData(
    var maxV: Float,
    var minV: Float,
    val values: List<CsvDataValue>,
    val cycles: MutableList<Cycle>,
    var voltageLabel: String = "Voltage",
    var voltageVisible: Boolean = true,
    var relayLabel: String = "Relay",
    var relayVisible: Boolean = true,
    var cyclesLabel: String = "Cycles",
    var cyclesVisible: Boolean = true,
) {
    companion object {
        const val DATE_TIME_CHART_FORMAT: String = "MM-dd-yy HH:mm"
        const val DATE_TIME_CSV_FORMAT: String = "uuuu-MM-dd HH:mm:ss"
        const val DATE_TIME_TOOLTIP_FORMAT: String = "HH:mm:ss MM-dd-yy"


        fun parseCsvFile(context: Context, uri: Uri): CsvData {
            val csvValues = mutableListOf<CsvDataValue>()
            val cycles = mutableListOf<Cycle>()
            val csvData = CsvData(0.0f, 0.0f, csvValues, cycles)

            val rows = try {
                // Open input stream from the content resolver
                val inputStream = context.contentResolver.openInputStream(uri) ?: return csvData

                // Read CSV using OpenCSV
                val reader = BufferedReader(InputStreamReader(inputStream))
                val csvReader = CSVReader(reader)
                csvReader.readAll()
            } catch (e: FileSystemException) {
                e.printStackTrace()
                return csvData
            }

            // Skip the header (first row)
            val header = rows.firstOrNull() ?: return csvData
            if (header.count() >= 3) {
                csvData.voltageLabel = readHeader(header[1]) ?: csvData.voltageLabel
                csvData.relayLabel = readHeader(header[2]) ?: csvData.relayLabel
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
                if (firstDigitIdx >= 0)
                    dateTimeString = dateTimeString.substring(firstDigitIdx)
                try {
                    val dateTime = try {
                        LocalDateTime.parse(dateTimeString, dateTimeFormatter)
                    } catch (e: DateTimeParseException) {
                        Log.e("parseCsvFile", "RowIndex: $rowIndex text: '$dateTimeString'")
                        e.printStackTrace()
                        continue
                    }

                    //Log.i("parseCsvFile", "$dateTimeString -> $dateTime")

                    // Parse other columns (assuming integers for Value1 and Value2)
                    val voltage = row[1].trim().toFloat()
                    val relay = row[2].trim().toFloat()

                    csvData.maxV = chooseValue(csvData.maxV < voltage, voltage, csvData.maxV)
                    csvData.minV = chooseValue(
                        csvData.minV == 0.0f || csvData.minV > voltage,
                        voltage,
                        csvData.minV
                    )

                    // Add to the result list
                    csvValues.add(CsvDataValue(dateTime, voltage, relay))
                } catch (e: NumberFormatException) {
                    Log.e("parseCsvFile", "RowIndex: $rowIndex text: '$dateTimeString'")
                    // Handle parsing errors for value1 or value2
                    e.printStackTrace()
                    continue
                }
                rowIndex++
            }

            detectCycles(csvData, ignoreZeros = true, csvData.minV, csvData.maxV)

            return csvData
        }

        fun readHeader(value: String): String? {
            var result: String? = null
            try {
                if (value.isEmpty()) return null
                value.toFloat()
            } catch (_: NumberFormatException) {
                result = value
            }
            return result
        }

        fun plotCsvData(
            context: Context?,
            chart: LineChart,
            data: CsvData,
            ignoreZeros: Boolean,
            isDarkTheme: Boolean
        ): Boolean {
            if (data.values.isEmpty()) return false

            val relayOffset = 0.1f

            val voltage = mutableListOf<Entry>()
            val relay = mutableListOf<Entry>()
            val cycles = mutableListOf<Entry>()

            data.values.forEach { csvData ->
                if (!ignoreZeros || csvData.voltage > 0f) {
                    val dt = csvData.dateTime.toEpoch().toFloat()
                    voltage.add(Entry(dt, csvData.voltage))

                    relay.add(
                        Entry(dt,
                            chooseValue(
                                csvData.relay > 0.0f, data.maxV + relayOffset,
                                chooseValue(data.minV - relayOffset > 0f, data.minV - relayOffset, 0f)
                            )
                        )
                    )
                    cycles.add(Entry(dt, csvData.cycleValue))
                }
            }

            val dataSetVoltage = LineDataSet(voltage, data.voltageLabel)
            dataSetVoltage.isVisible = data.voltageVisible
            dataSetVoltage.color = Color.BLUE
            dataSetVoltage.setCircleColor(Color.BLUE)

            val dataSetRelay = LineDataSet(relay, data.relayLabel)
            dataSetRelay.isVisible = data.relayVisible
            dataSetRelay.color = Color.GRAY
            dataSetRelay.setCircleColor(Color.GRAY)

            val dataSetCycles = LineDataSet(cycles, data.cyclesLabel)
            dataSetCycles.isVisible = data.cyclesVisible
            dataSetCycles.color = Color.RED
            dataSetCycles.setCircleColor(Color.RED)

            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            val desiredLabelsCount = 48
            var granularity =
                ((data.values.last().dateTime.toEpoch() - data.values.first().dateTime.toEpoch()) / desiredLabelsCount).toFloat()

            xAxis.granularity = granularity // in milliseconds
            xAxis.isGranularityEnabled = granularity > 0f
            xAxis.labelRotationAngle = -45f // Rotate labels for better visibility

            //chart.viewPortHandler.setMaximumScaleX(5f) // Allow max zoom level of 5x
            //chart.viewPortHandler.setMinimumScaleX(1f) // Allow minimum zoom level of 1x

            val lineData = LineData(dataSetVoltage, dataSetRelay, dataSetCycles)

            chart.data = lineData
            chart.description.isEnabled = false

            setChartSettings(context, chart, isDarkTheme)

            chart.invalidate() // Refresh the chart

            return true
        }

        fun detectCycles(data: CsvData, ignoreZeros: Boolean, minVoltage: Float, maxVoltage: Float, windowSize: Int = 10, threshold: Float = 0.1f) {
            if (data.values.size < windowSize) return

            val movingMedians = mutableListOf<Float>()
            val cycles = mutableListOf<Cycle>()
            var currentCycleType = CycleType.Unknown
            var cycleStart = data.values.first().dateTime
            var lastMedian = 0f

            // Filter values based on ignoreZeros
            val filteredValues = if (ignoreZeros) {
                data.values.filter { it.voltage > 0 }
            } else {
                data.values
            }

            // Ensure enough data points remain after filtering
            if (filteredValues.size < windowSize) return

            // Calculate moving median for the voltage values
            for (i in 0 until filteredValues.size - windowSize + 1) {
                val window = filteredValues.slice(i until i + windowSize)
                val median = window.map { it.voltage }.sorted().let {
                    if (windowSize % 2 == 0) {
                        (it[windowSize / 2 - 1] + it[windowSize / 2]) / 2
                    } else {
                        it[windowSize / 2]
                    }
                }
                movingMedians.add(median)
            }

            // Detect cycles based on changes in the moving median
            for ((index, csvData) in filteredValues.withIndex()) {
                csvData.cycleValue = csvData.voltage
                if (index < windowSize - 1) continue
                val median = movingMedians[index - windowSize + 1]

                val change = median - lastMedian
                val newCycleType = when {
                    change > threshold -> CycleType.Charging
                    change < -threshold -> CycleType.Discharging
                    abs(change) <= threshold -> CycleType.Floating
                    else -> CycleType.Unknown
                }

                // If cycle type changes, record the previous cycle and start a new one
                if (newCycleType != currentCycleType) {
                    if (currentCycleType != CycleType.Unknown) {
                        val cycleEnd = filteredValues[index - 1].dateTime
                        val avgValue = filteredValues.slice(index - windowSize + 1 until index)
                            .map { it.voltage }.average().toFloat()
                        cycles.add(Cycle(cycleStart, cycleEnd, currentCycleType, avgValue))
                    }
                    cycleStart = csvData.dateTime
                    currentCycleType = newCycleType
                }

                csvData.cycleType = currentCycleType
                csvData.cycleValue = chooseValue(
                    currentCycleType == CycleType.Charging, maxVoltage, minVoltage)
                csvData.cycleValue = chooseValue(
                    currentCycleType == CycleType.Unknown, csvData.voltage, csvData.cycleValue)
                csvData.cycleValue = chooseValue(csvData.cycleValue == 0f,
                    csvData.voltage, csvData.cycleValue)

                lastMedian = median
            }

            // Handle last cycle
            if (currentCycleType != CycleType.Unknown) {
                val lastCycleEnd = filteredValues.last().dateTime
                val lastCycleAvgValue = filteredValues.takeLast(windowSize)
                    .map { it.voltage }.average().toFloat()
                cycles.add(Cycle(cycleStart, lastCycleEnd, currentCycleType, lastCycleAvgValue))
            }

            // Assign detected cycles to the CsvData object
            data.cycles.clear()
            data.cycles.addAll(cycles)
        }
    }
}