package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.TransferPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TransferResult

class Transfer : FMeasurement("Transfer Measurement", "Transfer", "Transfer") {

    // Parameters
    val delTime    by input("Basic", "Delay Time [s]", 0.5) { (it * 1000.0).toInt() }
    val sdVoltages by input("Source-Drain", "Voltage [V]", Range.step(0, 60, 1))
    val symVSD     by input("Source-Drain", "Sweep Both Ways", true)
    val sgVoltages by input("Source-Gate", "Voltage [V]", Range.step(0, 60, 10))

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

    override fun createPlot(data: ResultTable): TransferPlot {
        return TransferPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TransferResult {
        return TransferResult(data, extra)
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Output.SET_SD_VOLTAGE,
            Output.SET_SG_VOLTAGE,
            Output.SD_VOLTAGE,
            Output.SD_CURRENT,
            Output.SG_VOLTAGE,
            Output.SG_CURRENT,
            Output.FPP_1,
            Output.FPP_2,
            Output.TEMPERATURE,
            Output.GROUND_CURRENT
        )

    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = sdVoltages.first()
        sgSMU.voltage = sgVoltages.first()
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

                results.addData(
                    vSD, vSG,
                    sdSMU.voltage, sdSMU.current,
                    sgSMU.voltage, sgSMU.current,
                    fpp1?.voltage ?: Double.NaN, fpp2?.voltage ?: Double.NaN,
                    tMeter?.temperature ?: Double.NaN,
                    gdSMU?.current ?: Double.NaN
                )

            }

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

        Util.errLog.println("Transfer measurement interrupted.")

    }

    override fun onError() {

    }

}