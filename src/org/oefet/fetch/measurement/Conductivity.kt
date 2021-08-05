package org.oefet.fetch.measurement

import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.results.CondResult
import java.lang.Double.min

class Conductivity : FetChMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity") {

    // User input parameters
    private val delTime  by userInput("Basic", "Delay Time [s]", 1.0) map { (it * 1e3).toInt() }
    private val currents by userInput("Source-Drain", "Current [A]", Range.linear(-10e-6, +10e-6, 11))
    private val symI     by userInput("Source-Drain", "Sweep Both Ways", false)
    private val holdG    by userInput("Source-Gate", "Active", false)
    private val gateV    by userInput("Source-Gate", "Voltage [V]", 50.0)

    // Instruments
    private val gdSMU  by optionalInstrument("Ground Channel (SPA)", SMU::class)
    private val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    private val sgSMU  by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { holdG }
    private val fpp1   by optionalInstrument("Four-Point Probe Channel 1", VMeter::class)
    private val fpp2   by optionalInstrument("Four-Point Probe Channel 2", VMeter::class)
    private val tMeter by optionalInstrument("Thermometer", TMeter::class)

    companion object {
        val SD_VOLTAGE     = DoubleColumn("SD Voltage", "V")
        val SD_CURRENT     = DoubleColumn("SD Current", "A")
        val SG_VOLTAGE     = DoubleColumn("SG Voltage", "V")
        val SG_CURRENT     = DoubleColumn("SG Current", "A")
        val FPP1_VOLTAGE   = DoubleColumn("FPP 1 Voltage", "V")
        val FPP2_VOLTAGE   = DoubleColumn("FPP 2 Voltage", "V")
        val FPP_VOLTAGE    = DoubleColumn("FPP Voltage", "V")
        val TEMPERATURE    = DoubleColumn("Temperature", "K")
        val GROUND_CURRENT = DoubleColumn("Ground Current", "A")
    }

    override fun createPlot(data: ResultTable): FPPPlot {
        return FPPPlot(data)
    }

    override fun processResults(data: ResultTable): CondResult {
        return CondResult(data)
    }

    override fun getColumns(): Array<Column<*>> {

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

    override fun run(results: ResultTable) {

        // Assert that source-drain must be connected
        val sdSMU = sdSMU
        val sgSMU = if (holdG) sgSMU else null

        val intTime = when {

            fpp1 != null && fpp2 != null -> min(fpp1!!.integrationTime, fpp2!!.integrationTime)
            fpp1 != null                 -> fpp1!!.integrationTime
            fpp2 != null                 -> fpp2!!.integrationTime
            else                         -> sdSMU.integrationTime

        }

        results.setAttribute("Integration Time", "$intTime s")
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

        val determineVoltage: (Double, Double, Double) -> Double = when {

            fpp1 != null && fpp2 != null -> { _, f1, f2 -> f2 - f1 }
            fpp1 != null                 -> { _, f1, _  -> f1 }
            fpp2 != null                 -> { _, _, f2  -> f2 }
            else                         -> { sd, _, _  -> sd }

        }

        // Sweep current
        for (current in if (symI) currents.mirror() else currents) {

            sdSMU.current = current

            sleep(delTime)

            val sdVoltage   = sdSMU.voltage
            val fpp1Voltage = fpp1?.voltage ?: Double.NaN
            val fpp2Voltage = fpp2?.voltage ?: Double.NaN

            results.addData(
                sdVoltage,
                sdSMU.current,
                sgSMU?.voltage ?: Double.NaN,
                sgSMU?.current ?: Double.NaN,
                fpp1Voltage,
                fpp2Voltage,
                determineVoltage(sdVoltage, fpp1Voltage, fpp2Voltage),
                tMeter?.temperature ?: Double.NaN,
                gdSMU?.current ?: Double.NaN
            )

        }

    }

    override fun onFinish() {

        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun onInterrupt() {}

    override fun onError() {}

}