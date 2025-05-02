package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Repeat
import jisa.devices.meter.IMeter
import jisa.devices.meter.TMeter
import jisa.devices.meter.VMeter
import jisa.devices.smu.SMU
import jisa.devices.source.VSource

import jisa.enums.AMode
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TVResult

class TVMeasurement : FetChMeasurement("Thermal Voltage Measurement", "TV", "Thermal Voltage", Images.getImage("fire.png")) {

    // User input parameters
    private val avgCount   by userInput("Basic", "Averaging Count", 1)
    private val avgDelay   by userTimeInput("Basic", "Averaging Delay", 0)
    private val order      by userChoice("Basic", "Sweep Order", "Gate → Heater", "Heater → Gate")
    private val heaterV    by userInput("Heater", "Heater Voltage [V]", Range.polynomial(0, 5, 6, 2))
    private val heaterHold by userTimeInput("Heater", "Hold Time", 60000)
    private val gates      by userInput("Gate", "Voltage [V]", Range.linear(0.0, 10.0, 11))
    private val gateHold   by userTimeInput("Gate", "Hold Time", 1000)
    private val gateOff    by userInput("Gate", "Auto Off", true)

    // Instruments
    private val gdSMU   by optionalInstrument("Ground Channel (SPA)", VSource::class)
    private val heater  by requiredInstrument("Heater Channel", SMU::class)
    private val sgSMU   by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { gates.any { it != 0.0 } }
    private val tvMeter by requiredInstrument("Thermal Voltage Channel", VMeter::class)
    private val tMeter  by optionalInstrument("Thermometer", TMeter::class)

    private val actionGate   = MeasurementSubAction("Gate")
    private val actionHeater = MeasurementSubAction("Heater")

    override fun getActions(): List<Action<*>> {

        return when(order) {
            ORDER_GATE_HEATER -> listOf(actionGate, actionHeater)
            ORDER_HEATER_GATE -> listOf(actionHeater, actionGate)
            else              -> emptyList()
        }

    }

    override fun createDisplay(data: ResultTable): TVPlot {
        return TVPlot(data)
    }

    override fun processResults(data: ResultTable): TVResult {
        return TVResult(data)
    }

    // Constants for referring to result table columns
    companion object : Columns() {

        val MEAS_NO               = decimalColumn("Measurement No.")
        val SET_GATE              = decimalColumn("Gate Set", "V")
        val SET_HEATER            = decimalColumn("Heater Set", "V")
        val TEMPERATURE           = decimalColumn("Temperature", "K")
        val GATE_VOLTAGE          = decimalColumn("Gate Voltage", "V")
        val GATE_CURRENT          = decimalColumn("Gate Current", "A")
        val HEATER_VOLTAGE        = decimalColumn("Heater Voltage", "V")
        val HEATER_CURRENT        = decimalColumn("Heater Current", "A")
        val HEATER_POWER          = decimalColumn("Heater Power", "W")
        val THERMAL_VOLTAGE       = decimalColumn("Thermal Voltage", "V")
        val THERMAL_VOLTAGE_ERROR = decimalColumn("Thermal Voltage Error", "V")
        val THERMAL_CURRENT       = decimalColumn("Thermal Current", "A")

        const val ORDER_GATE_HEATER = 0
        const val ORDER_HEATER_GATE = 1

    }

