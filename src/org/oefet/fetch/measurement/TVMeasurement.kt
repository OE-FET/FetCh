package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.util.*
import kotlin.Exception

class TVMeasurement : FetChMeasurement() {

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

    var heater:  SMU?    = null
    var ground:  SMU?    = null
    var gate:    SMU?    = null
    var tvMeter: VMeter? = null
    var tMeter:  TMeter? = null

    val label      = StringParameter("Basic", "Name", null, "TV")
    val intTime    = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    val avgCount   = IntegerParameter("Basic", "Averaging Count", null, 1)
    val minHV      = DoubleParameter("Heater", "Start", "V", 0.0)
    val maxHV      = DoubleParameter("Heater", "Stop", "V", 10.0)
    val numHV      = IntegerParameter("Heater", "No. Steps", null, 11)
    val symHV      = BooleanParameter("Heater", "Sweep Both Ways", null, false)
    val heaterHold = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    val minSGV     = DoubleParameter("Gate", "Start", "V", 0.0)
    val maxSGV     = DoubleParameter("Gate", "Stop", "V", 10.0)
    val numSGV     = IntegerParameter("Gate", "No. Steps", null, 11)
    val symSGV     = BooleanParameter("Gate", "Sweep Both Ways", null, false)
    val gateHold   = DoubleParameter("Gate", "Hold Time", "s", 1.0)

    private fun loadInstruments() {

        heater  = Instruments.htSMU
        ground  = Instruments.gdSMU
        gate    = Instruments.sgSMU
        tvMeter = Instruments.thermalVolt
        tMeter  = Instruments.tMeter

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (tvMeter == null) {
            errors += "Thermal-Voltage Voltmeter is not configured"
        }

        if (gate == null && (numSGV.value > 1 || minSGV.value != 0.0 || maxSGV.value != 0.0)) {
            errors += "No gate channel configured."
        }

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

    }

    override fun run(results: ResultTable) {

        loadInstruments()

        val intTime    = this.intTime.value
        val avgCount   = this.avgCount.value
        val minHV      = this.minHV.value
        val maxHV      = this.maxHV.value
        val numHV      = this.numHV.value
        val symHV      = this.symHV.value
        val heaterHold = (this.heaterHold.value * 1000).toInt()
        val minSGV     = this.minSGV.value
        val maxSGV     = this.maxSGV.value
        val numSGV     = this.numSGV.value
        val symSGV     = this.symSGV.value
        val gateHold   = (this.gateHold.value * 1000).toInt()

        val heater  = this.heater!!
        val tvMeter = this.tvMeter!!

        heater.turnOff()
        tvMeter.turnOff()
        ground?.turnOff()
        gate?.turnOff()

        heater.voltage          = minHV
        heater.integrationTime  = intTime
        tvMeter.averageMode     = AMode.MEAN_REPEAT
        tvMeter.averageCount    = avgCount
        tvMeter.integrationTime = intTime
        ground?.voltage         = 0.0
        ground?.integrationTime = intTime
        gate?.voltage           = minSGV
        gate?.integrationTime   = intTime

        val gateVoltages   = Range.linear(minSGV, maxSGV, numSGV)
        val heaterVoltages = if (symHV) Range.linear(minHV, maxHV, numHV).mirror() else Range.linear(minHV, maxHV, numHV)

        tvMeter.turnOn()
        ground?.turnOn()

        var count = 0.0

        for (gateVoltage in gateVoltages) {

            gate?.voltage = gateVoltage
            gate?.turnOn()

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
                    gate?.voltage ?: Double.NaN,
                    gate?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    tvMeter.voltage,
                    if (tvMeter is SMU) tvMeter.current else Double.NaN
                )

            }

            heater.turnOff()
            sleep(heaterHold)

        }

        gate?.turnOff()

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff() }
        runRegardless { gate?.turnOff() }
        runRegardless { ground?.turnOff() }
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