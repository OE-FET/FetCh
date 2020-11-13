package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections

class Conductivity : FMeasurement() {

    override val type = "FPP Conductivity"

    private val labelParam   = StringParameter("Basic", "Name", null, "Cond")
    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 1.0)
    private val minIParam    = DoubleParameter("Source-Drain", "Start", "A", 0.0)
    private val maxIParam    = DoubleParameter("Source-Drain", "Stop", "A", 10e-6)
    private val numIParam    = IntegerParameter("Source-Drain", "No. Steps", null, 11)
    private val symIParam    = BooleanParameter("Source-Drain", "Sweep Both Ways", null, false)
    private val holdGParam   = BooleanParameter("Source-Gate", "Active", null, false)
    private val gateVParam   = DoubleParameter("Source-Gate", "Voltage", "V", 50.0)

    val gdSMUConfig = addInstrument(Configurator.SMU("Ground Channel (SPA)", Connections))
    val sdSMUConfig = addInstrument(Configurator.SMU("Source-Drain Channel", Connections))
    val sgSMUConfig = addInstrument(Configurator.SMU("Source-Gate Channel", Connections))
    val fpp1Config  = addInstrument(Configurator.VMeter("Four-Point-Probe Channel 1", Connections))
    val fpp2Config  = addInstrument(Configurator.VMeter("Four-Point-Probe Channel 2", Connections))

    val intTime get() = intTimeParam.value
    val delTime get() = (1e3 * delTimeParam.value).toInt()
    val minI    get() = minIParam.value
    val maxI    get() = maxIParam.value
    val numI    get() = numIParam.value
    val symI    get() = symIParam.value
    val holdG   get() = holdGParam.value
    val gateV   get() = gateVParam.value

    override fun setLabel(value: String?) {
        labelParam.value = value
    }

    override fun getLabel(): String {
        return labelParam.value
    }

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        sgSMU    = sgSMUConfig.get()
        fpp1     = fpp1Config.get()
        fpp2     = fpp2Config.get()

        super.loadInstruments()

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
        sdSMU.integrationTime = intTime
        sgSMU?.integrationTime = intTime
        fpp1?.integrationTime = intTime
        fpp2?.integrationTime = intTime

        // Configure all channel voltages/currents
        gdSMU?.voltage = 0.0
        sgSMU?.voltage = gateV
        sdSMU.current = minI

        // Enable all channels
        gdSMU?.turnOn()
        sdSMU.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()
        sgSMU?.turnOn()

        // Sweep current
        for (current in if (symI) Range.linear(minI, maxI, numI).mirror() else Range.linear(minI, maxI, numI)) {

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

    override fun getName(): String = "Conductivity Measurement"

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