    override fun run(results: ResultTable) {

        // Record measurement parameters into result file
        results.setAttribute("Integration Time", "${tvMeter.integrationTime} s")
        results.setAttribute("Averaging Count", avgCount.toString())
        results.setAttribute("Heater Hold Time", "$heaterHold ms")
        results.setAttribute("Gate Hold Time", "$gateHold ms")
        results.setAttribute("Order", when (order) {
            ORDER_HEATER_GATE -> "Heater:Gate"
            ORDER_GATE_HEATER -> "Gate:Heater"
            else              -> "Unknown"
        })

        // Start with everything turned-off
        heater.turnOff()
        tvMeter.turnOff()
        gdSMU?.turnOff()

        if (gateOff) {
            sgSMU?.turnOff()
        }

        // Configure voltage channels to source initial values (or 0V in the case of ground channel)
        heater.voltage = heaterV.first()
        sgSMU?.voltage = gates.first()
        gdSMU?.voltage = 0.0

        // We don't want the instruments to do any averaging - we're doing that ourselves
        tvMeter.averageMode = AMode.NONE
        heater.averageMode  = AMode.NONE
        sgSMU?.averageMode  = AMode.NONE

        tvMeter.turnOn()
        gdSMU?.turnOn()

        var count = 0.0

        // Measurement routine will depend on which order of sweeps we chose
        when (order) {

            ORDER_GATE_HEATER -> {

                for (gateVoltage in gates) {

                    actionGate.start()
                    sgSMU?.voltage = gateVoltage
                    sgSMU?.turnOn()

                    sleep(gateHold)
                    actionGate.reset()

                    actionHeater.start()

                    for (heaterVoltage in heaterV) {

                        heater.voltage = heaterVoltage
                        heater.turnOn()

                        sleep(heaterHold)

                        // Take repeat measurements of thermal voltage
                        val tvVoltage = Repeat.run(avgCount, avgDelay) { tvMeter.voltage }
                        val hVoltage  = heater.voltage
                        val hCurrent  = heater.current
                        val hPower    = hVoltage * hCurrent

                        results.mapRow(
                            MEAS_NO               to count++,
                            SET_GATE              to gateVoltage,
                            SET_HEATER            to heaterVoltage,
                            TEMPERATURE           to (tMeter?.temperature ?: Double.NaN),
                            GATE_VOLTAGE          to (sgSMU?.voltage ?: Double.NaN),
                            GATE_CURRENT          to (sgSMU?.current ?: Double.NaN),
                            HEATER_VOLTAGE        to hVoltage,
                            HEATER_CURRENT        to hCurrent,
                            HEATER_POWER          to hPower,
                            THERMAL_VOLTAGE       to tvVoltage.mean,
                            THERMAL_VOLTAGE_ERROR to tvVoltage.standardDeviation,
                            THERMAL_CURRENT       to (if (tvMeter is IMeter) (tvMeter as IMeter).current else Double.NaN)
                        )

                    }

                    actionHeater.reset()

                    heater.turnOff()
                    sleep(heaterHold)

                }

            }

            ORDER_HEATER_GATE -> {

                for (heaterVoltage in heaterV) {

                    actionHeater.start()

                    heater.voltage = heaterVoltage
                    heater.turnOn()

                    sleep(heaterHold)

                    actionHeater.reset()

                    actionGate.start()

                    for (gateVoltage in gates) {

                        sgSMU?.voltage = gateVoltage
                        sgSMU?.turnOn()

                        sleep(gateHold)

                        // Take repeat measurements of thermal voltage
                        val tvVoltage = Repeat.run(avgCount, avgDelay) { tvMeter.voltage }
                        val hVoltage  = heater.voltage
                        val hCurrent  = heater.current
                        val hPower    = hVoltage * hCurrent

                        results.mapRow(
                            MEAS_NO               to count++,
                            SET_GATE              to gateVoltage,
                            SET_HEATER            to heaterVoltage,
                            TEMPERATURE           to (tMeter?.temperature ?: Double.NaN),
                            GATE_VOLTAGE          to (sgSMU?.voltage ?: Double.NaN),
                            GATE_CURRENT          to (sgSMU?.current ?: Double.NaN),
                            HEATER_VOLTAGE        to hVoltage,
                            HEATER_CURRENT        to hCurrent,
                            HEATER_POWER          to hPower,
                            THERMAL_VOLTAGE       to tvVoltage.mean,
                            THERMAL_VOLTAGE_ERROR to tvVoltage.standardDeviation,
                            THERMAL_CURRENT       to (if (tvMeter is IMeter) (tvMeter as IMeter).current else Double.NaN)
                        )

                    }

                    actionGate.reset()

                    if (gateOff) {
                        sgSMU?.turnOff()
                    }

                    sleep(gateHold)

                }

            }

        }

    }

    override fun onFinish() {

        runRegardless(
            { heater.turnOff() },
            { gdSMU?.turnOff() },
            { tvMeter.turnOff() }
        )

        if (gateOff) {
            runRegardless { sgSMU?.turnOff() }
        }

    }

    override fun onInterrupt() {
        Util.errLog.println("TV Measurement Interrupted")
    }

}