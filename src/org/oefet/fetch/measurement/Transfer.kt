package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TransferPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TransferResult

class Transfer : FetChMeasurement("Transfer Measurement", "Transfer", "Transfer", Images.getImage("transfer.png")) {

    // Parameters
    val delTime    by userTimeInput("Basic", "Delay Time", 500)
    val sdVoltages by userInput("Source-Drain", "Voltage [V]", Range.manual(6, 60))
    val sgVoltages by userInput("Source-Gate", "Voltage [V]", Range.step(0, 60, 1).mirror())
    val sdOff      by userInput("Auto-Off", "Source-Drain Channel", true)
    val sgOff      by userInput("Auto-Off", "Source-Gate Channel", true)

    // Instruments
    val gdSMU  by optionalInstrument("Ground Channel (SPA)", SMU::class)
    val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    val sgSMU  by requiredInstrument("Source-Gate Channel", SMU::class)
    val fpp1   by optionalInstrument("Four-Point-Probe Channel 1", VMeter::class)
    val fpp2   by optionalInstrument("Four-Point-Probe Channel 2", VMeter::class)
    val tMeter by optionalInstrument("Thermometer", TMeter::class)

    companion object {
        val SET_SD_VOLTAGE = DoubleColumn("Set SD Voltage", "V")
        val SET_SG_VOLTAGE = DoubleColumn("Set SG Voltage", "V")
        val SD_VOLTAGE     = DoubleColumn("SD Voltage", "V")
        val SD_CURRENT     = DoubleColumn("SD Current", "A")
        val SG_VOLTAGE     = DoubleColumn("SG Voltage", "V")
        val SG_CURRENT     = DoubleColumn("SG Current", "A")
        val FPP_1          = DoubleColumn("Four Point Probe 1", "V")
        val FPP_2          = DoubleColumn("Four Point Probe 2", "V")
        val TEMPERATURE    = DoubleColumn("Temperature", "K")
        val GROUND_CURRENT = DoubleColumn("Ground Current", "A")
    }

    override fun createDisplay(data: ResultTable): TransferPlot {
        return TransferPlot(data)
    }

    override fun processResults(data: ResultTable): TransferResult {
        return TransferResult(data)
    }

    override fun getColumns(): Array<Column<*>> {

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

        if (sdOff) {
            sdSMU.turnOff()
            gdSMU?.turnOff()
        }

        if (sgOff) {
            sgSMU.turnOff()
        }

        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage  = sdVoltages.first()
        sgSMU.voltage  = sgVoltages.first()
        gdSMU?.voltage = 0.0

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSD in sdVoltages) {

            sdSMU.voltage = vSD

            for (vSG in sgVoltages) {

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

    }

    override fun onFinish() {

        if (sdOff) {
            runRegardless( { sdSMU.turnOff() }, { gdSMU?.turnOff() } )
        }

        if (sgOff) {
            runRegardless { sgSMU.turnOff() }
        }

        runRegardless ( { fpp1?.turnOff() }, { fpp2?.turnOff() } )

    }

    override fun onInterrupt() {

        Util.errLog.println("Transfer measurement interrupted.")

    }

    override fun onError() {

    }

}