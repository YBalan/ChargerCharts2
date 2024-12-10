package com.example.chargercharts2.models

import com.example.chargercharts2.utils.ColorSchema
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CsvDataValue(
    val dateTime: LocalDateTime,
    val voltage: Float,
    val relay: Float,
    var cycle: Cycle? = null,
    var relayDuration: DateTimeRange? = null,
    var visible: Boolean = false,
    var source: String? = null,
    var colorSchema: ColorSchema? = null
){
    fun setCycle(cycle: Cycle?, csvData: CsvData){
        this.cycle = cycle
        if(cycle != null)
            this.cycle?.value = csvData.getValueForCycle(cycle.type)
    }

    fun withColorSchema(colorSchema: ColorSchema) : CsvDataValue{
        this.colorSchema = colorSchema
        return this
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