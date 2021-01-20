package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections

class Conductivity : FMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity") {

    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 1.0)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -10e-6, +10e-6, 11, Range.Type.LINEAR, 1)
    private val symIParam    = BooleanParameter("Source-Drain", "Sweep Both Ways", null, false)
    private val holdGParam   = BooleanParameter("Source-Gate", "Active", null, false)
    private val gateVParam   = DoubleParameter("Source-Gate", "Voltage", "V", 50.0)

    val gdSMUConfig = addInstrument("Ground Channel (SPA)", SMU::class.java)
    val sdSMUConfig = addInstrument("Source-Drain Channel", SMU::class.java)
    val sgSMUConfig = addInstrument("Source-Gate Channel", SMU::class.java)
    val fpp1Config  = addInstrument("Four-Point Probe Channel 1", VMeter::class.java)
    val fpp2Config  = addInstrument("Four-Point Probe Channel 2", VMeter::class.java)
    private val tMeterConfig  = addInstrument("Thermometer", TMeter::class.java)

    val intTime  get() = intTimeParam.value
    val delTime  get() = (1e3 * delTimeParam.value).toInt()
    val currents get() = currentParam.value
    val symI     get() = symIParam.value
    val holdG    get() = holdGParam.value
    val gateV    get() = gateVParam.value

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        sgSMU    = sgSMUConfig.get()
        fpp1     = fpp1Config.get()
        fpp2     = fpp2Config.get()
        tMeter   = tMeterConfig.get()

    }

    override fun checkForErrors(): List<String> {

        val errors = ArrayList<String>()

        if (sdSMU == null) {
            errors += "SD channel not configured"
        }

        if (holdG && sgSMU == null) {
            errors += "SG channel not configured"
        }

        return errors

    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")

        // Assert that source-drain must be connected
        val sdSMU = sdSMU!!
        val sgSMU = if (holdG) sgSMU else null

        // Turn everything off before starting
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime  = intTime
        sgSMU?.integrationTime = intTime
        fpp1?.integrationTime  = intTime
        fpp2?.integrationTime  = intTime

        // Configure all channel voltages/currents
        gdSMU?.voltage = 0.0
        sgSMU?.voltage = gateV
        sdSMU.current  = currents.first()

        // Enable all channels
        gdSMU?.turnOn()
        sdSMU.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()
        sgSMU?.turnOn()

        // Sweep current
        for (current in if (symI) currents.mirror() else currents) {

            sdSMU.current = current
            sleep(delTime)

            results.addData(
                sdSMU.voltage, sdSMU.current,
                sgSMU?.voltage ?: Double.NaN, sgSMU?.current ?: Double.NaN,
                fpp1?.voltage ?: sdSMU.voltage,
                fpp2?.voltage ?: 0.0,
                tMeter?.temperature ?: Double.NaN,
                gdSMU?.current ?: Double.NaN
            )

        }

    }

    private fun sleep(millis: Long) = sleep(millis.toInt())

    override fun onFinish() {

        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun getColumns(): Array<Col> = arrayOf(
        Col("SD Voltage", "V"),
        Col("SD Current", "A"),
        Col("SG Voltage", "V"),
        Col("SG Current", "A"),
        Col("FPP 1 Voltage", "V"),
        Col("FPP 2 Voltage", "V"),
        Col("FPP Voltage", "V") { (it[FPP1_VOLTAGE] - it[FPP2_VOLTAGE]) },
        Col("Temperature", "K"),
        Col("Ground Current", "A")
    )

    override fun onInterrupt() {}

    companion object {
        const val SD_VOLTAGE = 0
        const val SD_CURRENT = 1
        const val SG_VOLTAGE = 2
        const val SG_CURRENT = 3
        const val FPP1_VOLTAGE = 4
        const val FPP2_VOLTAGE = 5
        const val FPP_VOLTAGE = 6
        const val TEMPERATURE = 7
        const val GROUND_CURRENT = 8
    }

}