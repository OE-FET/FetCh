package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.*
import jisa.enums.Coupling
import jisa.enums.Input
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.results.ACHallResult
import kotlin.math.pow
import kotlin.math.sqrt

class ACHall : FetChMeasurement("AC Hall Measurement", "ACHall", "AC Hall") {

    // User input parameters
    private val intTime     by userInput("Basic", "Integration Time [s]", 100.0)
    private val delTime     by userInput("Basic", "Delay Time [s]", 300.0) map { (it * 1e3).toInt() }
    private val repeats     by userInput("Basic", "Repeats", 300)
    private val paGain      by userInput("Basic", "Pre-Amp Gain", 1.0)
    private val exGain      by userInput("Basic", "Extra Gain", 10.0)
    private val rmsField    by userInput("Magnets", "RMS Field Strength [T]", 0.666 / sqrt(2.0))
    private val frequencies by userInput("Magnets", "Frequency [Hz]", Range.manual(1.5))
    private val spin        by userInput("Magnets", "Spin-Up Time [s]", 600.0) map { (it * 1e3).toInt() }
    private val currents    by userInput("Source-Drain", "Current [A]", Range.step(-10e-6, +10e-6, 5e-6))
    private val gates       by userInput("Source-Gate", "Voltage [V]", Range.manual(0.0))

    // Instruments
    private val gdSMU   by optionalInstrument("Ground Channel (SPA)", SMU::class)
    private val sdSMU   by requiredInstrument("Source-Drain Channel", SMU::class)
    private val sgSMU   by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { gates.any { it != 0.0 } }
    private val dcPower by requiredInstrument("Motor Power Supply", DCPower::class)
    private val lockIn  by requiredInstrument("Lock-In Amplifier", DPLockIn::class)
    private val preAmp  by optionalInstrument("Voltage Pre-Amplifier", VPreAmp::class) requiredIf { paGain != 1.0 }
    private val tMeter  by optionalInstrument("Thermometer", TMeter::class)

    private val stageSpinUp    = MeasurementSubAction("Spin-up magnets")
    private val stageAutoRange = MeasurementSubAction("Auto-range lock-in amplifier")
    private val stageStabilise = MeasurementSubAction("Waiting for system to stabilise")
    private val stageMeasure   = MeasurementSubAction("Measuring")

    private lateinit var fControl: FControl

    private val totGain get() = paGain * exGain

    companion object {

        val SD_VOLTAGE = DoubleColumn("SD Voltage", "V")
        val SD_CURRENT = DoubleColumn("SD Current", "A")
        val SG_VOLTAGE = DoubleColumn("SG Voltage", "V")
        val SG_CURRENT = DoubleColumn("SG Current", "A")
        val RMS_FIELD = DoubleColumn("RMS Field Strength", "T")
        val FREQUENCY = DoubleColumn("Field Frequency", "Hz")
        val X_VOLTAGE = DoubleColumn("X Voltage", "V")
        val X_ERROR = DoubleColumn("X Error", "V")
        val Y_VOLTAGE = DoubleColumn("Y Voltage", "V")
        val Y_ERROR = DoubleColumn("Y Error", "V")
        val HALL_VOLTAGE = DoubleColumn("Hall Voltage", "V")
        val HALL_ERROR = DoubleColumn("Hall Voltage Error", "V")
        val TEMPERATURE = DoubleColumn("Temperature", "K")

    }

    override fun getColumns(): Array<Column<*>> {

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

        fControl = FControl(lockIn, dcPower)

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats)
        results.setAttribute("Pre-Amp Gain", paGain)
        results.setAttribute("Extra Pre-Amp Gain", exGain)

        sdSMU.turnOff()
        sgSMU?.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp?.coupling = Coupling.AC
        preAmp?.input = Input.DIFF
        preAmp?.gain = paGain

        // Initialise the SMUs
        sdSMU.current = currents.first()
        sgSMU?.voltage = gates.first()
        gdSMU?.voltage = 0.0

        // Configure the lock-in amplifier
        lockIn.refMode = LockIn.RefMode.EXTERNAL
        lockIn.timeConstant = intTime

        // Set everything going
        sdSMU.turnOn()
        sgSMU?.turnOn()
        gdSMU?.turnOn()
        fControl.start()

        val xValues = Repeat.prepare(repeats, 1000) { lockIn.lockedX / totGain }
        val yValues = Repeat.prepare(repeats, 1000) { lockIn.lockedY / totGain }

        for (frequency in frequencies) {


            stageSpinUp.start();
            fControl.target = frequency

            sleep(spin / 10)
            stageSpinUp.complete()

            stageAutoRange.start()
            lockIn.autoRange()
            stageAutoRange.complete()

            stageStabilise.start()
            sleep(spin)

            for (gate in gates) {

                sgSMU?.voltage = gate

                var startX: Double? = null
                var startY: Double? = null

                for (current in currents) {

                    sdSMU.current = current

                    stageStabilise.start()
                    sleep(delTime)
                    stageStabilise.reset()

                    stageMeasure.start()
                    Repeat.runTogether(xValues, yValues)
                    stageMeasure.reset()

                    val x = xValues.mean
                    val y = yValues.mean
                    val r = sqrt(x.pow(2) + y.pow(2))
                    val eX = xValues.standardDeviation
                    val eY = yValues.standardDeviation

                    if (startX == null) startX = x
                    if (startY == null) startY = y

                    val hallValue = sqrt((x - startX).pow(2) + (y - startY).pow(2))
                    val hallError = sqrt(((x / r) * eX).pow(2) + ((y / r) * eY).pow(2))

                    results.mapRow(
                       SD_VOLTAGE   to sdSMU.voltage,
                       SD_CURRENT   to current,
                       SG_VOLTAGE   to gate,
                       SG_CURRENT   to (sgSMU?.current ?: Double.NaN),
                       RMS_FIELD    to rmsField,
                       FREQUENCY    to frequency,
                       X_VOLTAGE    to x - startX,
                       X_ERROR      to eX,
                       Y_VOLTAGE    to y - startX,
                       Y_ERROR      to eY,
                       HALL_VOLTAGE to hallValue,
                       HALL_ERROR   to if (hallError.isFinite()) hallError else (eX + eY) / 2.0,
                       TEMPERATURE  to (tMeter?.temperature ?: Double.NaN)
                    )

                }

            }

        }

    }

    override fun getActions(): List<MeasurementSubAction> {
        return listOf(stageSpinUp, stageAutoRange, stageStabilise, stageMeasure)
    }

    override fun onFinish() {

        runRegardless { sdSMU.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fControl.stop() }

        actions.forEach { it.reset() }

    }

    override fun processResults(data: ResultTable): ACHallResult {
        return ACHallResult(data)
    }

    override fun createDisplay(data: ResultTable): ACHallPlot {
        return ACHallPlot(data)
    }

}