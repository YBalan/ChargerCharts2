package com.example.chargercharts2.models

import java.time.LocalDateTime

data class CsvData(
    var maxV: Float,
    var minV: Float,
    val values: List<CsvDataValues>,
    var voltageLabel: String = "Voltage",
    var voltageVisible: Boolean = true,
    var relayLabel: String = "Relay",
    var relayVisible: Boolean = true,
){
    companion object {
        const val dateTimeChartFormat: String = "MM-dd-yy HH:mm"
        const val dateTimeCsvFormat: String = "uuuu-MM-dd HH:mm:ss"
        const val dateTimeToolTipFormat: String = "HH:mm:ss MM-dd-yy"
    }
}

data class CsvDataValues(
    val dateTime: LocalDateTime,
    val voltage: Float,
    val relay: Float,
)