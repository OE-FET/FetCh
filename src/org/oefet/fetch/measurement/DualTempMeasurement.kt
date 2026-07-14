package org.oefet.fetch.measurement

import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.devices.meter.TMeter
import jisa.enums.Icon
import jisa.results.ResultTable
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Measurement class for performing simultaneous temperature measurements with two thermometers
 * after both readings have stabilised.
 */
class DualTempMeasurement : FetChMeasurement("Dual Temperature Measurement", "DualTemp", Icon.THERMOMETER.blackImage) {

    // Parameters
    private val pctMargin by userInput("Temperature Stabilisation", "Stays within [%]", 0.3)
    private val duration  by userTimeInput("Temperature Stabilisation", "For at least", 60000) map { it.toLong() }
    private val maxTime   by userTimeInput("Temperature Stabilisation", "Timeout", 180000) map { it.toLong() }
    private val repTime   by userInput("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats   by userInput("Basic", "No. Repeats", 50)

    // Instruments
    private val tMeter1 by requiredInstrument("Thermometer 1", TMeter::class)
    private val tMeter2 by requiredInstrument("Thermometer 2", TMeter::class)

    // Columns
    companion object : Columns() {
        val TEMPERATURE1                 = decimalColumn("Temperature 1", "K")
        val TEMPERATURE1_STD             = decimalColumn("Temperature 1 Error", "K")
        val TEMPERATURE2                 = decimalColumn("Temperature 2", "K")
        val TEMPERATURE2_STD             = decimalColumn("Temperature 2 Error", "K")
        val TEMPERATURE_DIFFERENCE       = decimalColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_ERROR = decimalColumn("Temperature Difference Error", "K")
    }

    override fun run(results: ResultTable) {

        // Wait for both temperatures to stabilise at the same time.
        runInParallel(
            { tMeter1.waitForStableTemperature(pctMargin, duration, maxTime) },
            { tMeter2.waitForStableTemperature(pctMargin, duration, maxTime) }
        )

        // Run repeat temperature measurements together so both instruments are sampled in the same repeat cycle.
        val tMeter1Values = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
        val tMeter2Values = Repeat.prepare(repeats, repTime) { tMeter2.temperature }

        Repeat.runTogether(tMeter1Values, tMeter2Values)

        val differences = tMeter1Values.values.zip(tMeter2Values.values) { t1, t2 -> t2 - t1 }
        val diffMean    = differences.average()
        val diffStd     = if (differences.size < 2) {
            0.0
        } else {
            sqrt(differences.sumOf { (it - diffMean).pow(2) } / (differences.size - 1))
        }

        // Record results
        results.mapRow(
            TEMPERATURE1                 to tMeter1Values.mean,
            TEMPERATURE1_STD             to tMeter1Values.standardDeviation,
            TEMPERATURE2                 to tMeter2Values.mean,
            TEMPERATURE2_STD             to tMeter2Values.standardDeviation,
            TEMPERATURE_DIFFERENCE       to diffMean,
            TEMPERATURE_DIFFERENCE_ERROR to diffStd
        )

    }

    override fun onFinish() { /* Nothing to do */ }

}
