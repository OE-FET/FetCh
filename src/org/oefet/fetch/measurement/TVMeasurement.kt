package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import java.util.*
import kotlin.Exception

class TVMeasurement : FMeasurement("Thermal Voltage Measurement", "TV", "Thermal Voltage") {

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

    private val intTimeParam    = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val avgCountParam   = IntegerParameter("Basic", "Averaging Count", null, 1)
    private val heaterVParam    = RangeParameter("Heater", "Heater Voltage", "V", 0.0, 5.0, 6, Range.Type.POLYNOMIAL, 2)
    private val symHVParam      = BooleanParameter("Heater", "Sweep Both Ways", null, false)
    private val heaterHoldParam = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    private val gateParam       = RangeParameter("Gate", "Voltage", "V", 0.0, 10.0, 11, Range.Type.LINEAR, 1)
    private val symSGVParam     = BooleanParameter("Gate", "Sweep Both Ways", null, false)
    private val gateHoldParam   = DoubleParameter("Gate", "Hold Time", "s", 1.0)

    val gdSMUConfig   = addInstrument("Ground Channel (SPA)", SMU::class.java)
    val htSMUConfig   = addInstrument("Heater Channel", SMU::class.java)
    val sgSMUConfig   = addInstrument("Source-Gate Channel", SMU::class.java)
    val tvMeterConfig = addInstrument("Thermal Voltage Channel", VMeter::class.java)
    private val tMeterConfig  = addInstrument("Thermometer", TMeter::class.java)

    val intTime    get() = intTimeParam.value
    val avgCount   get() = avgCountParam.value
    val heaterV    get() = heaterVParam.value
    val symHV      get() = symHVParam.value
    val heaterHold get() = (heaterHoldParam.value * 1000).toInt()
    val gates      get() = gateParam.value
    val symSGV     get() = symSGVParam.value
    val gateHold   get() = (gateHoldParam.value * 1000).toInt()

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        heater   = htSMUConfig.get()
        sgSMU    = sgSMUConfig.get()
        tvMeter  = tvMeterConfig.get()
        tMeter   = tMeterConfig.get()

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

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Averaging Count", avgCount.toString())
        results.setAttribute("Heater Hold Time", "$heaterHold ms")
        results.setAttribute("Gate Hold Time", "$gateHold ms")

        val heater  = this.heater!!
        val tvMeter = this.tvMeter!!

        heater.turnOff()
        tvMeter.turnOff()
        gdSMU?.turnOff()
        sgSMU?.turnOff()

        heater.voltage           = heaterV.first()
        heater.integrationTime   = intTime
        tvMeter.averageMode      = AMode.MEAN_REPEAT
        tvMeter.averageCount     = avgCount
        tvMeter.integrationTime  = intTime
        gdSMU?.voltage           = 0.0
        gdSMU?.integrationTime   = intTime
        sgSMU?.voltage           = gates.first()
        sgSMU?.integrationTime   = intTime

        tvMeter.turnOn()
        gdSMU?.turnOn()

        var count = 0.0

        for (gateVoltage in gates) {

            sgSMU?.voltage = gateVoltage
            sgSMU?.turnOn()

            sleep(gateHold)

            for (heaterVoltage in if (symHV) heaterV.mirror() else heaterV) {

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

    override fun onInterrupt() {
        Util.errLog.println("TV Measurement Interrupted")
    }

}