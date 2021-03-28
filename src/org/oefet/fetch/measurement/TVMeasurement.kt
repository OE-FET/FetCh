package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Repeat
import jisa.devices.interfaces.IMeter
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVResult
import java.util.*

class TVMeasurement : FMeasurement("Thermal Voltage Measurement", "TV", "Thermal Voltage") {

    // User input parameters
    private val avgCount   by input("Basic", "Averaging Count", 1)
    private val avgDelay   by input("Basic", "Averaging Delay [s]", 0.0) { (it * 1e3).toInt() }
    private val order      by choice("Basic", "Sweep Order", "Gate → Heater", "Heater → Gate")
    private val heaterV    by input("Heater", "Heater Voltage [V]", Range.polynomial(0, 5, 6, 2))
    private val symHV      by input("Heater", "Sweep Both Ways", false)
    private val heaterHold by input("Heater", "Hold Time [s]", 60.0) { (it * 1e3).toInt() }
    private val gates      by input("Gate", "Voltage [V]", Range.linear(0.0, 10.0, 11))
    private val symSGV     by input("Gate", "Sweep Both Ways", false)
    private val gateHold   by input("Gate", "Hold Time [s]", 1.0) { (it * 1e3).toInt() }

    // Instruments
    private val gdSMU   by optionalConfig("Ground Channel (SPA)", SMU::class)
    private val heater  by requiredConfig("Heater Channel", SMU::class)
    private val sgSMU   by optionalConfig("Source-Gate Channel", SMU::class)
    private val tvMeter by requiredConfig("Thermal Voltage Channel", VMeter::class)
    private val tMeter  by optionalConfig("Thermometer", TMeter::class)

    override fun createPlot(data: ResultTable): TVPlot {
        return TVPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVResult {
        return TVResult(data, extra)
    }

    // Constants for referring to result table columns
    companion object {

        val MEAS_NO               = Col("Measurement No.")
        val SET_GATE              = Col("Gate Set", "V")
        val SET_HEATER            = Col("Heater Set", "V")
        val TEMPERATURE           = Col("Temperature", "K")
        val GATE_VOLTAGE          = Col("Gate Voltage", "V")
        val GATE_CURRENT          = Col("Gate Current", "A")
        val HEATER_VOLTAGE        = Col("Heater Voltage", "V")
        val HEATER_CURRENT        = Col("Heater Current", "A")
        val HEATER_POWER          = Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] }
        val THERMAL_VOLTAGE       = Col("Thermal Voltage", "V")
        val THERMAL_VOLTAGE_ERROR = Col("Thermal Voltage Error", "V")
        val THERMAL_CURRENT       = Col("Thermal Current", "A")

        const val ORDER_GATE_HEATER = 0
        const val ORDER_HEATER_GATE = 1

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (sgSMU == null && !(gates.max() == 0.0 && gates.min() == 0.0)) {
            errors += "No gate channel configured."
        }

        return errors

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
        sgSMU?.turnOff()

        // Configure voltage channels to source initial values (or 0V in the case of ground channel)
        heater.voltage = heaterV.first()
        sgSMU?.voltage = gates.first()
        gdSMU?.voltage = 0.0

        // We don't want the instruments to do any averaging - we're doing that ourselves
        tvMeter.averageMode = AMode.NONE
        heater.averageMode  = AMode.NONE
        sgSMU?.averageMode  = AMode.NONE
        gdSMU?.averageMode  = AMode.NONE

        tvMeter.turnOn()
        gdSMU?.turnOn()

        var count = 0.0

        // Measurement routine will depend on which order of sweeps we chose
        when (order) {

            ORDER_GATE_HEATER -> {

                for (gateVoltage in gates) {

                    sgSMU?.voltage = gateVoltage
                    sgSMU?.turnOn()

                    sleep(gateHold)

                    for (heaterVoltage in if (symHV) heaterV.mirror() else heaterV) {

                        heater.voltage = heaterVoltage
                        heater.turnOn()

                        sleep(heaterHold)

                        // Take repeat measurements of thermal voltage
                        val tvVoltage = Repeat.run(avgCount, avgDelay) { tvMeter.voltage }

                        results.addData(
                            count ++,
                            gateVoltage,
                            heaterVoltage,
                            tMeter?.temperature ?: Double.NaN,
                            sgSMU?.voltage ?: Double.NaN,
                            sgSMU?.current ?: Double.NaN,
                            heater.voltage,
                            heater.current,
                            tvVoltage.mean,
                            tvVoltage.standardDeviation,
                            if (tvMeter is IMeter) (tvMeter as IMeter).current else Double.NaN
                        )

                    }

                    heater.turnOff()
                    sleep(heaterHold)

                }

            }

            ORDER_HEATER_GATE -> {

                for (heaterVoltage in if (symHV) heaterV.mirror() else heaterV) {

                    heater.voltage = heaterVoltage
                    heater.turnOn()

                    sleep(heaterHold)

                    for (gateVoltage in gates) {

                        sgSMU?.voltage = gateVoltage
                        sgSMU?.turnOn()

                        sleep(gateHold)

                        // Take repeat measurements of thermal voltage
                        val tvVoltage = Repeat.run(avgCount, avgDelay) { tvMeter.voltage }

                        results.addData(
                            count ++,
                            gateVoltage,
                            heaterVoltage,
                            tMeter?.temperature ?: Double.NaN,
                            sgSMU?.voltage ?: Double.NaN,
                            sgSMU?.current ?: Double.NaN,
                            heater.voltage,
                            heater.current,
                            tvVoltage.mean,
                            tvVoltage.standardDeviation,
                            if (tvMeter is IMeter) (tvMeter as IMeter).current else Double.NaN
                        )

                    }

                    sgSMU?.turnOff()
                    sleep(gateHold)

                }

            }

        }

    }

    override fun onFinish() {
        runRegardless { heater.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { tvMeter.turnOff() }
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            MEAS_NO,
            SET_GATE,
            SET_HEATER,
            TEMPERATURE,
            GATE_VOLTAGE,
            GATE_CURRENT,
            HEATER_VOLTAGE,
            HEATER_CURRENT,
            HEATER_POWER,
            THERMAL_VOLTAGE,
            THERMAL_VOLTAGE_ERROR,
            THERMAL_CURRENT
        )

    }

    override fun onInterrupt() {
        Util.errLog.println("TV Measurement Interrupted")
    }

    override fun onError() {

    }

}