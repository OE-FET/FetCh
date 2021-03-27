package org.oefet.fetch.measurement

import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.CondResult
import java.lang.Double.min

class Conductivity : FMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity") {

    private val gdSMU  by optionalConfig("Ground Channel (SPA)", SMU::class)
    private val sdSMU  by requiredConfig("Source-Drain Channel", SMU::class)
    private val sgSMU  by optionalConfig("Source-Gate Channel", SMU::class)
    private val fpp1   by optionalConfig("Four-Point Probe Channel 1", VMeter::class)
    private val fpp2   by optionalConfig("Four-Point Probe Channel 2", VMeter::class)
    private val tMeter by optionalConfig("Thermometer", TMeter::class)

    private val delTime  by input("Basic", "Delay Time [s]", 1.0) { (it * 1e3).toInt() }
    private val currents by input("Source-Drain", "Current [A]", Range.linear(-10e-6, +10e-6, 11))
    private val symI     by input("Source-Drain", "Sweep Both Ways", false)
    private val holdG    by input("Source-Gate", "Active", false)
    private val gateV    by input("Source-Gate", "Voltage [V]", 50.0)

    companion object {
        val SD_VOLTAGE     = Col("SD Voltage", "V")
        val SD_CURRENT     = Col("SD Current", "A")
        val SG_VOLTAGE     = Col("SG Voltage", "V")
        val SG_CURRENT     = Col("SG Current", "A")
        val FPP1_VOLTAGE   = Col("FPP 1 Voltage", "V")
        val FPP2_VOLTAGE   = Col("FPP 2 Voltage", "V")
        val FPP_VOLTAGE    = Col("FPP Voltage", "V")
        val TEMPERATURE    = Col("Temperature", "K")
        val GROUND_CURRENT = Col("Ground Current", "A")
    }

    override fun createPlot(data: ResultTable): FPPPlot {
        return FPPPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): CondResult {
        return CondResult(data, extra)
    }

    override fun getColumns(): Array<Col> {

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

    override fun checkForErrors(): List<String> {

        val errors = ArrayList<String>()

        if (holdG && sgSMU == null) {
            errors += "SG channel not configured"
        }

        return errors

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