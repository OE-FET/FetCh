package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.control.RTask
import jisa.devices.*
import jisa.enums.Coupling
import jisa.enums.Input
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.lang.Exception
import java.time.Duration
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class ACHallMeasurement : FetChMeasurement() {

    // private var iValues       = Range.linear(0, 0, 1)
    // private var gValues       = Range.linear(0, 0, 1)
    // private var intTime       = 100.0
    // private var delTime       = Duration.ofMinutes(10).toMillis().toInt()
    // private var repeats       = 600
    // private var paGain        = 1.0
    // private var exGain        = 10.0
    // private var rmsField      = 0.666 / sqrt(2.0)

    override val type: String = "AC Hall"

    private var gdSMU: SMU?         = null
    private var sdSMU: SMU?         = null
    private var sgSMU: SMU?         = null
    private var lockIn: DPLockIn?   = null
    private var preAmp: VPreAmp?    = null
    private var dcPower: DCPower?   = null
    private var tMeter: TMeter?     = null
    private var fControl: FControl? = null
    private var tControl: TC?       = null

    val label    = StringParameter("Basic", "Name", null, "ACHall")
    val intTime  = DoubleParameter("Basic", "Integration Time", "s", 100.0)
    val delTime  = DoubleParameter("Basic", "Delay Time", "s", Duration.ofMinutes(10).toSeconds().toDouble())
    val repeats  = IntegerParameter("Basic", "Repeats", null, 600)
    val paGain   = DoubleParameter("Basic", "Pre-Amp Gain", null, 1.0)
    val exGain   = DoubleParameter("Basic", "Extra Gain", null, 10.0)
    val rmsField = DoubleParameter("Magnets", "RMS Field Strength", "T", 0.666 / sqrt(2.0))
    val minF     = DoubleParameter("Magnets", "Min Frequency", "Hz", 1.0)
    val maxF     = DoubleParameter("Magnets", "Max Frequency", "Hz", 1.0)
    val numF     = IntegerParameter("Magnets", "No. Steps", null, 1)
    val spin     = DoubleParameter("Magnets", "Spin-Up Time", "s", 600.0)
    val minI     = DoubleParameter("Source-Drain", "Start", "A", 0.0)
    val maxI     = DoubleParameter("Source-Drain", "Stop", "A", 50e-6)
    val numI     = IntegerParameter("Source-Drain", "No. Steps", null, 11)
    val minG     = DoubleParameter("Source-Gate", "Start", "V", 0.0)
    val maxG     = DoubleParameter("Source-Gate", "Stop", "V", 0.0)
    val numG     = IntegerParameter("Source-Gate", "No. Steps", null, 1)

    private fun Array<out Double>.stdDeviation(): Double {

        val mean = average()
        var sum = 0.0

        for (value in this) sum += (value - mean).pow(2)

        return sqrt(sum / (size - 1))

    }

    private fun loadInstruments() {

        gdSMU    = Instruments.gdSMU
        sdSMU    = Instruments.sdSMU
        sgSMU    = Instruments.sgSMU
        lockIn   = Instruments.lockIn
        preAmp   = Instruments.preAmp
        dcPower  = Instruments.dcPower
        tMeter   = Instruments.tMeter
        fControl = if (lockIn != null && dcPower != null) FControl(lockIn!!, dcPower!!) else null
        tControl = Instruments.tControl

        val errors = LinkedList<String>()

        if (sdSMU == null)    errors += "No SD channel configured"
        if (sgSMU == null)    errors += "No SG channel configured"
        if (lockIn == null)   errors += "No lock-in configured"
        if (preAmp == null)   errors += "No pre-amp configured"
        if (fControl == null) errors += "No frequency control available"

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

    }

    override fun run(results: ResultTable) {

        loadInstruments()

        // Assert that all required instruments must be connected
        val sdSMU    = sdSMU!!
        val sgSMU    = sgSMU!!
        val lockIn   = lockIn!!
        val preAmp   = preAmp!!
        val fControl = fControl!!

        // Get parameter values
        val intTime  = intTime.value
        val delTime  = (delTime.value * 1000.0).toInt()
        val repeats  = repeats.value
        val paGain   = paGain.value
        val rmsField = rmsField.value
        val spinUp   = (spin.value * 1000.0).toInt()
        val iValues  = Range.linear(minI.value, maxI.value, numI.value)
        val gValues  = Range.linear(minG.value, maxG.value, numG.value)
        val fValues  = Range.linear(minF.value, maxF.value, numF.value)
        val totGain  = paGain * exGain.value

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp.coupling = Coupling.AC;
        preAmp.input    = Input.DIFF;
        preAmp.gain     = paGain;

        // Initialise the SMUs
        sdSMU.current  = iValues.first()
        sgSMU.voltage  = gValues.first()
        gdSMU?.voltage = 0.0

        // Configure the lock-in amplifier
        lockIn.setRefMode(LockIn.RefMode.EXTERNAL)
        lockIn.timeConstant = intTime

        // Set everything going
        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fControl.start()

        for (frequency in fValues) {

            fControl.target = frequency

            sleep(spinUp)

            for (gate in gValues) {

                sgSMU.voltage = gate

                var startX: Double? = null
                var startY: Double? = null

                for (current in iValues) {

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

    override fun log(task: RTask, log: ResultTable) {

        log.addData(
            task.mSecFromStart.toDouble(),
            sdSMU?.voltage ?: Double.NaN,
            sdSMU?.current ?: Double.NaN,
            sgSMU?.voltage ?: Double.NaN,
            sgSMU?.current ?: Double.NaN,
            lockIn?.frequency ?: Double.NaN,
            lockIn?.lockedX ?: Double.NaN,
            lockIn?.lockedY ?: Double.NaN,
            tMeter?.temperature ?: Double.NaN,
            tControl?.heaterPower ?: Double.NaN
        )

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun getLogColumns(): Array<Col> {

        return arrayOf(
            Col("Running Time", "ms"),
            Col("Source-Drain Voltage", "V"),
            Col("Source-Drain Current", "A"),
            Col("Source-Gate Voltage", "V"),
            Col("Source-Gate Current", "A"),
            Col("Field Frequency", "Hz"),
            Col("X Voltage", "V"),
            Col("Y Voltage", "V"),
            Col("Temperature", "K"),
            Col("Heater Power", "%")
        )

    }

    override fun onInterrupt() {

    }

    companion object {

        const val SD_VOLTAGE   = 0
        const val SD_CURRENT   = 1
        const val SG_VOLTAGE   = 2
        const val SG_CURRENT   = 3
        const val RMS_FIELD    = 4
        const val FREQUENCY    = 5
        const val X_VOLTAGE    = 6
        const val X_ERROR      = 7
        const val Y_VOLTAGE    = 8
        const val Y_ERROR      = 9
        const val HALL_VOLTAGE = 10
        const val HALL_ERROR   = 11
        const val TEMPERATURE  = 12

        const val LOG_TIME_MS      = 0
        const val LOG_SD_VOLTAGE   = 1
        const val LOG_SD_CURRENT   = 2
        const val LOG_SG_VOLTAGE   = 3
        const val LOG_SG_CURRENT   = 4
        const val LOG_FREQUENCY    = 5
        const val LOG_X_VOLTAGE    = 6
        const val LOG_Y_VOLTAGE    = 7
        const val LOG_TEMPERATURE  = 8
        const val LOG_HEATER_POWER = 9

    }

}