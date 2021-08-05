package org.oefet.fetch.measurement

import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.results.OutputResult

class Output : FetChMeasurement("Output Measurement", "Output", "Output") {

    // Parameters
    val delTime    by userInput("Basic", "Delay Time [s]", 0.5) map { it.toMSec() }
    val sdVoltages by userInput("Source-Drain", "Voltage [V]", Range.step(0, 60, 1).mirror())
    val sgVoltages by userInput("Source-Gate", "Voltage [V]", Range.step(0, 60, 10))

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

    override fun createPlot(data: ResultTable): OutputPlot {
        return OutputPlot(data)
    }

    override fun processResults(data: ResultTable): OutputResult {
        return OutputResult(data)
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

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
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

        for (vSG in sgVoltages) {

            sgSMU.voltage = vSG

            for (vSD in sdVoltages) {

                sdSMU.voltage = vSD

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

                checkPoint()

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

}
