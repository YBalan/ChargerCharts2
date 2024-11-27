package com.example.chargercharts2.analytics

import android.util.Log
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue
import com.example.chargercharts2.models.Cycle
import com.example.chargercharts2.models.CycleType
import com.example.chargercharts2.models.DateTimeRange
import java.time.LocalDateTime
import kotlin.math.abs

object DetectCycles {
    fun analyze(data: CsvData, ignoreZeros: Boolean,
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
            val median = calcMedian(window)
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
                CycleType.Charging -> data.maxV
                CycleType.Discharging -> data.minV
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

    fun analyzeSimple(csvData: CsvData, ignoreZeros: Boolean,
                      windowSize: Int = 7, threshold: Float = 0.11f,
                      showCycleTraces: Boolean = true,
                      useMedian: Boolean = true,
                      fillRelayDuration: Boolean = true){

        if (csvData.values.size < windowSize) return

        val cycles = mutableListOf<Cycle>()
        // Filter values based on ignoreZeros
        val filteredValues = if (ignoreZeros) {
            csvData.values.filter { it.voltage > 0 }
        } else {
            csvData.values
        }

        val firstValue = filteredValues.firstOrNull()
            ?: CsvDataValue(LocalDateTime.now(), 0f, 0f)

        val movingData =  fillMovingData(filteredValues, windowSize, useMedian)
        var lastMovingValue = movingData.firstOrNull() ?: firstValue.voltage
        var lastVoltageValue = firstValue.voltage
        var lastRelayValue = firstValue.relay
        var lastRelayChanged = firstValue.dateTime
        var prevRelayDuration = DateTimeRange(lastRelayChanged)

        //val firstWindow = filteredValues.slice(0 until 0 + windowSize)
        //val firstChange = firstWindow.first().voltage - firstWindow.last().voltage
        //val firstCycleType = if(firstChange <= 0) CycleType.Discharging else CycleType.Charging

        var currentCycle = Cycle(firstValue.dateTime, type = CycleType.Floating)
        for ((index, csvDataValue) in filteredValues.withIndex()) {
            csvDataValue.setCycle(currentCycle, csvData)

            if(fillRelayDuration){
                prevRelayDuration.end = csvDataValue.dateTime
                csvDataValue.relayDuration = prevRelayDuration

                if(csvDataValue.relay != lastRelayValue){
                    prevRelayDuration.end = csvDataValue.dateTime
                    prevRelayDuration = DateTimeRange(csvDataValue.dateTime)
                    csvDataValue.relayDuration = prevRelayDuration
                    lastRelayValue = csvDataValue.relay
                    lastRelayChanged = csvDataValue.dateTime
                }
            }

            //if (index < windowSize - 1) continue
            //val movingValue = movingData[index - windowSize + 1]
            val movingValue = movingData[index]

            val change = movingValue - lastMovingValue

            val currentVoltageValue = csvDataValue.voltage

            val currentCycleType = when {
                change > threshold -> CycleType.Charging
                change < -threshold -> CycleType.Discharging
                //abs(change) <= (threshold - 0.01f) -> CycleType.Floating
                else -> CycleType.Unknown
            }

            if(currentCycleType != CycleType.Unknown &&
                currentCycleType != currentCycle.type){

                currentCycle.end = csvDataValue.dateTime
                cycles.add(currentCycle)

                currentCycle = Cycle(csvDataValue.dateTime, type = currentCycleType)
                csvDataValue.setCycle(currentCycle, csvData)
            }

            lastMovingValue = movingValue
            lastVoltageValue = csvDataValue.voltage
        }

        val lastDateTime = filteredValues.lastOrNull()?.dateTime
        prevRelayDuration.end = lastDateTime
        currentCycle.end = lastDateTime
        if(!cycles.contains(currentCycle))
            cycles.add(currentCycle)

        csvData.cycles.clear()
        csvData.cycles.addAll(cycles)

        if(IS_DEBUG_BUILD && showCycleTraces) {
            for (cycle in csvData.cycles) {
                Log.i(
                    "Analytics",
                    "${cycle.type}: start: ${cycle.start}; end: ${cycle.end}; duration: ${cycle.duration}; valAvg: ${cycle.avgValue} valMed: ${cycle.medValue}"
                )
            }
        }
    }

    fun fillMovingData(csvData: List<CsvDataValue>, windowSize: Int, useMedian: Boolean = true)
        : List<Float>{
        val movingData = mutableListOf<Float>()
        val window = csvData.take(windowSize)
        val movingValue = if (useMedian) calcMedian(window) else calcAverage(window)
        movingData.addAll(window.map { movingValue })

        // Ensure enough data points remain after filtering
        if (csvData.size < windowSize) return movingData

        // Calculate moving median or average for the voltage values
        for (i in 0 until csvData.size - windowSize + 1) {
            val window = csvData.slice(i until i + windowSize)
            val movingValue = if (useMedian) calcMedian(window) else calcAverage(window)
            movingData.add(movingValue)
        }

        return movingData
    }

    private fun calcAverage(window: List<CsvDataValue>): Float = window.map { it.voltage }.average().toFloat()

    private fun calcMedian(window: List<CsvDataValue>): Float =
        window.map { it.voltage }.sorted().let {
        val windowSize = window.size
        if (windowSize % 2 == 0) {
            (it[windowSize / 2 - 1] + it[windowSize / 2]) / 2
        } else {
            it[windowSize / 2]
        }
    }
}