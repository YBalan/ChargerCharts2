package com.example.chargercharts2.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


fun getDateTime(seconds: Float): LocalDateTime {
    return LocalDateTime.ofEpochSecond(seconds.toLong(), 0, ZoneOffset.UTC)
}

// Convert LocalDateTime to epoch milliseconds
fun LocalDateTime.toEpoch(): Long {
    return this.atZone(ZoneOffset.UTC).toInstant().epochSecond
}

fun getDefaultZoneOffset(): ZoneOffset {
    return ZonedDateTime.now(ZoneId.systemDefault()).offset
}

