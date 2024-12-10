package com.example.chargercharts2.models

import java.time.Duration
import java.time.LocalDateTime

data class DateTimeRange(var start: LocalDateTime, var end: LocalDateTime? = null){
    val duration: Duration
        get() = Duration.between(start, end ?: start)
}