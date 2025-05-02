package org.oefet.fetch.measurement

import jisa.devices.meter.VMeter
import jisa.enums.Icon
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class VMeasure : FetChMeasurement("Voltage Measurement", "Voltage", "VMeasure", Icon.VOLTMETER.blackImage) {

    // The user must specify a total time, number of intervals, and provide a voltmeter to use
    val totalTime    by userTimeInput("Timing", "Total Time", 10000)
    val numIntervals by userInput("Timing", "No. Measurements", 11)
    val vMeter       by requiredInstrument("Voltmeter", VMeter::class)

    companion object : Columns() {

        val TIME    = decimalColumn("Time", "s")
        val VOLTAGE = decimalColumn("Voltage", "V")

    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        return FetChPlot("Voltage Measurement").apply {

            createSeries().watch(data, TIME, VOLTAGE)
            isLegendVisible = false
            isMouseEnabled  = true

        }

    }

    override fun run(results: ResultTable) {

        // Make sure the voltmeter is on
        vMeter.turnOn()

        // Work out the sleep interval
        val interval = totalTime / (numIntervals - 1)

        // Loop over all measurement intervals
        for (time in Range.step(0, totalTime, interval)) {

            results.mapRow(
                TIME    to (time / 1e3),     // Record time in seconds
                VOLTAGE to vMeter.voltage    // Measure and record voltage
            )

            // Wait the interval time before doing the next iteration (unless this is the last iteration)
            if (time < totalTime) {
                sleep(interval)
            }

        }

    }

    override fun onFinish() {
        // Turn off the voltmeter on finished/interrupt/error
        runRegardless { vMeter.turnOff() }
    }

}