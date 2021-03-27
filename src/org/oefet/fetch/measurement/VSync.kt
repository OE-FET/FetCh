package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.SyncPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.OutputResult

class VSync : FMeasurement("Synced Voltage Measurement", "Sync", "VSync") {

    private val paramDelTime = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val paramMinVSD  = DoubleParameter("Source-Drain", "Start", "V", 0.0)
    private val paramMaxVSD  = DoubleParameter("Source-Drain", "Stop", "V", 60.0)
    private val paramNumVSD  = IntegerParameter("Source-Drain", "No. Steps", null, 7)
    private val paramSymVSD  = BooleanParameter("Source-Drain", "Sweep Both Ways", null, true)
    private val paramOffset  = DoubleParameter("Source-Gate", "Offset", "V", 0.0)

    private val gdSMUConfig  = addOptionalInstrument("Ground Channel (SPA)", SMU::class) { gdSMU = it }
    private val sdSMUConfig  = addOptionalInstrument("Source-Drain Channel", SMU::class) { sdSMU = it }
    private val sgSMUConfig  = addOptionalInstrument("Source-Gate Channel", SMU::class) { sgSMU = it }
    private val fpp1Config   = addOptionalInstrument("Four-Point Probe Channel 1", VMeter::class) { fpp1 = it }
    private val fpp2Config   = addOptionalInstrument("Four-Point Probe Channel 2", VMeter::class) { fpp2 = it }
    private val tMeterConfig = addOptionalInstrument("Thermometer", TMeter::class) { tMeter = it }

    val delTime get() = (paramDelTime.value * 1000).toInt()
    val minVSD  get() = paramMinVSD.value
    val maxVSD  get() = paramMaxVSD.value
    val numVSD  get() = paramNumVSD.value
    val symVSD  get() = paramSymVSD.value
    val offset  get() = paramOffset.value

    companion object {
        val SET_SD_VOLTAGE = Col("Set SD Voltage", "V")
        val SET_SG_VOLTAGE = Col("Set SG Voltage", "V")
        val SD_VOLTAGE     = Col("SD Voltage", "V")
        val SD_CURRENT     = Col("SD Current", "A")
        val SG_VOLTAGE     = Col("SG Voltage", "V")
        val SG_CURRENT     = Col("SG Current", "A")
        val FPP_1          = Col("Four Point Probe 1", "V")
        val FPP_2          = Col("Four Point Probe 2", "V")
        val TEMPERATURE    = Col("Temperature", "K")
        val GROUND_CURRENT = Col("Ground Current", "A")
    }

    override fun createPlot(data: ResultTable): SyncPlot {
        return SyncPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): OutputResult {
        return OutputResult(data, extra)
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            SET_SD_VOLTAGE,
            SET_SG_VOLTAGE,
            SD_VOLTAGE,
            SD_CURRENT,
            SG_VOLTAGE,
            SG_CURRENT,
            FPP_1,
            FPP_2,
            TEMPERATURE,
            GROUND_CURRENT
        )

    }

    override fun checkForErrors() : List<String> {

        val errors = ArrayList<String>()

        if (sdSMU == null) errors += "SD channel not configured"
        if (sgSMU == null) errors += "SG channel not configured"

        return errors

    }

    override fun run(results: ResultTable) {

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        val voltages = if (symVSD) {
            Range.linear(minVSD, maxVSD, numVSD).mirror()
        } else {
            Range.linear(minVSD, maxVSD, numVSD)
        }

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSD + offset
        gdSMU?.voltage = 0.0

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSD in voltages) {

            val vSG = vSD + offset

            sdSMU.voltage = vSD
            sgSMU.voltage = vSG

            sleep(delTime)

            results.addData(
                vSD,
                vSG,
                sdSMU.voltage,
                sdSMU.current,
                sgSMU.voltage,
                sgSMU.current,
                fpp1?.voltage ?: Double.NaN,
                fpp2?.voltage ?: Double.NaN,
                tMeter?.temperature ?: Double.NaN,
                gdSMU?.current ?: Double.NaN
            )

        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun onInterrupt() {

        Util.errLog.println("Synced voltage measurement interrupted.")

    }

    override fun onError() {

    }

}
