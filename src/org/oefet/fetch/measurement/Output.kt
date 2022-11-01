package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.maths.Range
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.OutputResult
import kotlin.Double.Companion.NaN

class Output : FetChMeasurement("Output Measurement", "Output", "Output", Images.getImage("output.png")) {

    // Parameters
    val delTime    by userTimeInput("Basic", "Delay Time", 500)
    val sdVoltages by userInput("Source-Drain", "Voltage [V]", Range.step(0, 60, 1).mirror())
    val sgVoltages by userInput("Source-Gate", "Voltage [V]", Range.step(0, 60, 10))
    val sdOff      by userInput("Auto-Off", "Source-Drain Channel", true)
    val sgOff      by userInput("Auto-Off", "Source-Gate Channel", true)

    // Instruments
    val gdSMU  by optionalInstrument("Ground Channel (SPA)", VSource::class)
    val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    val sgSMU  by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { sgVoltages.size() > 1 }
    val fpp1   by optionalInstrument("Four-Point-Probe Channel 1", VMeter::class)
    val fpp2   by optionalInstrument("Four-Point-Probe Channel 2", VMeter::class)
    val tMeter by optionalInstrument("Thermometer", TMeter::class)

    companion object Columns {

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

        val COLUMN_ORDER   = arrayOf(
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

    override fun createDisplay(data: ResultTable): OutputPlot {
        return OutputPlot(data)
    }

    override fun processResults(data: ResultTable): OutputResult {
        return OutputResult(data)
    }

    override fun getColumns(): Array<DoubleColumn> = COLUMN_ORDER

    override fun run(results: ResultTable) {

        val gdSMU = gdSMU

        // Record the integration and delay times
        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        // If we are allowed to turn off the SD channel, then start off with it off
        if (sdOff) {
            sdSMU.turnOff()
            gdSMU?.turnOff()
        }

        // If we are allowed to turn off the SG channel, then start off with it off
        if (sgOff) {
            sgSMU?.turnOff()
        }

        // Configure initial source modes
        sdSMU.voltage  = sdVoltages.first()
        sgSMU?.voltage = sgVoltages.first()
        gdSMU?.voltage = 0.0

        // Make sure everything that is configured for use is now turned on
        sdSMU.turnOn()
        sgSMU?.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSG in sgVoltages) {

            // Set source-gate voltage
            sgSMU?.voltage = vSG

            for (vSD in sdVoltages) {

                // Set source-drain voltage and wait for the delay time
                sdSMU.voltage = vSD
                sleep(delTime)

                // Measure and record values in results table
                results.mapRow(
                    SET_SD_VOLTAGE to vSD,
                    SET_SG_VOLTAGE to vSG,
                    SD_VOLTAGE     to sdSMU.voltage,
                    SD_CURRENT     to sdSMU.current,
                    SG_VOLTAGE     to (sgSMU?.voltage ?: vSG),
                    SG_CURRENT     to (sgSMU?.current ?: NaN),
                    FPP_1          to (fpp1?.voltage ?: NaN),
                    FPP_2          to (fpp2?.voltage ?: NaN),
                    TEMPERATURE    to (tMeter?.temperature ?: NaN),
                    GROUND_CURRENT to (if (gdSMU is ISource) gdSMU.current else NaN)
                )

                // Check if we have been instructed to stop
                checkPoint()

            }

        }

    }

    override fun onFinish() {

        if (sdOff) {
            runRegardless( { sdSMU.turnOff() }, { gdSMU?.turnOff() } )
        }

        if (sgOff) {
            runRegardless { sgSMU?.turnOff() }
        }

        runRegardless ( { fpp1?.turnOff() }, { fpp2?.turnOff() } )

    }

}
