package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.Icon
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.SyncPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.quantities.SimpleQuantity
import org.oefet.fetch.results.FetChResult
import org.oefet.fetch.results.OutputResult

class SyncV : FetChMeasurement("Synced Voltage Measurement", "Sync", "VSync") {

    // Parameters
    val delTime  by input("Basic", "Delay Time [s]", 0.5) map { (it * 1e3).toInt() }
    val voltages by input("Source-Drain", "Voltage [V]", Range.linear(0, 60))
    val symVSD   by input("Source-Drain", "Sweep Both Ways", true)
    val offset   by input("Source-Gate", "Offset [V]", 0.0)

    // Instruments
    val gdSMU  by optionalConfig("Ground Channel (SPA)", SMU::class)
    val sdSMU  by requiredConfig("Source-Drain Channel", SMU::class)
    val sgSMU  by requiredConfig("Source-Gate Channel", SMU::class)
    val fpp1   by optionalConfig("Four-Point-Probe Channel 1", VMeter::class)
    val fpp2   by optionalConfig("Four-Point-Probe Channel 2", VMeter::class)
    val tMeter by optionalConfig("Thermometer", TMeter::class)

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

    override fun processResults(data: ResultTable, extra: List<Quantity>): FetChResult {

        return object : FetChResult("Synced Voltage Measurement", "VSync", Icon.CLOCK.blackImage, data, extra) {

            override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
                return emptyList()
            }

        }

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

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        val voltages = if (symVSD) voltages.mirror() else voltages

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = voltages.first()
        sgSMU.voltage = voltages.first() + offset
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

        runRegardless { sdSMU.turnOff() }
        runRegardless { sgSMU.turnOff() }
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
