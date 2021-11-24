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
    private val type     by userChoice("Source-Drain", "Type", "Current Sweep", "Voltage Sweep")
    private val values   by userInput("Source-Drain", "Values [A or V]", Range.linear(-10e-6, +10e-6, 11))
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

        const val TYPE_CURRENT = 0
        const val TYPE_VOLTAGE = 1

    }

    override fun createDisplay(data: ResultTable): FPPPlot {
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
        results.setAttribute("Used FPP", fpp1 != null || fpp2 != null || sdSMU.isFourProbeEnabled)
        results.setAttribute("Sweep Type", when(type) {
            TYPE_CURRENT -> "Current"
            TYPE_VOLTAGE -> "Voltage"
            else         -> "Unknown"
        })

        // Turn everything off before starting
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure all channel voltages/currents
        gdSMU?.voltage = 0.0
        sgSMU?.voltage = gateV

        when (type) {
            TYPE_CURRENT -> sdSMU.current = values.first()
            TYPE_VOLTAGE -> sdSMU.voltage = values.first()
        }

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
        for (value in values) {

            when (type) {
                TYPE_CURRENT -> sdSMU.current = value
                TYPE_VOLTAGE -> sdSMU.voltage = value
            }

            sleep(delTime)

            val sdVoltage   = sdSMU.voltage
            val fpp1Voltage = fpp1?.voltage ?: Double.NaN
            val fpp2Voltage = fpp2?.voltage ?: Double.NaN

            results.mapRow(
                SD_VOLTAGE     to sdVoltage,
                SD_CURRENT     to sdSMU.current,
                SG_VOLTAGE     to (sgSMU?.voltage ?: Double.NaN),
                SG_CURRENT     to (sgSMU?.current ?: Double.NaN),
                FPP1_VOLTAGE   to fpp1Voltage,
                FPP2_VOLTAGE   to fpp2Voltage,
                FPP_VOLTAGE    to determineVoltage(sdVoltage, fpp1Voltage, fpp2Voltage),
                TEMPERATURE    to (tMeter?.temperature ?: Double.NaN),
                GROUND_CURRENT to (gdSMU?.current ?: Double.NaN)
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