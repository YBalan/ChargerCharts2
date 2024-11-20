package com.example.chargercharts2.analytics

import android.util.Log
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.Cycle
import com.example.chargercharts2.models.CycleType
import kotlin.math.abs

object DetectCycles {
    fun analyze(data: CsvData, ignoreZeros: Boolean,
                     minVoltage: Float, maxVoltage: Float,
                     windowSize: Int = 5, threshold: Float = 0.1f) {
        if (data.values.size < windowSize) return

        val movingMedians = mutableListOf<Float>()
        val cycles = mutableListOf<Cycle>()
        var currentCycleType = CycleType.Unknown
        var cycleStart = data.values.first().dateTime
        var prevCycle = Cycle(cycleStart, cycleStart, currentCycleType)
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
            csvData.cycle = prevCycle
            prevCycle.value = csvData.voltage
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
                    prevCycle.end = cycleEnd
                    prevCycle = Cycle(cycleStart, cycleEnd, currentCycleType)
                    prevCycle.avgValue = avgValue
                    prevCycle.medValue = median
                    cycles.add(prevCycle)
                }
                cycleStart = csvData.dateTime
                currentCycleType = newCycleType
            }

            prevCycle.value = when (prevCycle.type) {
                CycleType.Charging -> maxVoltage
                CycleType.Discharging -> minVoltage
                CycleType.Floating -> prevCycle.avgValue / 2
                else -> csvData.voltage
            }
            csvData.cycle = prevCycle

            lastMedian = median
        }

        // Handle last cycle
        if (currentCycleType != CycleType.Unknown) {
            val lastCycleEnd = filteredValues.last().dateTime
            val lastCycleAvgValue = filteredValues.takeLast(windowSize)
                .map { it.voltage }.average().toFloat()
            cycles.add(Cycle(cycleStart, lastCycleEnd, currentCycleType))
        }

        // Assign detected cycles to the CsvData object
        data.cycles.clear()
        data.cycles.addAll(cycles)

        for(cycle in data.cycles) {
            Log.i("parseCsvFile", "${cycle.type}: start: ${cycle.start}; end: ${cycle.end}; duration: ${cycle.duration}; val: ${cycle.avgValue}")
        }
    }
}