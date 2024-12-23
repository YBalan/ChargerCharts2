package com.example.chargercharts2.models

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.chargercharts2.utils.getFileName
import com.opencsv.CSVReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

enum class CycleType
{
    Unknown,
    Charging,
    Discharging,
    Floating,
}

data class DateTimeRange(var start: LocalDateTime, var end: LocalDateTime? = null){
    val duration: Duration
        get() = Duration.between(start, end ?: start)
}

data class CsvDataValue(
    val dateTime: LocalDateTime,
    val voltage: Float,
    val relay: Float,
    var cycle: Cycle? = null,
    var relayDuration: DateTimeRange? = null,
    var visible: Boolean = false,
    var source: String? = null,
){
    fun setCycle(cycle: Cycle?, csvData: CsvData){
        this.cycle = cycle
        if(cycle != null)
            this.cycle?.value = csvData.getValueForCycle(cycle.type)
    }

    override fun toString() : String{
        return "${DateTimeFormatter.ofPattern(CsvData.DATE_TIME_CSV_VALUE_FORMAT).format(dateTime)} [${"%.1f".format(voltage)}V]"
    }

    companion object {
        fun highlightValueFormatter(data: Any?, dataSetName: String?): String? {
            val dtFormatter = DateTimeFormatter.ofPattern(CsvData.DATE_TIME_TOOLTIP_FORMAT)
            return (data as? CsvDataValue)?.let {
                (if(dataSetName.isNullOrEmpty()) "" else dataSetName + "\n") +
                (if(it.source.isNullOrEmpty()) "" else it.source + "\n") +
                "Date: ${dtFormatter.format(it.dateTime)}"+
                "\nVoltage: ${it.voltage}"+
                "\nRelay: ${if(it.relay == 0f) "Off" else "On"}" +
                (if(it.relayDuration != null) " (${it.relayDuration?.duration.toString(false, true)})" else "")+
                (if(it.cycle != null) "\nCycle: ${it.cycle?.type}" else "") +
                (if(it.cycle?.duration != null) "\nDuration: ${it.cycle?.duration.toString(false, false)}" else "")
            }
        }

        fun Duration?.toString(showSeconds: Boolean, showDays: Boolean): String {
            if (this == null || this == Duration.ZERO) return "N/A"

            val days = this.toDays()
            val hours = if(showDays) this.toHours() % 24 else this.toHours()
            val minutes = this.toMinutes() % 60
            val seconds = this.seconds % 60

            return buildString {
                if (showDays && days > 0) append("$days day${if (days > 1) "s" else ""} ")
                append("%02d:".format(hours))
                append("%02d".format(minutes))
                if (showSeconds || minutes == 0L) {
                    append(":%02ds".format(seconds))
                }
            }.trim()
        }
    }
}

data class Cycle(
    val start: LocalDateTime,
    var end: LocalDateTime? = null,
    val type: CycleType = CycleType.Unknown,
){
    var value: Float? = null
    var avgValue: Float = 0f
    var medValue: Float = 0f
    val duration: Duration
        get() = Duration.between(start, end ?: start)
}

