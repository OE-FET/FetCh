package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.enums.Icon
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.results.CondResult
import java.lang.Double.min

class Conductivity : FetChMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity", Icon.ELECTRICITY.blackImage) {

    // User input parameters
    private val delTime by userTimeInput("Basic", "Delay Time [s]", 1000)
    private val type    by userChoice("Source-Drain", "Type", "Current Sweep", "Voltage Sweep")
    private val values  by userInput("Source-Drain", "Values [A or V]", Range.linear(-10e-6, +10e-6, 11))
    private val holdG   by userInput("Source-Gate", "Active", false)
    private val gateV   by userInput("Source-Gate", "Voltage [V]", 50.0)

    // Instruments
    private val gdSMU  by optionalInstrument("Ground Channel (SPA)", VSource::class)
    private val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    private val sgSMU  by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { holdG }
    private val fpp1   by optionalInstrument("Four-Point Probe Channel 1", VMeter::class)
    private val fpp2   by optionalInstrument("Four-Point Probe Channel 2", VMeter::class)
    private val tMeter by optionalInstrument("Thermometer", TMeter::class)

    // Columns in table of results
    companion object : Columns() {

        val SD_VOLTAGE     = decimalColumn("SD Voltage", "V")
        val SD_CURRENT     = decimalColumn("SD Current", "A")
        val SG_VOLTAGE     = decimalColumn("SG Voltage", "V")
        val SG_CURRENT     = decimalColumn("SG Current", "A")
        val FPP1_VOLTAGE   = decimalColumn("FPP 1 Voltage", "V")
        val FPP2_VOLTAGE   = decimalColumn("FPP 2 Voltage", "V")
        val FPP_VOLTAGE    = decimalColumn("FPP Voltage", "V")
        val TEMPERATURE    = decimalColumn("Temperature", "K")
        val GROUND_CURRENT = decimalColumn("Ground Current", "A")

        const val TYPE_CURRENT = 0
        const val TYPE_VOLTAGE = 1

    }

    override fun createDisplay(data: ResultTable): FPPPlot {
        return FPPPlot(data)
    }

    override fun processResults(data: ResultTable): CondResult {
        return CondResult(data)
    }

    override fun run(results: ResultTable) {

        // Assert that source-drain must be connected
        val sdSMU = sdSMU
        val sgSMU = if (holdG) sgSMU else null
        val gdSMU = gdSMU

        // Record the most limiting integration time as the integration time for the whole measurement
        val intTime = when {

            fpp1 != null && fpp2 != null -> min(fpp1!!.integrationTime, fpp2!!.integrationTime)
            fpp1 != null                 -> fpp1!!.integrationTime
            fpp2 != null                 -> fpp2!!.integrationTime
            else                         -> sdSMU.integrationTime

        }

        // If a fpp voltmeter is set, or the source-drain SMU is in 4PP mode, then this is a 4PP measurement
        val isFPP = fpp1 != null || fpp2 != null || sdSMU.isFourProbeEnabled

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Used FPP", isFPP)

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

        // Determine how we should calculate the 4PP/2PP voltage for this measurement (1)
        val determineVoltage: (Double, Double, Double) -> Double = when {

            fpp1 != null && fpp2 != null -> { _, f1, f2 -> f2 - f1 } // If two 4PP voltmeters, take difference
            fpp1 != null                 -> { _, f1, _  -> f1 }      // If one 4PP voltmeter, use value directly
            fpp2 != null                 -> { _, _, f2  -> f2 }      // If one 4PP voltmeter, use value directly
            else                         -> { sd, _, _  -> sd }      // If no 4PP voltmeters, use source-drain voltage

        }

        // Sweep current
        for (value in values) {

            // Choose whether to set current or voltage based on set sweep mode
            when (type) {
                TYPE_CURRENT -> sdSMU.current = value
                TYPE_VOLTAGE -> sdSMU.voltage = value
            }

            // Wait for the set delay time
            sleep(delTime)

            // Record voltages to pass to previously determined voltage determination function (1)
            val sdVoltage   = sdSMU.voltage
            val fpp1Voltage = fpp1?.voltage ?: Double.NaN
            val fpp2Voltage = fpp2?.voltage ?: Double.NaN

            // Map all results to a new row of the table of results
            results.mapRow(
                SD_VOLTAGE     to sdVoltage,
                SD_CURRENT     to sdSMU.current,
                SG_VOLTAGE     to (sgSMU?.voltage ?: Double.NaN),
                SG_CURRENT     to (sgSMU?.current ?: Double.NaN),
                FPP1_VOLTAGE   to fpp1Voltage,
                FPP2_VOLTAGE   to fpp2Voltage,
                FPP_VOLTAGE    to determineVoltage(sdVoltage, fpp1Voltage, fpp2Voltage),
                TEMPERATURE    to (tMeter?.temperature ?: Double.NaN),
                GROUND_CURRENT to (if (gdSMU is ISource) gdSMU.current else Double.NaN)
            )

        }

    }

    override fun onFinish() {

        runRegardless(
            { gdSMU?.turnOff() },
            { sdSMU.turnOff() },
            { sgSMU?.turnOff() },
            { fpp1?.turnOff() },
            { fpp2?.turnOff() }
        )

    }

}