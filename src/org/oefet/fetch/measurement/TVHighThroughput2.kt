package org.oefet.fetch.measurement

import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.devices.meter.TMeter
import jisa.devices.meter.VMeter
import jisa.devices.pid.TC
import jisa.devices.smu.SMU
import jisa.enums.AMode
import jisa.gui.Element
import jisa.maths.Range
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVHTPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TVHighThroughputResult
import kotlin.math.pow
import kotlin.math.sqrt

class TVHighThroughput2 : FetChMeasurement("Thermal Voltage High Throughput But Better", "TVThroughput", "Thermal Voltage High Throughput", Images.getImage("fire.png")) {

    private val repTime     by userTimeInput("Basic", "Repeat Time", 0)
    private val spacing     by userInput("Basic", "Thermometer Spacing [m]", 1e-2)
    private val length      by userInput("Basic", "Electrode Distance [m]", 1e-3)
    private val repeats     by userInput("Basic", "Repeats", 50)
    private val controlModeInput = userChoice("Control Mode", "Mode", "Temperature PID", "Constant Current")
    private val controlMode       by controlModeInput
    private val pctStabPelt by userInput("Temperature Control", "Stays within [%]", 0.3).visibleIf(controlModeInput, 0)
    private val durStabPelt by userTimeInput("Temperature Control", "For at least", 60000).visibleIf(controlModeInput, 0) map { it.toLong() }
    private val tmoStabPelt by userTimeInput("Temperature Control", "Timeout", 180000).visibleIf(controlModeInput, 0) map { it.toLong() }
    private val pctStabChip by userInput("Chip Stabilisation", "Stays within [%]", 1.0)
    private val durStabChip by userTimeInput("Chip Stabilisation", "For at least", 10000) map { it.toLong() }
    private val tmoStabChip by userTimeInput("Chip Stabilisation", "Timeout", 180000) map { it.toLong() }
    private val coldTemps   by userInput("Temperature Control", "Cold Side [K]", Range.linear(295.15, 274, 3)).visibleIf(controlModeInput, 0)
    private val coldOff      by userInput("Temperature Control", "Auto-Off Cold Side", true).visibleIf(controlModeInput, 0)
    private val hotTemps    by userInput("Temperature Control", "Hot Side [K]", Range.linear(295.15, 312, 3)).visibleIf(controlModeInput, 0)
    private val hotOff      by userInput("Temperature Control", "Auto-Off Hot Side", true).visibleIf(controlModeInput, 0)
    private val coldCurrents by userInput("Current Control", "Cold Side [A] (-5 to +5)", Range.linear(0.0, -5.0, 3)).visibleIf(controlModeInput, 1) map {
        Range.manual(*it.array().map { current -> current.coerceIn(-5.0, 5.0) }.toTypedArray())
    }
    private val hotCurrents by userInput("Current Control", "Hot Side [A] (-5 to +5)", Range.linear(0.0, 5.0, 3)).visibleIf(controlModeInput, 1) map {
        Range.manual(*it.array().map { current -> current.coerceIn(-5.0, 5.0) }.toTypedArray())
    }
    private val currentTime by userTimeInput("Current Control", "For at least", 60000).visibleIf(controlModeInput, 1) map { it.toLong() }
    private val currentOff  by userInput("Current Control", "Auto-Off Both Sides", true).visibleIf(controlModeInput, 1)


