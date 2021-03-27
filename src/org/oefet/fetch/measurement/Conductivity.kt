package org.oefet.fetch.measurement

import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.CondResult
import java.lang.Double.min

class Conductivity : FMeasurement("Conductivity Measurement", "Cond", "FPP Conductivity") {

    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 1.0)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -10e-6, +10e-6, 11)
    private val symIParam    = BooleanParameter("Source-Drain", "Sweep Both Ways", null, false)
    private val holdGParam   = BooleanParameter("Source-Gate", "Active", null, false)
    private val gateVParam   = DoubleParameter("Source-Gate", "Voltage", "V", 50.0)

    private val gdSMUConfig  = addOptionalInstrument("Ground Channel (SPA)", SMU::class) { gdSMU = it }
    private val sdSMUConfig  = addOptionalInstrument("Source-Drain Channel", SMU::class) { sdSMU = it }
    private val sgSMUConfig  = addOptionalInstrument("Source-Gate Channel", SMU::class) { sgSMU = it }
    private val fpp1Config   = addOptionalInstrument("Four-Point Probe Channel 1", VMeter::class) { fpp1 = it }
    private val fpp2Config   = addOptionalInstrument("Four-Point Probe Channel 2", VMeter::class) { fpp2 = it }
    private val tMeterConfig = addOptionalInstrument("Thermometer", TMeter::class) { tMeter = it }

    val delTime  get() = (1e3 * delTimeParam.value).toInt()
    val currents get() = currentParam.value
    val symI     get() = symIParam.value
    val holdG    get() = holdGParam.value
    val gateV    get() = gateVParam.value

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

        if (sdSMU == null) {
            errors += "SD channel not configured"
        }

        if (holdG && sgSMU == null) {
            errors += "SG channel not configured"
        }

        return errors

    }

    override fun run(results: ResultTable) {

        // Assert that source-drain must be connected
        val sdSMU = sdSMU!!
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
        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun onInterrupt() {}

    override fun onError() {}

}