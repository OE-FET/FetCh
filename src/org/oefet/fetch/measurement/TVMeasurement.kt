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

    // Basic parameters
    private val avgCountParam   = IntegerParameter("Basic", "Averaging Count", null, 1)
    private val avgDelayParam   = DoubleParameter("Basic", "Averaging Delay", "s", 0.0)
    private val orderParam      = ChoiceParameter("Basic", "Sweep Order", 0, "Gate → Heater", "Heater → Gate")

    // Heater parameters
    private val heaterVParam    = RangeParameter("Heater", "Heater Voltage", "V", Range.polynomial(0, 5, 6, 2), 0.0, 5.0, 6, 1.0, 2)
    private val symHVParam      = BooleanParameter("Heater", "Sweep Both Ways", null, false)
    private val heaterHoldParam = DoubleParameter("Heater", "Hold Time", "s", 60.0)

    // Gate parameters
    private val gateParam       = RangeParameter("Gate", "Voltage", "V", 0.0, 10.0, 11)
    private val symSGVParam     = BooleanParameter("Gate", "Sweep Both Ways", null, false)
    private val gateHoldParam   = DoubleParameter("Gate", "Hold Time", "s", 1.0)

    // Instrument configurators
    private val gdSMUConfig     = addOptionalInstrument("Ground Channel (SPA)", SMU::class)
    private val htSMUConfig     = addOptionalInstrument("Heater Channel", SMU::class)
    private val sgSMUConfig     = addOptionalInstrument("Source-Gate Channel", SMU::class)
    private val tvMeterConfig   = addOptionalInstrument("Thermal Voltage Channel", VMeter::class)
    private val tMeterConfig    = addOptionalInstrument("Thermometer", TMeter::class)

    // Quick access to parameter values
    val avgCount   get() = avgCountParam.value
    val avgDelay   get() = (avgDelayParam.value * 1000).toInt()
    val heaterV    get() = heaterVParam.value
    val symHV      get() = symHVParam.value
    val heaterHold get() = (heaterHoldParam.value * 1000).toInt()
    val gates      get() = gateParam.value
    val symSGV     get() = symSGVParam.value
    val gateHold   get() = (gateHoldParam.value * 1000).toInt()
    val order      get() = orderParam.value

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

    override fun loadInstruments() {

        gdSMU   = gdSMUConfig.get()
        heater  = htSMUConfig.get()
        sgSMU   = sgSMUConfig.get()
        tvMeter = tvMeterConfig.get()
        tMeter  = tMeterConfig.get()

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (tvMeter == null) {
            errors += "Thermal-Voltage Voltmeter is not configured"
        }

        if (sgSMU == null && !(gates.max() != gates.min())) {
            errors += "No gate channel configured."
        }

        return errors

    }

    override fun run(results: ResultTable) {

        // Heater SMU and TV voltmeter channels MUST always be configured
        val heater  = this.heater!!
        val tvMeter = this.tvMeter!!

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
                            if (tvMeter is IMeter) tvMeter.current else Double.NaN
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
                            if (tvMeter is SMU) tvMeter.current else Double.NaN
                        )

                    }

                    sgSMU?.turnOff()
                    sleep(gateHold)

                }

            }

        }

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { tvMeter?.turnOff() }
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