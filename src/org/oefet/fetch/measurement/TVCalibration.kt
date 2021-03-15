package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.util.*

class TVCalibration : FMeasurement("Thermal Voltage Calibration Measurement", "TVC", "Thermal Voltage Calibration") {

    val avgCountParam = IntegerParameter("Basic", "Averaging Count", null,1)
    val probeParam    = ChoiceParameter("Basic", "Strip", 0, "Left", "Right")
    val heaterVParam  = RangeParameter("Heater", "Heater Voltage", "V", 0.0, 5.0, 6, Range.Type.POLYNOMIAL, 2)
    val holdHVParam   = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    val currParam     = RangeParameter("Resistive Thermometer", "Current", "A", 0.0, 100e-6, 11, Range.Type.LINEAR, 1)
    val holdSIParam   = DoubleParameter("Resistive Thermometer", "Hold Time", "s", 0.5)

    val gdSMUConfig = addInstrument("Ground Channel (SPA)", SMU::class.java)
    val htSMUConfig = addInstrument("Heater Channel", SMU::class.java)
    val sdSMUConfig = addInstrument("Strip Source-Drain Channel", SMU::class.java)
    val fpp1Config  = addInstrument("Four-Point Probe Channel 1", VMeter::class.java)
    val fpp2Config  = addInstrument("Four-Point Probe Channel 2", VMeter::class.java)
    private val tMeterConfig  = addInstrument("Thermometer", TMeter::class.java)

    val avgCount get() = avgCountParam.value
    val probe    get() = probeParam.value
    val heaterV  get() = heaterVParam.value
    val holdHV   get() = (1e3 * holdHVParam.value).toInt()
    val currents get() = currParam.value
    val holdSI   get() = (1e3 * holdSIParam.value).toInt()

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        heater   = htSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        fpp1     = fpp1Config.get()
        fpp2     = fpp2Config.get()
        tMeter   = tMeterConfig.get()

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (sdSMU == null) {
            errors += "Source-drain channel is not configured"
        }

//        if (tMeter == null) {
//            errors += "No thermometer configured"
//        }

        return errors

    }

    override fun run(results: ResultTable) {

//        val tMeter = this.tMeter!!
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
        heater.voltage          = heaterV[0]
        sdSMU.current           = currents.first()
        sdSMU.averageMode       = AMode.MEAN_REPEAT
        sdSMU.averageCount      = avgCount

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

        for (heaterVoltage in heaterV) {

            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

            for (stripCurrent in currents) {

                sdSMU.current = stripCurrent
                sdSMU.turnOn()
                sleep(holdSI)

                results.addData(
                    heaterVoltage,
                    stripCurrent,
                    gdSMU?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    voltage(),
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

    override fun onInterrupt() {

    }

    override fun onError() {

    }

}