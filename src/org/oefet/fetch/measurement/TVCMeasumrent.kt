package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.util.*

class TVCMeasumrent : FetChMeasurement() {

    override val type = "Thermal Voltage Calibration"

    var ground: SMU?    = null
    var tMeter: TMeter? = null
    var heater: SMU?    = null
    var strip: SMU?     = null

    val label    = StringParameter("Basic", "Name", null, "TVC")
    val intTime  = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    val avgCount = IntegerParameter("Basic", "Averaging Count", null,1)
    val probe    = IntegerParameter("Basic", "Strip Number", null, 0)
    val minHV    = DoubleParameter("Heater", "Start", "V", 0.0)
    val maxHV    = DoubleParameter("Heater", "Stop", "V", 10.0)
    val numHV    = IntegerParameter("Heater", "No. Steps", null, 11)
    val holdHV   = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    val minSI    = DoubleParameter("Resistive Thermometer", "Start", "A", 0.0)
    val maxSI    = DoubleParameter("Resistive Thermometer", "Stop", "A", 100e-6)
    val numSI    = IntegerParameter("Resistive Thermometer", "No. Steps", null, 11)
    val holdSI   = DoubleParameter("Resistive Thermometer", "Hold Time", "s", 0.5)


    private fun loadInstruments() {

        heater  = Instruments.htSMU
        ground  = Instruments.gdSMU
        strip   = Instruments.sdSMU
        tMeter  = Instruments.tMeter

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (strip == null) {
            errors += "Source-drain channel is not configured"
        }

        if (tMeter == null) {
            errors += "No gate channel configured."
        }

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

    }

    override fun run(results: ResultTable) {

        loadInstruments()

        val tMeter = this.tMeter!!
        val heater = this.heater!!
        val strip  = this.strip!!

        val intTime  =  this.intTime.value
        val avgCount = this.avgCount.value
        val minHV    =  this.minHV.value
        val maxHV    =  this.maxHV.value
        val numHV    =  this.numHV.value
        val holdHV   =  (this.holdHV.value * 1000).toInt()
        val minSI    =  this.minSI.value
        val maxSI    =  this.maxSI.value
        val numSI    =  this.numSI.value
        val holdSI   =  (this.holdSI.value * 1000).toInt()

        results.setAttribute("Probe Number", probe.value.toString())

        ground?.turnOff()
        heater.turnOff()
        strip.turnOff()

        ground?.integrationTime = intTime
        ground?.voltage         = 0.0
        heater.integrationTime  = intTime
        heater.voltage          = minHV
        strip.integrationTime   = intTime
        strip.current           = minSI
        strip.averageMode       = AMode.MEAN_REPEAT
        strip.averageCount      = avgCount

        ground?.turnOn()

        for (heaterVoltage in Range.linear(minHV, maxHV, numHV)) {

            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

            for (stripCurrent in Range.linear(minSI, maxSI, numSI)) {

                strip.current = stripCurrent
                strip.turnOn()
                sleep(holdSI)

                results.addData(
                    heaterVoltage,
                    stripCurrent,
                    ground?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    strip.voltage,
                    strip.current,
                    tMeter.temperature
                )

            }

        }

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff() }
        runRegardless { ground?.turnOff() }
        runRegardless { strip?.turnOff() }
    }

    override fun getLabel(): String {
        return label.value
    }

    override fun getName(): String {
        return "Thermal Voltage Calibration Measurement"
    }

    companion object {
        const val SET_HEATER_VOLTAGE = 0
        const val SET_STRIP_CURRENT  = 1
        const val GROUND_CURRENT     = 2
        const val HEATER_VOLTAGE     = 3
        const val HEATER_CURRENT     = 4
        const val HEATER_POWER       = 5
        const val STRIP_VOLTAGE      = 6
        const val STRIP_CURRENT      = 7
        const val TEMPERATURE        = 8
    }

    override fun getColumns(): Array<Col> {
        return arrayOf(
            Col("Set Heater Voltage", "V"),
            Col("Set Strip Current", "A"),
            Col("Ground Current", "A"),
            Col("Heater Voltage", "V"),
            Col("Heater Current", "A"),
            Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] },
            Col("Strip Voltage", "V"),
            Col("Strip Current", "A"),
            Col("Temperature", "K")
        )
    }

    override fun setLabel(value: String?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

}