    // Instruments
    private val vMeter1     by requiredInstrument("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2     by optionalInstrument("Thermal Voltage Meter 2", VMeter::class)
    private val ground      by optionalInstrument("Ground", SMU::class)
    private val tMeter1     by requiredInstrument("Thermometer 1", TMeter::class)
    private val tMeter2     by requiredInstrument("Thermometer 2", TMeter::class)
    private val tMeter3     by requiredInstrument("Thermometer 3", TMeter::class)
    private val tMeter4     by requiredInstrument("Thermometer 4", TMeter::class)
    private val tMeter5     by requiredInstrument("Thermometer 5", TMeter::class)
    private val hotPeltier  by requiredInstrument("Hot Peltier", TC.Loop::class)
    private val coldPeltier by requiredInstrument("Cold Peltier", TC.Loop::class)

    // Data columns
    companion object : Columns() {

        val VOLTAGE                      = decimalColumn("Voltage", "V")
        val VOLTAGE_ERROR                = decimalColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1                 = decimalColumn("Temperature 1", "K")
        val TEMPERATURE2                 = decimalColumn("Temperature 2", "K")
        val TEMPERATURE3                 = decimalColumn("Temperature 3", "K")
        val TEMPERATURE4                 = decimalColumn("Temperature 4", "K")
        val TEMPERATURE5                 = decimalColumn("Temperature 5", "K")
        val TEMPERATURE_GRADIENT         = decimalColumn("Temperature Gradient", "K/sensor")
        val TEMPERATURE_GRADIENT_ERROR   = decimalColumn("Temperature Gradient Error", "K/sensor")
        val TEMPERATURE_DIFFERENCE       = decimalColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_ERROR = decimalColumn("Temperature Difference Error", "K")
        val COLD_CURRENT_SETPOINT         = decimalColumn("Cold Current Setpoint", "A")
        val HOT_CURRENT_SETPOINT          = decimalColumn("Hot Current Setpoint", "A")
        val COLD_CURRENT_MEASURED         = decimalColumn("Cold Current Measured", "A")
        val HOT_CURRENT_MEASURED          = decimalColumn("Hot Current Measured", "A")

    }

    override fun processResults(data: ResultTable): TVHighThroughputResult {
        return TVHighThroughputResult(data)
    }

    override fun run(results: ResultTable) {

        if (controlMode == 0 && coldTemps.size() != hotTemps.size()) {
            throw Exception("Temperature arrays have different sizes")
        }

        if (controlMode == 1 && coldCurrents.size() != hotCurrents.size()) {
            throw Exception("Current arrays have different sizes")
        }

        // Record measurement parameters into result file
        results.setAttribute("Cold Temperatures", "$coldTemps K")
        results.setAttribute("Hot Temperatures", "$hotTemps K")
        results.setAttribute("Cold Control", if (controlMode == 1) "Constant current sweep: $coldCurrents A" else "PID temperature")
        results.setAttribute("Hot Control", if (controlMode == 1) "Constant current sweep: $hotCurrents A" else "PID temperature")
        if (controlMode == 1) results.setAttribute("Minimum Current Hold Time", "$currentTime ms")

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

        val coldCurrentLoop = coldPeltier as? TC.CurrentLimitedLoop
            ?: throw Exception("Cold Peltier does not support programmable current limits")
        val hotCurrentLoop = hotPeltier as? TC.CurrentLimitedLoop
            ?: throw Exception("Hot Peltier does not support programmable current limits")

        coldCurrentLoop.currentLimit = 5.0
        hotCurrentLoop.currentLimit = 5.0

        val controlPoints = if (controlMode == 0) {
            coldTemps.zip(hotTemps)
        } else {
            coldCurrents.zip(hotCurrents)
        }

        for ((coldValue, hotValue) in controlPoints) {

            if (controlMode == 1) {
                coldCurrentLoop.setCurrent(coldValue)
            } else {
                coldPeltier.setPoint = coldValue
            }

            if (controlMode == 1) {
                hotCurrentLoop.setCurrent(hotValue)
            } else {
                hotPeltier.setPoint = hotValue
            }

            coldPeltier.isPIDEnabled = true
            hotPeltier.isPIDEnabled  = true

            runInParallel(
                { if (controlMode == 0) coldPeltier.waitForStableTemperature(coldValue, pctStabPelt, durStabPelt) },
                { if (controlMode == 0) hotPeltier.waitForStableTemperature(hotValue, pctStabPelt, durStabPelt) },
                { if (controlMode == 1) Thread.sleep(currentTime) },
                { tMeter1.waitForStableTemperature(pctStabChip, durStabChip, tmoStabChip) },
                { tMeter2.waitForStableTemperature(pctStabChip, durStabChip, tmoStabChip) }
            )

            val v1 = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
            val v2 = Repeat.prepare(repeats, repTime) { vMeter2?.voltage ?: Double.NaN }
            val t1 = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
            val t2 = Repeat.prepare(repeats, repTime) { tMeter2.temperature }
            val t3 = Repeat.prepare(repeats, repTime) { tMeter3.temperature }
            val t4 = Repeat.prepare(repeats, repTime) { tMeter4.temperature }
            val t5 = Repeat.prepare(repeats, repTime) { tMeter5.temperature }

            Repeat.runTogether(v1, v2, t1, t2, t3, t4, t5)

            val fit = Fitting.linearFitWeighted(
                Range.linear(0, 4*spacing, 5),
                listOf(t1.mean, t2.mean, t3.mean, t4.mean, t5.mean),
                listOf(t1.standardDeviation, t2.standardDeviation, t3.standardDeviation, t4.standardDeviation, t5.standardDeviation).map{ it.pow(-2) }
            )

            results.mapRow(
                VOLTAGE                      to determineVoltage(v1.mean, v2.mean, ground?.voltage ?: Double.NaN),
                VOLTAGE_ERROR                to determineError(v1.standardDeviation, v2.standardDeviation, ground?.voltage ?: Double.NaN),
                TEMPERATURE1                 to t1.mean,
                TEMPERATURE2                 to t2.mean,
                TEMPERATURE3                 to t3.mean,
                TEMPERATURE4                 to t4.mean,
                TEMPERATURE5                 to t5.mean,
                TEMPERATURE_GRADIENT         to fit.gradient,
                TEMPERATURE_GRADIENT_ERROR   to fit.gradientError,
                TEMPERATURE_DIFFERENCE       to (fit.gradient * length),
                TEMPERATURE_DIFFERENCE_ERROR to (fit.gradientError * length),
                COLD_CURRENT_SETPOINT         to if (controlMode == 1) coldValue else Double.NaN,
                HOT_CURRENT_SETPOINT          to if (controlMode == 1) hotValue else Double.NaN,
                COLD_CURRENT_MEASURED         to if (controlMode == 1) coldCurrentLoop.current else Double.NaN,
                HOT_CURRENT_MEASURED          to if (controlMode == 1) hotCurrentLoop.current else Double.NaN
            )

        }


    }

    override fun onFinish() {

        runRegardless(
            { vMeter1.turnOff() },
            { vMeter2?.turnOff() },
            { ground?.turnOff() },
        )
        if (coldOff || (controlMode == 1 && currentOff)) {

            runRegardless(
                { coldPeltier.manualValue = 0.0},
                { coldPeltier.isPIDEnabled = false }
            )

        }

        if (hotOff || (controlMode == 1 && currentOff)) {

            runRegardless(
                { hotPeltier.manualValue  = 0.0},
                { hotPeltier.isPIDEnabled = false }
            )

        }

    }

    override fun createDisplay(data: ResultTable): TVHTPlot {
        return TVHTPlot(data)
    }

}
