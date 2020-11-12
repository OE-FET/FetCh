package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.LockIn
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

    private val label         = StringParameter("Basic", "Name", null, "ACHall")
    private val intTimeParam  = DoubleParameter("Basic", "Integration Time", "s", 100.0)
    private val delTimeParam  = DoubleParameter("Basic", "Delay Time", "s", Duration.ofMinutes(10).toSeconds().toDouble())
    private val repeatsParam  = IntegerParameter("Basic", "Repeats", null, 600)
    private val paGainParam   = DoubleParameter("Basic", "Pre-Amp Gain", null, 1.0)
    private val exGainParam   = DoubleParameter("Basic", "Extra Gain", null, 10.0)
    private val rmsFieldParam = DoubleParameter("Magnets", "RMS Field Strength", "T", 0.666 / sqrt(2.0))
    private val minFParam     = DoubleParameter("Magnets", "Min Frequency", "Hz", 1.0)
    private val maxFParam     = DoubleParameter("Magnets", "Max Frequency", "Hz", 1.0)
    private val numFParam     = IntegerParameter("Magnets", "No. Steps", null, 1)
    private val spinParam     = DoubleParameter("Magnets", "Spin-Up Time", "s", 600.0)
    private val minIParam     = DoubleParameter("Source-Drain", "Start", "A", 0.0)
    private val maxIParam     = DoubleParameter("Source-Drain", "Stop", "A", 50e-6)
    private val numIParam     = IntegerParameter("Source-Drain", "No. Steps", null, 11)
    private val minGParam     = DoubleParameter("Source-Gate", "Start", "V", 0.0)
    private val maxGParam     = DoubleParameter("Source-Gate", "Stop", "V", 0.0)
    private val numGParam     = IntegerParameter("Source-Gate", "No. Steps", null, 1)
    private val gdSMUConfig   = addInstrument(Configurator.SMU("Ground Channel (SPA)", Connections))
    private val sdSMUConfig   = addInstrument(Configurator.SMU("Source-Drain Channel", Connections))
    private val sgSMUConfig   = addInstrument(Configurator.SMU("Source-Gate Channel", Connections))

    val intTime  get() = intTimeParam.value
    val delTime  get() = (1e3 * delTimeParam.value).toInt()
    val repeats  get() = repeatsParam.value
    val paGain   get() = paGainParam.value
    val exGain   get() = exGainParam.value
    val rmsField get() = rmsFieldParam.value
    val minF     get() = minFParam.value
    val maxF     get() = maxFParam.value
    val numF     get() = numFParam.value
    val spin     get() = (1e3 * spinParam.value).toInt()
    val minI     get() = minIParam.value
    val maxI     get() = maxIParam.value
    val numI     get() = numIParam.value
    val minG     get() = minGParam.value
    val maxG     get() = maxGParam.value
    val numG     get() = numGParam.value
    val totGain  get() = paGain * exGain

    private fun Array<out Double>.stdDeviation(): Double {

        val mean = average()
        var sum = 0.0

        for (value in this) sum += (value - mean).pow(2)

        return sqrt(sum / (size - 1))

    }

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        sgSMU    = sgSMUConfig.get()

        super.loadInstruments()

    }

    override fun checkForErrors() : List<String> {

        val errors = LinkedList<String>()

        if (sdSMU == null)    errors += "No SD channel configured"
        if (sgSMU == null)    errors += "No SG channel configured"
        if (lockIn == null)   errors += "No lock-in configured"
        if (preAmp == null)   errors += "No pre-amp configured"
        if (fControl == null) errors += "No frequency control available"

        return errors

    }

    override fun run(results: ResultTable) {

        // Assert that all required instruments must be connected
        val sdSMU    = sdSMU!!
        val sgSMU    = sgSMU!!
        val lockIn   = lockIn!!
        val preAmp   = preAmp!!
        val fControl = fControl!!

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp.coupling = Coupling.AC;
        preAmp.input = Input.DIFF;
        preAmp.gain = paGain;

        // Initialise the SMUs
        sdSMU.current = minI
        sgSMU.voltage = minG
        gdSMU?.voltage = 0.0

        // Configure the lock-in amplifier
        lockIn.setRefMode(LockIn.RefMode.EXTERNAL)
        lockIn.timeConstant = intTime

        // Set everything going
        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fControl.start()

        for (frequency in Range.linear(minF, maxF, numF)) {

            fControl.target = frequency

            sleep(spin)

            for (gate in Range.linear(minG, maxG, numG)) {

                sgSMU.voltage = gate

                var startX: Double? = null
                var startY: Double? = null

                for (current in Range.linear(minI, maxI, numI)) {

                    sdSMU.current = current

                    sleep(delTime)

                    val xValues = Array(repeats) { 0.0 }
                    val yValues = Array(repeats) { 0.0 }

                    repeat(repeats) { n ->

                        sleep(1000)
                        xValues[n] = lockIn.lockedX / totGain
                        yValues[n] = lockIn.lockedY / totGain

                    }

                    val x  = xValues.average()
                    val y  = yValues.average()
                    val r  = sqrt(x.pow(2) + y.pow(2))
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
                        sgSMU.current,                                            // SG Current
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