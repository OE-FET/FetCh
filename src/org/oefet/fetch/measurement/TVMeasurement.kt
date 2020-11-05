package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.util.*
import kotlin.Exception

class TVMeasurement : FMeasurement() {

    companion object {

        const val MEAS_NO         = 0
        const val SET_GATE        = 1
        const val SET_HEATER      = 2
        const val TEMPERATURE     = 3
        const val GATE_VOLTAGE    = 4
        const val GATE_CURRENT    = 5
        const val HEATER_VOLTAGE  = 6
        const val HEATER_CURRENT  = 7
        const val HEATER_POWER    = 8
        const val THERMAL_VOLTAGE = 9
        const val THERMAL_CURRENT = 10

    }

    override val type = "Thermal Voltage"

    private val label           = StringParameter("Basic", "Name", null, "TV")
    private val intTimeParam    = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val avgCountParam   = IntegerParameter("Basic", "Averaging Count", null, 1)
    private val minHVParam      = DoubleParameter("Heater", "Start", "V", 0.0)
    private val maxHVParam      = DoubleParameter("Heater", "Stop", "V", 10.0)
    private val numHVParam      = IntegerParameter("Heater", "No. Steps", null, 11)
    private val symHVParam      = BooleanParameter("Heater", "Sweep Both Ways", null, false)
    private val heaterHoldParam = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    private val minSGVParam     = DoubleParameter("Gate", "Start", "V", 0.0)
    private val maxSGVParam     = DoubleParameter("Gate", "Stop", "V", 10.0)
    private val numSGVParam     = IntegerParameter("Gate", "No. Steps", null, 11)
    private val symSGVParam     = BooleanParameter("Gate", "Sweep Both Ways", null, false)
    private val gateHoldParam   = DoubleParameter("Gate", "Hold Time", "s", 1.0)

    val intTime    get() = intTimeParam.value
    val avgCount   get() = avgCountParam.value
    val minHV      get() = minHVParam.value
    val maxHV      get() = maxHVParam.value
    val numHV      get() = numHVParam.value
    val symHV      get() = symHVParam.value
    val heaterHold get() = (heaterHoldParam.value * 1000).toInt()
    val minSGV     get() = minSGVParam.value
    val maxSGV     get() = maxSGVParam.value
    val numSGV     get() = numSGVParam.value
    val symSGV     get() = symSGVParam.value
    val gateHold   get() = (gateHoldParam.value * 1000).toInt()

    override fun loadInstruments() {

        gdSMU    = Instruments.gdSMU
        heater   = Instruments.htSMU
        sgSMU    = Instruments.sgSMU
        tvMeter  = Instruments.tvMeter
        tMeter   = Instruments.tMeter

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (tvMeter == null) {
            errors += "Thermal-Voltage Voltmeter is not configured"
        }

        if (sgSMU == null && (numSGV > 1 || minSGV != 0.0 || maxSGV != 0.0)) {
            errors += "No gate channel configured."
        }

        return errors

    }

    override fun run(results: ResultTable) {

        val heater  = this.heater!!
        val tvMeter = this.tvMeter!!

        heater.turnOff()
        tvMeter.turnOff()
        gdSMU?.turnOff()
        sgSMU?.turnOff()

        heater.voltage           = minHV
        heater.integrationTime   = intTime
        tvMeter.averageMode      = AMode.MEAN_REPEAT
        tvMeter.averageCount     = avgCount
        tvMeter.integrationTime  = intTime
        gdSMU?.voltage           = 0.0
        gdSMU?.integrationTime   = intTime
        sgSMU?.voltage           = minSGV
        sgSMU?.integrationTime   = intTime

        val gateVoltages   = Range.linear(minSGV, maxSGV, numSGV)
        val heaterVoltages = if (symHV) Range.linear(minHV, maxHV, numHV).mirror() else Range.linear(minHV, maxHV, numHV)

        tvMeter.turnOn()
        gdSMU?.turnOn()

        var count = 0.0

        for (gateVoltage in gateVoltages) {

            sgSMU?.voltage = gateVoltage
            sgSMU?.turnOn()

            sleep(gateHold)

            for (heaterVoltage in heaterVoltages) {

                heater.voltage = heaterVoltage
                heater.turnOn()

                sleep(heaterHold)

                results.addData(
                    count ++,
                    gateVoltage,
                    heaterVoltage,
                    tMeter?.temperature ?: Double.NaN,
                    sgSMU?.voltage ?: Double.NaN,
                    sgSMU?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    tvMeter.voltage,
                    if (tvMeter is SMU) tvMeter.current else Double.NaN
                )

            }

            heater.turnOff()
            sleep(heaterHold)

        }

        sgSMU?.turnOff()

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff()  }
        runRegardless { sgSMU?.turnOff()    }
        runRegardless { gdSMU?.turnOff()  }
        runRegardless { tvMeter?.turnOff() }
    }

    override fun getLabel() = label.value

    override fun getName()  = "Thermal Voltage Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Measurement No."),
            Col("Gate Set", "V"),
            Col("Heater Set", "V"),
            Col("Temperature", "K"),
            Col("Gate Voltage", "V"),
            Col("Gate Current", "A"),
            Col("Heater Voltage", "V"),
            Col("Heater Current", "A"),
            Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] },
            Col("Thermal Voltage", "V"),
            Col("Thermal Current", "A")
        )

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun onInterrupt() {
        Util.errLog.println("TV Measurement Interrupted")
    }

}