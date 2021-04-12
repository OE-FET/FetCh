package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import org.oefet.fetch.gui.elements.TVCPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVCResult

class TVCalibration : FetChMeasurement("Thermal Voltage Calibration Measurement", "TVC", "Thermal Voltage Calibration") {

    // Parameters
    private val avgCount by input("Basic", "Averaging Count",1)
    private val avgDelay by input("Basic", "Averaging Delay [s]", 0.0) map { (it * 1e3).toInt() }
    private val probe    by choice("Basic", "Strip", "Left", "Right")
    private val heaterV  by input("Heater", "Heater Voltage [V]", Range.polynomial(0, 5, 6, 2))
    private val holdHV   by input("Heater", "Hold Time [s]", 60.0) map { (it * 1e3).toInt() }
    private val currents by input("Resistive Thermometer", "Current [A]", Range.linear(0.0, 100e-6, 11))
    private val holdSI   by input("Resistive Thermometer", "Hold Time", 0.5) map { (it * 1e3).toInt() }

    // Instruments
    private val gdSMU  by optionalConfig("Ground Channel (SPA)", SMU::class)
    private val heater by requiredConfig("Heater Channel", SMU::class)
    private val sdSMU  by requiredConfig("Strip Source-Drain Channel", SMU::class)
    private val fpp1   by optionalConfig("Four-Point Probe Channel 1", VMeter::class)
    private val fpp2   by optionalConfig("Four-Point Probe Channel 2", VMeter::class)
    private val tMeter by optionalConfig("Thermometer", TMeter::class)

    private val actionHeater = MeasurementSubAction("Hold Heater")
    private val actionSweep  = MeasurementSubAction("Sweep Current")

    companion object {

        val SET_HEATER_VOLTAGE  = Col("Set Heater Voltage", "V")
        val SET_STRIP_CURRENT   = Col("Set Strip Current", "A")
        val GROUND_CURRENT      = Col("Ground Current", "A")
        val HEATER_VOLTAGE      = Col("Heater Voltage", "V")
        val HEATER_CURRENT      = Col("Heater Current", "A")
        val HEATER_POWER        = Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] }
        val STRIP_VOLTAGE       = Col("Strip Voltage", "V")
        val STRIP_VOLTAGE_ERROR = Col("Strip Voltage Error", "V")
        val STRIP_CURRENT       = Col("Strip Current", "A")
        val TEMPERATURE         = Col("Temperature", "K")

    }

    override fun getActions(): List<Action<*>> {
        return listOf(actionHeater, actionSweep)
    }

    override fun createPlot(data: ResultTable): TVCPlot {
        return TVCPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVCResult {
        return TVCResult(data, extra)
    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Averaging Count", avgCount.toString())
        results.setAttribute("Probe Number", probe.toString())
        results.setAttribute("Heater Hold Time", "$holdHV ms")
        results.setAttribute("Delay Time", "$holdSI ms")

        gdSMU?.turnOff()
        heater.turnOff()
        sdSMU.turnOff()

        gdSMU?.voltage          = 0.0
        heater.voltage          = heaterV.first()
        sdSMU.current           = currents.first()
        sdSMU.averageMode       = AMode.NONE
        sdSMU.averageCount      = 1

        gdSMU?.turnOn()

        val voltage = if (fpp1 != null && fpp2 != null) {

            { fpp2!!.voltage - fpp1!!.voltage }

        } else if (fpp1 != null) {

            { fpp1!!.voltage }

        } else if (fpp2 != null) {

            { fpp2!!.voltage }

        } else {

            { sdSMU.voltage }

        }

        val stripMeasurement = Repeat.prepare(avgCount, avgDelay) { voltage() }

        for (heaterVoltage in heaterV) {

            actionHeater.start()

            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

            actionHeater.reset()

            actionSweep.start()

            for (stripCurrent in currents) {

                sdSMU.current = stripCurrent
                sdSMU.turnOn()
                sleep(holdSI)

                stripMeasurement.run()

                results.addData(
                    heaterVoltage,
                    stripCurrent,
                    gdSMU?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    stripMeasurement.mean,
                    stripMeasurement.standardDeviation,
                    sdSMU.current,
                    tMeter?.temperature ?: Double.NaN
                )

            }

            actionSweep.reset()

        }

    }

    override fun onFinish() {
        runRegardless { heater.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU.turnOff() }
        actions.forEach { it.reset() }
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            SET_HEATER_VOLTAGE,
            SET_STRIP_CURRENT,
            GROUND_CURRENT,
            HEATER_VOLTAGE,
            HEATER_CURRENT,
            HEATER_POWER,
            STRIP_VOLTAGE,
            STRIP_VOLTAGE_ERROR,
            STRIP_CURRENT,
            TEMPERATURE
        )

    }

    override fun onInterrupt() {

    }

    override fun onError() {

    }

}