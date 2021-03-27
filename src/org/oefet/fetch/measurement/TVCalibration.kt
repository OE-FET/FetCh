package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.TVCPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVCResult
import java.util.*

class TVCalibration : FMeasurement("Thermal Voltage Calibration Measurement", "TVC", "Thermal Voltage Calibration") {

    private val avgCountParam = IntegerParameter("Basic", "Averaging Count", null,1)
    private val avgDelayParam = DoubleParameter("Basic", "Averaging Delay", "s", 0.0)
    private val probeParam    = ChoiceParameter("Basic", "Strip", 0, "Left", "Right")
    private val heaterVParam  = RangeParameter("Heater", "Heater Voltage", "V", Range.polynomial(0, 5, 6, 2), 0.0, 5.0, 6, 1.0, 2)
    private val holdHVParam   = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    private val currParam     = RangeParameter("Resistive Thermometer", "Current", "A", 0.0, 100e-6, 11)
    private val holdSIParam   = DoubleParameter("Resistive Thermometer", "Hold Time", "s", 0.5)

    private val gdSMUConfig  = addOptionalInstrument("Ground Channel (SPA)", SMU::class) { gdSMU = it }
    private val htSMUConfig  = addOptionalInstrument("Heater Channel", SMU::class) { heater = it }
    private val sdSMUConfig  = addOptionalInstrument("Strip Source-Drain Channel", SMU::class) { sdSMU = it }
    private val fpp1Config   = addOptionalInstrument("Four-Point Probe Channel 1", VMeter::class) { fpp1 = it }
    private val fpp2Config   = addOptionalInstrument("Four-Point Probe Channel 2", VMeter::class) { fpp2 = it }
    private val tMeterConfig = addOptionalInstrument("Thermometer", TMeter::class) { tMeter = it }

    val avgCount get() = avgCountParam.value
    val avgDelay get() = (avgDelayParam.value * 1000.0).toInt()
    val probe    get() = probeParam.value
    val heaterV  get() = heaterVParam.value
    val holdHV   get() = (1e3 * holdHVParam.value).toInt()
    val currents get() = currParam.value
    val holdSI   get() = (1e3 * holdSIParam.value).toInt()

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

    override fun createPlot(data: ResultTable): TVCPlot {
        return TVCPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVCResult {
        return TVCResult(data, extra)
    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (sdSMU == null) {
            errors += "Source-drain channel is not configured"
        }

        return errors

    }

    override fun run(results: ResultTable) {

        val heater = this.heater!!
        val sdSMU  = this.sdSMU!!

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

            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

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

        }

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU?.turnOff() }
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