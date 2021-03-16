package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections

class Conductivity : FMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity") {

    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 1.0)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -10e-6, +10e-6, 11, Range.Type.LINEAR, 1)
    private val symIParam    = BooleanParameter("Source-Drain", "Sweep Both Ways", null, false)
    private val holdGParam   = BooleanParameter("Source-Gate", "Active", null, false)
    private val gateVParam   = DoubleParameter("Source-Gate", "Voltage", "V", 50.0)

    private val gdSMUConfig  = addInstrument("Ground Channel (SPA)", SMU::class) { gdSMU = it }
    private val sdSMUConfig  = addInstrument("Source-Drain Channel", SMU::class) { sdSMU = it }
    private val sgSMUConfig  = addInstrument("Source-Gate Channel", SMU::class) { sgSMU = it }
    private val fpp1Config   = addInstrument("Four-Point Probe Channel 1", VMeter::class) { fpp1 = it }
    private val fpp2Config   = addInstrument("Four-Point Probe Channel 2", VMeter::class) { fpp2 = it }
    private val tMeterConfig = addInstrument("Thermometer", TMeter::class) { tMeter = it }

    val delTime  get() = (1e3 * delTimeParam.value).toInt()
    val currents get() = currentParam.value
    val symI     get() = symIParam.value
    val holdG    get() = holdGParam.value
    val gateV    get() = gateVParam.value

    companion object {
        val SD_VOLTAGE     = Col("SD Voltage", "V")
        val SD_CURRENT     = Col("SD Current", "A")
        val SG_VOLTAGE     = Col("SG Voltage", "V")
        val SG_CURRENT     = Col("SG Current", "A")
        val FPP1_VOLTAGE   = Col("FPP 1 Voltage", "V")
        val FPP2_VOLTAGE   = Col("FPP 2 Voltage", "V")
        val FPP_VOLTAGE    = Col("FPP Voltage", "V") { (it[FPP1_VOLTAGE] - it[FPP2_VOLTAGE]) }
        val TEMPERATURE    = Col("Temperature", "K")
        val GROUND_CURRENT = Col("Ground Current", "A")
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            SD_VOLTAGE,
            SD_CURRENT,
            SG_VOLTAGE,
            SG_CURRENT,
            FPP1_VOLTAGE,
            FPP2_VOLTAGE,
            FPP_VOLTAGE,
            TEMPERATURE,
            GROUND_CURRENT
        )
        
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

        // Assert that source-drain must be connected
        val sdSMU = sdSMU!!
        val sgSMU = if (holdG) sgSMU else null

        results.setAttribute("Integration Time", "${fpp1?.integrationTime ?: fpp2?.integrationTime ?: sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        // Turn everything off before starting
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

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

    override fun onInterrupt() {}

    override fun onError() {

    }

}