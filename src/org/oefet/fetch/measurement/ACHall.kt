package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.*
import jisa.enums.Coupling
import jisa.enums.Input
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ACHallResult
import kotlin.math.pow
import kotlin.math.sqrt

class ACHall : FMeasurement("AC Hall Measurement", "ACHall", "AC Hall") {

    // User input parameters
    private val intTime     by input("Basic", "Integration Time [s]", 100.0)
    private val delTime     by input("Basic", "Delay Time [s]", 300.0) map { (it * 1e3).toInt() }
    private val repeats     by input("Basic", "Repeats", 300)
    private val paGain      by input("Basic", "Pre-Amp Gain", 1.0)
    private val exGain      by input("Basic", "Extra Gain", 10.0)
    private val rmsField    by input("Magnets", "RMS Field Strength [T]", 0.666 / sqrt(2.0))
    private val frequencies by input("Magnets", "Frequency [Hz]", Range.manual(1.5))
    private val spin        by input("Magnets", "Spin-Up Time [s]", 600.0) map { (it * 1e3).toInt() }
    private val currents    by input("Source-Drain", "Current [A]", Range.step(-10e-6, +10e-6, 5e-6))
    private val gates       by input("Source-Gate", "Voltage [V]", Range.manual(0.0))

    // Instruments
    private val gdSMU   by optionalConfig("Ground Channel (SPA)", SMU::class)
    private val sdSMU   by requiredConfig("Source-Drain Channel", SMU::class)
    private val sgSMU   by optionalConfig("Source-Gate Channel", SMU::class) requiredIf { gates.any { it != 0.0 }}
    private val dcPower by requiredConfig("Motor Power Supply", DCPower::class)
    private val lockIn  by requiredConfig("Lock-In Amplifier", DPLockIn::class)
    private val preAmp  by optionalConfig("Voltage Pre-Amplifier", VPreAmp::class) requiredIf { paGain != 1.0 }
    private val tMeter  by optionalConfig("Thermometer", TMeter::class)

    private lateinit var fControl: FControl

    private val totGain get() = paGain * exGain

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

    override fun loadInstruments() {

        super.loadInstruments()
        fControl = FControl(lockIn, dcPower)

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

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())
        results.setAttribute("Pre-Amp Gain", paGain)
        results.setAttribute("Extra Pre-Amp Gain", exGain)

        sdSMU.turnOff()
        sgSMU?.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp?.coupling = Coupling.AC
        preAmp?.input    = Input.DIFF
        preAmp?.gain     = paGain

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

        runRegardless { sdSMU.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fControl.stop() }

    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): ACHallResult {
        return ACHallResult(data, extra)
    }

    override fun createPlot(data: ResultTable): ACHallPlot {
        return ACHallPlot(data)
    }

}