data class CsvData(
    var maxV: Float = 0.0f,
    var minV: Float = 0.0f,
    val values: MutableList<CsvDataValue> = mutableListOf(),
    val cycles: MutableList<Cycle> = mutableListOf(),

    var voltageLabel: String = "Voltage",
    var voltageVisible: Boolean = true,
    var voltageColor: Int = Color.BLUE,

    var relayLabel: String = "Relay",
    var relayVisible: Boolean = true,
    var relayColor: Int = Color.GRAY,
    var relayVOffset: Float = 0.1f,

    var cyclesLabel: String = "Cycles",
    var cyclesVisible: Boolean = false,
    var cyclesColor: Int = Color.RED,
    var cycleVOffset: Float = 0.2f,

    var source: String = "",
) {
    fun clear(){
        minV = 0.0f
        maxV = 0.0f
        values.clear()
        cycles.clear()
    }

    fun getYRightValue(value: Float, shortValue: Boolean) : String? {
        return when (value) {
            getValueForRelayOff(value) -> "Off"
            getValueForRelayOn(value) -> "On"
            getValueForCycleDischarging() -> if(shortValue) "-" else CycleType.Discharging.toString()
            getValueForCycleCharging() -> if(shortValue) "+" else CycleType.Charging.toString()
            else -> null
        }
    }

    fun getValueForRelay(value: Float) : Float {
        return if (value > 0.0f) getValueForRelayOn(value) else getValueForRelayOff(value)
    }

    fun getValueForRelayOn(value: Float) : Float{
        return maxV + relayVOffset
    }

    fun getValueForRelayOff(value: Float) : Float{
        return if (minV - relayVOffset < 0f) 0f else minV - relayVOffset
    }

    fun getValueForCycle(cycleType: CycleType) : Float? {
        return when (cycleType) {
            CycleType.Charging -> getValueForCycleCharging()
            CycleType.Discharging -> getValueForCycleDischarging()
            else -> null
        }
    }

    fun getValueForCycleCharging() : Float{
        return maxV + cycleVOffset
    }

    fun getValueForCycleDischarging() : Float {
        return if (minV - cycleVOffset < 0f) 0f else minV - cycleVOffset
    }

    fun addValue(csvValue: CsvDataValue){
        maxV = if(maxV < csvValue.voltage) csvValue.voltage else maxV
        minV = if(minV == 0.0f || minV > csvValue.voltage) csvValue.voltage else minV

        values.add(csvValue)
    }

    companion object {
        const val DATE_TIME_CSV_CHART_FORMAT: String = "MM-dd-yy HH:mm"
        const val DATE_TIME_UDP_CHART_FORMAT: String = "HH:mm"
        const val DATE_TIME_CSV_VALUE_FORMAT: String = "HH:mm:ss"
        const val DATE_TIME_CSV_FORMAT: String = "uuuu-MM-dd HH:mm:ss"
        const val DATE_TIME_TOOLTIP_FORMAT: String = "HH:mm:ss MM-dd-yy"

        fun parseCsvFiles(context: Context, uris: List<Uri?>): CsvData {
            val csvData = CsvData()
            csvData.source = if(uris.count() > 1) getMultiSourceName(context, uris) else uris.firstOrNull().getFileName(context) ?: "N/A"
            uris.forEach{uri ->
                parseCsvFile(context, uri, fillValueSource = uris.count() > 1).values.forEach { csvValue ->
                    csvData.addValue(csvValue)
                }
            }
            return csvData
        }

        data class FileInfo(val key: String, val name: String, val dt: String)
        private fun getMultiSourceName(context: Context, uris: List<Uri?>): String {
            val grouped = uris.mapNotNull { uri ->
                uri.getFileName(context)?.let { fileName ->
                    val dt = fileName.substringAfter('_').split("(", ".")[0]
                    val source = fileName.substringBefore('_')
                    FileInfo(source.lowercase(), source, dt)
                }
            }.groupBy { it.key }

            val dateTimes = grouped.flatMap { gr ->
                gr.value.filter { i -> i.dt.isNotEmpty() }.map { it.dt }
            }
            var dts = if (dateTimes.isNotEmpty()) "_${dateTimes.min()}_${dateTimes.max()}" else ""
            dts = if(dateTimes.count() == 1) "_${dateTimes.min()}" else dts
            return grouped.map { it.value.first().name }.joinToString(", ") + dts
        }

        fun parseCsvFile(context: Context, uri: Uri?, fillValueSource: Boolean): CsvData {
            val csvData = CsvData()
            if(uri == null) return csvData
            csvData.source = uri.getFileName(context).toString()

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
            val dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_CSV_FORMAT)
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

                    // Add to the result list
                    csvData.addValue(CsvDataValue(dateTime, voltage, relay, source = if(fillValueSource) csvData.source else null))
                } catch (e: NumberFormatException) {
                    Log.e("parseCsvFile", "RowIndex: $rowIndex text: '$dateTimeString'")
                    // Handle parsing errors for value1 or value2
                    e.printStackTrace()
                    continue
                }
                rowIndex++
            }

            return csvData
        }

        private fun readHeader(value: String): String? {
            var result: String? = null
            try {
                if (value.isEmpty()) return null
                value.toFloat()
            } catch (_: NumberFormatException) {
                result = value
            }
            return result
        }
    }
}