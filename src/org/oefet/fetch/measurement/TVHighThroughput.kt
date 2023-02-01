package org.oefet.fetch.measurement

import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TC
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVHTPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TVHighThroughputResult
import kotlin.math.pow
import kotlin.math.sqrt

class TVHighThroughput : FetChMeasurement("Thermal Voltage High Throughput", "TVThroughput", "Thermal Voltage High Throughput", Images.getImage("fire.png")) {

    private val repTime     by userTimeInput("Basic", "Repeat Time", 0)
    private val repeats     by userInput("Basic", "Repeats", 50)
    private val pctStabPelt by userInput("Peltier Stabilisation", "Stays within [%]", 0.3)
    private val durStabPelt by userTimeInput("Peltier Stabilisation", "For at least", 60000) map { it.toLong() }
    private val tmoStabPelt by userTimeInput("Peltier Stabilisation", "Timeout", 180000) map { it.toLong() }
    private val pctStabChip by userInput("Chip Stabilisation", "Stays within [%]", 1.0)
    private val durStabChip by userTimeInput("Chip Stabilisation", "For at least", 10000) map { it.toLong() }
    private val tmoStabChip by userTimeInput("Chip Stabilisation", "Timeout", 180000) map { it.toLong() }
    private val coldTemps   by userInput("Temperature", "Cold Side [K]", Range.linear(295.15, 274, 3))
    private val hotTemps    by userInput("Temperature", "Hot Side [K]", Range.linear(295.15, 312, 3))

    // Instruments
    private val vMeter1     by requiredInstrument("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2     by optionalInstrument("Thermal Voltage Meter 2", VMeter::class)
    private val ground      by optionalInstrument("Ground", SMU::class)
    private val tMeter1     by requiredInstrument("Thermometer Cold", TMeter::class)
    private val tMeter2     by requiredInstrument("Thermometer Hot", TMeter::class)
    private val hotPeltier  by requiredInstrument("Hot Peltier", TC.Loop::class)
    private val coldPeltier by requiredInstrument("Cold Peltier", TC.Loop::class)


    // Data columns
    companion object : Columns() {

        val VOLTAGE                      = decimalColumn("Voltage", "V")
        val VOLTAGE_ERROR                = decimalColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1                 = decimalColumn("Temperature 1", "K")
        val TEMPERATURE2                 = decimalColumn("Temperature 2", "K")
        val TEMPERATURE_DIFFERENCE       = decimalColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_ERROR = decimalColumn("Temperature Difference Error", "K")

    }

    override fun processResults(data: ResultTable): TVHighThroughputResult {
        return TVHighThroughputResult(data)
    }

    override fun run(results: ResultTable) {

        if (coldTemps.size() != hotTemps.size()) {
            throw Exception("Temperature Arrays have different size")
        }

        // Record measurement parameters into result file
        results.setAttribute("Cold Temperatures", "$coldTemps K")
        results.setAttribute("Hot Temperatures", "$hotTemps K")

        // Start with everything turned off
        vMeter1.turnOff()
        vMeter2?.turnOff()
        ground?.turnOff()
        print("turn off")

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter1.averageMode  = AMode.NONE
        vMeter2?.averageMode = AMode.NONE
        ground?.voltage      = 0.0

        vMeter1.turnOn()
        vMeter2?.turnOn()
        ground?.turnOn()

        val determineVoltage: (Double, Double, Double) -> Double = when {
            vMeter2 != null -> { v1, v2, _ -> v2 - v1 }
            ground != null  -> { v1, _, gd -> v1 - gd }
            else            -> { v1, _, _ -> v1 }
        }

        val determineError: (Double, Double, Double) -> Double = when {
            vMeter2 != null -> { v1, v2, _ -> sqrt(v1.pow(2) + v2.pow(2)) }
            ground != null  -> { v1, _, gd -> sqrt(v1.pow(2) + gd.pow(2)) }
            else            -> { v1, _, _  -> v1 }
        }

        for ((tHot, tCold) in coldTemps.zip(hotTemps)) {

            coldPeltier.setPoint     = tCold
            hotPeltier.setPoint      = tHot
            coldPeltier.isPIDEnabled = true
            hotPeltier.isPIDEnabled  = true

            runInParallel(
                { coldPeltier.waitForStableTemperature(tCold, pctStabPelt, durStabPelt) },
                { hotPeltier.waitForStableTemperature(tHot, pctStabPelt, durStabPelt) },
                { tMeter1.waitForStableTemperatureMaxTime(pctStabChip, durStabChip, tmoStabChip) },
                { tMeter2.waitForStableTemperatureMaxTime(pctStabChip, durStabChip, tmoStabChip) }
            )

            val v1 = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
            val v2 = Repeat.prepare(repeats, repTime) { vMeter2?.voltage ?: Double.NaN }
            val t1 = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
            val t2 = Repeat.prepare(repeats, repTime) { tMeter2.temperature }
            val tD = Repeat.prepare(repeats, repTime) { tMeter2.temperature - tMeter1.temperature }

            Repeat.runTogether(v1, v2, t1, t2, tD)

            results.mapRow(
                VOLTAGE                      to determineVoltage(v1.mean, v2.mean, ground?.voltage ?: Double.NaN),
                VOLTAGE_ERROR                to determineError(v1.standardDeviation, v2.standardDeviation, ground?.voltage ?: Double.NaN),
                TEMPERATURE1                 to t1.mean,
                TEMPERATURE2                 to t2.mean,
                TEMPERATURE_DIFFERENCE       to tD.mean,
                TEMPERATURE_DIFFERENCE_ERROR to tD.standardDeviation
            )

        }


    }

    override fun onFinish() {

        runRegardless(
            { vMeter1.turnOff() },
            { vMeter2?.turnOff() },
            { ground?.turnOff() },
            { hotPeltier.manualValue  = 0.0},
            { hotPeltier.isPIDEnabled = false },
            { coldPeltier.manualValue = 0.0},
            { hotPeltier.isPIDEnabled = false }
        )

    }

    override fun createDisplay(data: ResultTable): TVHTPlot {
        return TVHTPlot(data)
    }

}