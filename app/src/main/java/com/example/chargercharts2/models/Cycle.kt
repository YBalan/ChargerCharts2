package com.example.chargercharts2.models

import java.time.Duration
import java.time.LocalDateTime

enum class CycleType
{
    Unknown,
    Charging,
    Discharging,
    Floating,
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