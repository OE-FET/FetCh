package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.control.Repeat
import jisa.devices.interfaces.*
import jisa.devices.Configuration
import jisa.enums.Coupling
import jisa.enums.Input
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.gui.Plot
import jisa.maths.Range
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.gui.tabs.Connections
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ACHallResult
import org.oefet.fetch.results.ResultFile
import java.time.Duration
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class ACHall : FMeasurement("AC Hall Measurement", "ACHall", "AC Hall") {

    private val intTimeParam   = DoubleParameter("Basic", "Integration Time", "s", 100.0)
    private val delTimeParam   = DoubleParameter("Basic", "Delay Time", "s", Duration.ofMinutes(10).toSeconds().toDouble())
    private val repeatsParam   = IntegerParameter("Basic", "Repeats", null, 600)
    private val paGainParam    = DoubleParameter("Basic", "Pre-Amp Gain", null, 1.0)
    private val exGainParam    = DoubleParameter("Basic", "Extra Gain", null, 10.0)
    private val rmsFieldParam  = DoubleParameter("Magnets", "RMS Field Strength", "T", 0.666 / sqrt(2.0))
    private val frequencyParam = RangeParameter("Magnets", "Frequency", "Hz", 1.5, 1.5, 1)
    private val spinParam      = DoubleParameter("Magnets", "Spin-Up Time", "s", 600.0)
    private val currentParam   = RangeParameter("Source-Drain", "Current", "A", -50e-6, 50e-6, 5)
    private val gateParam      = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 0.0, 1)

    private val gdSMUConfig    = addInstrument("Ground Channel (SPA)", SMU::class) { gdSMU = it }
    private val sdSMUConfig    = addInstrument("Source-Drain Channel", SMU::class) { sdSMU = it }
    private val sgSMUConfig    = addInstrument("Source-Gate Channel", SMU::class)  { sgSMU = it }
    private val dcPowerConfig  = addInstrument("Motor Power Supply", DCPower::class) { dcPower = it }
    private val lockInConfig   = addInstrument("Lock-In Amplifier", DPLockIn::class) { lockIn = it }
    private val preAmpConfig   = addInstrument("Voltage Pre-Amplifier", VPreAmp::class) { preAmp = it }
    private val tMeterConfig   = addInstrument("Thermometer", TMeter::class) { tMeter = it }

    val intTime     get() = intTimeParam.value
    val delTime     get() = (1e3 * delTimeParam.value).toInt()
    val repeats     get() = repeatsParam.value
    val paGain      get() = paGainParam.value
    val exGain      get() = exGainParam.value
    val rmsField    get() = rmsFieldParam.value
    val frequencies get() = frequencyParam.value
    val spin        get() = (1e3 * spinParam.value).toInt()
    val currents    get() = currentParam.value
    val gates       get() = gateParam.value
    val totGain     get() = paGain * exGain

    companion object {

        val SD_VOLTAGE   = Col("SD Voltage", "V")
        val SD_CURRENT   = Col("SD Current", "A")
        val SG_VOLTAGE   = Col("SG Voltage", "V")
        val SG_CURRENT   = Col("SG Current", "A")
        val RMS_FIELD    = Col("RMS Field Strength", "T")
        val FREQUENCY    = Col("Field Frequency", "Hz")
        val X_VOLTAGE    = Col("X Voltage", "V")
        val X_ERROR      = Col("X Error", "V")
        val Y_VOLTAGE    = Col("Y Voltage", "V")
        val Y_ERROR      = Col("Y Error", "V")
        val HALL_VOLTAGE = Col("Hall Voltage", "V")
        val HALL_ERROR   = Col("Hall Voltage Error", "V")
        val TEMPERATURE  = Col("Temperature", "K")

    }

    override fun createPlot(data: ResultTable): ACHallPlot {
        return ACHallPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): ACHallResult {
        return ACHallResult(data, extra)
    }

    override fun loadInstruments() {

        super.loadInstruments()

        fControl = if (lockIn != null && dcPower != null) {
            FControl(lockIn!!, dcPower!!)
        } else {
            null
        }

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (sdSMU == null) {
            errors += "No SD channel configured"
        }

        if (sgSMU == null && !(gates.min() == 0.0 && gates.max() == 0.0)) {
            errors += "No SG channel configured"
        }

        if (lockIn == null) {
            errors += "No lock-in configured"
        }

        if (preAmp == null) {
            errors += "No pre-amp configured"
        }

        if (fControl == null) {
            errors += "No frequency control available"
        }

        return errors

    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())
        results.setAttribute("Pre-Amp Gain", paGain)
        results.setAttribute("Extra Pre-Amp Gain", exGain)

        // Assert that all required instruments must be connected
        val sdSMU    = sdSMU!!
        val lockIn   = lockIn!!
        val preAmp   = preAmp!!
        val fControl = fControl!!

        sdSMU.turnOff()
        sgSMU?.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp.coupling = Coupling.AC
        preAmp.input    = Input.DIFF
        preAmp.gain     = paGain

        // Initialise the SMUs
        sdSMU.current  = currents.first()
        sgSMU?.voltage = gates.first()
        gdSMU?.voltage = 0.0

        // Configure the lock-in amplifier
        lockIn.refMode      = LockIn.RefMode.EXTERNAL
        lockIn.timeConstant = intTime

        // Set everything going
        sdSMU.turnOn()
        sgSMU?.turnOn()
        gdSMU?.turnOn()
        fControl.start()

        val xValues = Repeat.prepare(repeats, 1000) { lockIn.lockedX / totGain }
        val yValues = Repeat.prepare(repeats, 1000) { lockIn.lockedY / totGain }

        for (frequency in frequencies) {

            fControl.target = frequency

            sleep(spin/10)

            lockIn.autoRange()

            sleep(spin)

            for (gate in gates) {

                sgSMU?.voltage = gate

                var startX: Double? = null
                var startY: Double? = null

                for (current in currents) {

                    sdSMU.current = current

                    sleep(delTime)

                    Repeat.runTogether(xValues, yValues)

                    val x = xValues.mean
                    val y = yValues.mean
                    val r = sqrt(x.pow(2) + y.pow(2))
                    val eX = xValues.standardDeviation
                    val eY = yValues.standardDeviation

                    if (startX == null) startX = x
                    if (startY == null) startY = y

                    val hallValue = sqrt((x - startX).pow(2) + (y - startY).pow(2))
                    val hallError = sqrt(((x / r) * eX).pow(2) + ((y / r) * eY).pow(2))

                    results.addData(
                        sdSMU.voltage,                                            // SD Voltage
                        current,                                                  // SD Current
                        gate,                                                     // SG Voltage
                        sgSMU?.current ?: Double.NaN,                             // SG Current
                        rmsField,                                                 // RMS Field
                        frequency,                                                // Field Frequency
                        x - startX,                                               // Locked X
                        eX,                                                       // Error X
                        y - startY,                                               // Locked Y
                        eY,                                                       // Error Y
                        hallValue,                                                // Hall Voltage
                        if (hallError.isFinite()) hallError else (eX + eY) / 2.0, // Hall Error
                        tMeter?.temperature ?: Double.NaN                         // Temperature
                    )

                }

            }

        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fControl?.stop() }

    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            SD_VOLTAGE,
            SD_CURRENT,
            SG_VOLTAGE,
            SG_CURRENT,
            RMS_FIELD,
            FREQUENCY,
            X_VOLTAGE,
            X_ERROR,
            Y_VOLTAGE,
            Y_ERROR,
            HALL_VOLTAGE,
            HALL_ERROR,
            TEMPERATURE
        )

    }

    override fun onInterrupt() {

    }

    override fun onError() {

    }

}