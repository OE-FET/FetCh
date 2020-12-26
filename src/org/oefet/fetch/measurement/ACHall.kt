package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.*
import jisa.enums.Coupling
import jisa.enums.Input
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import java.time.Duration
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class ACHall : FMeasurement() {

    override val type: String = "AC Hall"

    private val label          = StringParameter("Basic", "Name", null, "ACHall")
    private val intTimeParam   = DoubleParameter("Basic", "Integration Time", "s", 100.0)
    private val delTimeParam   = DoubleParameter("Basic", "Delay Time", "s", Duration.ofMinutes(10).toSeconds().toDouble())
    private val repeatsParam   = IntegerParameter("Basic", "Repeats", null, 600)
    private val paGainParam    = DoubleParameter("Basic", "Pre-Amp Gain", null, 1.0)
    private val exGainParam    = DoubleParameter("Basic", "Extra Gain", null, 10.0)
    private val rmsFieldParam  = DoubleParameter("Magnets", "RMS Field Strength", "T", 0.666 / sqrt(2.0))
    private val frequencyParam = RangeParameter("Magnets", "Frequency", "Hz", 1.5, 1.5, 1, Range.Type.LINEAR, 1)
    private val spinParam      = DoubleParameter("Magnets", "Spin-Up Time", "s", 600.0)
    private val currentParam   = RangeParameter("Source-Drain", "Current", "A", -50e-6, 50e-6, 5, Range.Type.LINEAR, 1)
    private val gateParam      = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 0.0, 1, Range.Type.LINEAR, 1)
    private val gdSMUConfig    = addInstrument("Ground Channel (SPA)", SMU::class.java)
    private val sdSMUConfig    = addInstrument("Source-Drain Channel", SMU::class.java)
    private val sgSMUConfig    = addInstrument("Source-Gate Channel", SMU::class.java)
    private val dcPowerConfig  = addInstrument("Motor Power Supply", DCPower::class.java)
    private val lockInConfig   = addInstrument("Lock-In Amplifier", DPLockIn::class.java)
    private val preAmpConfig   = addInstrument("Voltage Pre-Amplifier", VPreAmp::class.java)
    private val tMeterConfig   = addInstrument("Thermometer", TMeter::class.java)

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

    private fun Array<out Double>.stdDeviation(): Double {

        if (size < 2) {
            return 0.0
        }

        val mean = average()
        var sum = 0.0

        for (value in this) sum += (value - mean).pow(2)

        return sqrt(sum / (size - 1))

    }

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.instrument
        sdSMU    = sdSMUConfig.instrument
        sgSMU    = sgSMUConfig.instrument
        dcPower  = dcPowerConfig.instrument
        lockIn   = lockInConfig.instrument
        preAmp   = preAmpConfig.instrument
        tMeter   = tMeterConfig.instrument
        fControl = if (lockIn != null && dcPower != null) FControl(lockIn!!, dcPower!!) else null


    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (sdSMU == null)                                                 errors += "No SD channel configured"
        if (sgSMU == null && !(gates.min() == 0.0 && gates.max() == 0.0))  errors += "No SG channel configured"
        if (lockIn == null)                                                errors += "No lock-in configured"
        if (preAmp == null)                                                errors += "No pre-amp configured"
        if (fControl == null)                                              errors += "No frequency control available"

        return errors

    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())
        results.setAttribute("Pre-Amp Gain", paGain)
        results.setAttribute("Extra Pre-Amp Gain", exGain)

        // Assert that all required instruments must be connected
        val sdSMU = sdSMU!!
        val lockIn = lockIn!!
        val preAmp = preAmp!!
        val fControl = fControl!!

        sdSMU.turnOff()
        sgSMU?.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp.coupling = Coupling.AC;
        preAmp.input    = Input.DIFF;
        preAmp.gain     = paGain;

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

        for (frequency in frequencies) {

            fControl.target = frequency

            sleep(spin/10)

            lockIn.autoRange();

            sleep(spin)

            for (gate in gates) {

                sgSMU?.voltage = gate

                var startX: Double? = null
                var startY: Double? = null

                for (current in currents) {

                    sdSMU.current = current

                    sleep(delTime)

                    val xValues = Array(repeats) { 0.0 }
                    val yValues = Array(repeats) { 0.0 }

                    repeat(repeats) { n ->

                        sleep(1000)
                        xValues[n] = lockIn.lockedX / totGain
                        yValues[n] = lockIn.lockedY / totGain

                    }

                    val x = xValues.average()
                    val y = yValues.average()
                    val r = sqrt(x.pow(2) + y.pow(2))
                    val eX = xValues.stdDeviation()
                    val eY = yValues.stdDeviation()

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

    override fun getLabel(): String = label.value

    override fun getName(): String = "AC Hall Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("RMS Field Strength", "T"),
            Col("Field Frequency", "Hz"),
            Col("X Voltage", "V"),
            Col("X Error", "V"),
            Col("Y Voltage", "V"),
            Col("Y Error", "V"),
            Col("Hall Voltage", "V"),
            Col("Hall Voltage Error", "V"),
            Col("Temperature", "K")
        )

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun onInterrupt() {

    }

    companion object {

        const val SD_VOLTAGE = 0
        const val SD_CURRENT = 1
        const val SG_VOLTAGE = 2
        const val SG_CURRENT = 3
        const val RMS_FIELD = 4
        const val FREQUENCY = 5
        const val X_VOLTAGE = 6
        const val X_ERROR = 7
        const val Y_VOLTAGE = 8
        const val Y_ERROR = 9
        const val HALL_VOLTAGE = 10
        const val HALL_ERROR = 11
        const val TEMPERATURE = 12

    }

}