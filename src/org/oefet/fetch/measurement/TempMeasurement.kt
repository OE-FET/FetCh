package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.meter.TMeter
import jisa.enums.Icon
import jisa.results.ResultTable

/**
 * Measurement class for performing temperature measurements after a stabilisation period.
 */
class TempMeasurement : FetChMeasurement("Temperature Measurement", "Temp", Icon.THERMOMETER.blackImage) {

    // Parameters
    private val pctMargin by userInput("Temperature Stabilisation", "Stays within [%]", 0.3)
    private val duration  by userTimeInput("Temperature Stabilisation", "For at least", 60000)
    private val maxTime   by userTimeInput("Temperature Stabilisation", "Timeout", 180000)
    private val repTime   by userInput("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats   by userInput("Basic", "No. Repeats", 50)

    // Instruments
    private val tMeter by requiredInstrument("Thermometer", TMeter::class)

    // Columns
    companion object : Columns() {
        val TEMPERATURE     = decimalColumn("Temperature", "K")
        val TEMPERATURE_STD = decimalColumn("Temperature Error", "K")
    }

    override fun run(results: ResultTable) {

        // Wait for temperature to stabilise
        tMeter.waitForStableTemperature(pctMargin, duration.toLong(), maxTime.toLong())

        // Run repeat temperature measurements
        val tMeterValues = Repeat.run(repeats, repTime) { tMeter.temperature }

        // Record results
        results.mapRow(
            TEMPERATURE     to tMeterValues.mean,
            TEMPERATURE_STD to tMeterValues.standardDeviation
        )

    }

    override fun onFinish() { /* Nothing to do */ }

}

