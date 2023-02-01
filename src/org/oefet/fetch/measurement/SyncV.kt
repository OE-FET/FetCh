package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.Icon
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.SyncPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.FetChResult

class SyncV : FetChMeasurement("Synced Voltage Measurement", "Sync", "VSync", Images.getImage("output.png")) {

    // Parameters
    val delTime  by userTimeInput("Basic", "Delay Time", 500)
    val voltages by userInput("Source-Drain", "Voltage [V]", Range.linear(0, 60))
    val symVSD   by userInput("Source-Drain", "Sweep Both Ways", true)
    val offset   by userInput("Source-Gate", "Offset [V]", 0.0)

    // Instruments
    val gdSMU  by optionalInstrument("Ground Channel (SPA)", SMU::class)
    val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    val sgSMU  by requiredInstrument("Source-Gate Channel", SMU::class)
    val fpp1   by optionalInstrument("Four-Point-Probe Channel 1", VMeter::class)
    val fpp2   by optionalInstrument("Four-Point-Probe Channel 2", VMeter::class)
    val tMeter by optionalInstrument("Thermometer", TMeter::class)

    companion object : Columns() {

        val SET_SD_VOLTAGE = decimalColumn("Set SD Voltage", "V")
        val SET_SG_VOLTAGE = decimalColumn("Set SG Voltage", "V")
        val SD_VOLTAGE     = decimalColumn("SD Voltage", "V")
        val SD_CURRENT     = decimalColumn("SD Current", "A")
        val SG_VOLTAGE     = decimalColumn("SG Voltage", "V")
        val SG_CURRENT     = decimalColumn("SG Current", "A")
        val FPP_1          = decimalColumn("Four Point Probe 1", "V")
        val FPP_2          = decimalColumn("Four Point Probe 2", "V")
        val TEMPERATURE    = decimalColumn("Temperature", "K")
        val GROUND_CURRENT = decimalColumn("Ground Current", "A")

    }

    override fun createDisplay(data: ResultTable): SyncPlot {
        return SyncPlot(data)
    }

    override fun processResults(data: ResultTable): FetChResult {

        return object : FetChResult("Synced Voltage Measurement", "VSync", Icon.CLOCK.blackImage, data) {

            override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {
                return emptyList()
            }

        }

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

            results.mapRow(
                SET_SD_VOLTAGE to vSD,
                SET_SG_VOLTAGE to vSG,
                SD_VOLTAGE     to sdSMU.voltage,
                SD_CURRENT     to sdSMU.current,
                SG_VOLTAGE     to sgSMU.voltage,
                SG_CURRENT     to sgSMU.current,
                FPP_1          to (fpp1?.voltage ?: Double.NaN),
                FPP_2          to (fpp2?.voltage ?: Double.NaN),
                TEMPERATURE    to (tMeter?.temperature ?: Double.NaN),
                GROUND_CURRENT to (gdSMU?.current ?: Double.NaN)
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
