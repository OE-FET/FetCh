package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.time.Duration
import kotlin.math.abs

class FPPMeasurement : FetChMeasurement() {

    private var gdSMU : SMU?    = null
    private var sdSMU : SMU?    = null
    private var sgSMU : SMU?    = null
    private var fpp1  : VMeter? = null
    private var fpp2  : VMeter? = null
    private var tm    : TMeter? = null

    override val type = "FPP Conductivity"

    val label   = StringParameter("Basic","Name",null, "FPPCond")
    val intTime = DoubleParameter("Basic","Integration Time","s", 20e-3)
    val delTime = DoubleParameter("Basic","Delay Time","s", 1.0)
    val minI    = DoubleParameter("Source-Drain", "Start", "A",0.0)
    val maxI    = DoubleParameter("Source-Drain", "Stop", "A",10e-6)
    val numI    = IntegerParameter("Source-Drain", "No. Steps", null, 11)
    val symI    = BooleanParameter("Source-Drain", "Sweep Both Ways", null, false)
    val holdG   = BooleanParameter("Source-Gate", "Active", null, false)
    val gateV   = DoubleParameter("Source-Gate","Voltage","V", 50.0)

    override fun loadInstruments(instruments: Instruments) {

        sdSMU = instruments.sdSMU
        sgSMU = instruments.sgSMU
        gdSMU = instruments.gdSMU
        fpp1  = instruments.fpp1
        fpp2  = instruments.fpp2
        tm    = instruments.tm

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun run(results: ResultTable) {

        val minI    = minI.value
        val maxI    = maxI.value
        val numI    = numI.value
        val symI    = symI.value
        val holdG   = holdG.value
        val gateV   = gateV.value
        val intTime = intTime.value
        val delTime = (delTime.value * 1000.0).toLong()

        val errors = ArrayList<String>()

        if (sdSMU == null) {
            errors += "SD channel not configured"
        }

        if (fpp1 == null) {
            errors += "FPP channel not configured"
        }

        if (holdG && sgSMU == null) {
            errors += "SG channel not configured"
        }

        if (errors.isNotEmpty()) throw Exception(errors.joinToString(", "))

        val sdSMU = sdSMU!!
        val fpp1  = fpp1!!
        val gdSMU = gdSMU
        val sgSMU = if (holdG) sgSMU else null
        val fpp2  = fpp2

        // Turn everything off before starting
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        fpp1.turnOff()
        fpp2?.turnOff()

        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime  = intTime
        sgSMU?.integrationTime = intTime
        fpp1.integrationTime   = intTime
        fpp2?.integrationTime  = intTime

        // Configure all channel voltages/currents
        gdSMU?.voltage = 0.0
        sgSMU?.voltage = gateV
        sdSMU.current  = minI

        // Enable all channels
        gdSMU?.turnOn()
        sdSMU.turnOn()
        fpp1.turnOn()
        fpp2?.turnOn()
        sgSMU?.turnOn()

        // Sweep current
        for (current in if (symI) Range.linear(minI, maxI, numI).mirror() else Range.linear(minI, maxI, numI)) {

            sdSMU.current = current
            sleep(delTime)

            results.addData(
                sdSMU.voltage, sdSMU.current,
                sgSMU?.voltage ?: Double.NaN, sgSMU?.current ?: Double.NaN,
                fpp1.voltage,
                fpp2?.voltage ?: 0.0,
                tm?.temperature ?: Double.NaN,
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

    override fun getLabel(): String {
        return label.value
    }

    override fun getName(): String = "FPP Conductivity Measurement"

    override fun getColumns(): Array<Col> = arrayOf(
        Col("SD Voltage", "V"),
        Col("SD Current", "A"),
        Col("SG Voltage", "V"),
        Col("SG Current", "A"),
        Col("FPP 1 Voltage", "V"),
        Col("FPP 2 Voltage", "V"),
        Col("FPP Voltage", "V") { abs( it[FPP2_VOLTAGE] - it[FPP1_VOLTAGE]) },
        Col("Temperature", "K"),
        Col("Ground Current", "A")
    )

    override fun onInterrupt() {}

    companion object {
        const val SD_VOLTAGE     = 0
        const val SD_CURRENT     = 1
        const val SG_VOLTAGE     = 2
        const val SG_CURRENT     = 3
        const val FPP1_VOLTAGE   = 4
        const val FPP2_VOLTAGE   = 5
        const val FPP_VOLTAGE    = 6
        const val TEMPERATURE    = 7
        const val GROUND_CURRENT = 8
    }

}