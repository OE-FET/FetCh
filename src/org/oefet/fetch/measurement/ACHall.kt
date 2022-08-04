package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Repeat
import jisa.devices.interfaces.*
import jisa.enums.Coupling
import jisa.enums.Icon
import jisa.enums.Input
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import jisa.results.BooleanColumn
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.SimpleACHallPlot
import org.oefet.fetch.results.ACHallResult
import kotlin.Double.Companion.NaN
import kotlin.math.pow
import kotlin.math.sqrt

class ACHall : FetChMeasurement("AC Hall Measurement", "ACHall", "AC Hall", Icon.CIRCLES.blackImage) {

    // User input parameters
    private val intTime         by userInput("Basic", "Integration Time [s]", 100.0)
    private val delTime         by userTimeInput("Basic", "Delay Time", 300000)
    private val repeats         by userInput("Basic", "Repeats", 300)
    private val paGain          by userInput("Basic", "Pre-Amp Gain", 1.0)
    private val exGain          by userInput("Basic", "Extra Gain", 10.0)
    private val rmsField        by userInput("Magnets", "RMS Field Strength [T]", 0.666 / sqrt(2.0))
    private val hallFrequencies by userInput("Magnets", "Frequencies [Hz]", Range.manual(1.2))
    private val spin            by userTimeInput("Magnets", "Spin-Up Time", 600000)
    private val doFaraday       by userInput("Faraday Sweep", "Do Faraday Sweep", true)
    private val faraFrequencies by userInput("Faraday Sweep", "Frequencies [Hz]", Range.manual(1.0, 1.2, 1.4))
    private val currents        by userInput("Source-Drain", "Current [A]", Range.step(-10e-6, +10e-6, 5e-6))
    
    private val totGain get() = paGain * exGain

    // Instruments
    private val gdSMU   by optionalInstrument("Ground Channel (SPA)", SMU::class)
    private val sdSMU   by requiredInstrument("Source-Drain Channel", SMU::class)
    private val dcPower by requiredInstrument("Motor Power Supply", DCPower::class)
    private val lockIn  by requiredInstrument("Lock-In Amplifier", DPLockIn::class)
    private val preAmp  by optionalInstrument("Voltage Pre-Amplifier", VPreAmp::class) requiredIf { paGain != 1.0 }
    private val tMeter  by optionalInstrument("Thermometer", TMeter::class)

    private val stageSpinUp    = MeasurementSubAction("Spin-up magnets")
    private val stageAutoRange = MeasurementSubAction("Auto-range lock-in amplifier")
    private val stageStabilise = MeasurementSubAction("Waiting for system to stabilise")
    private val stageMeasure   = MeasurementSubAction("Measuring")
    private val stageFaraday   = MeasurementSubAction("Faraday Sweep")

    private lateinit var fControl: FControl

    companion object {

        val FARADAY      = BooleanColumn("Faraday Sweep")
        val SD_VOLTAGE   = DoubleColumn("SD Voltage", "V")
        val SD_CURRENT   = DoubleColumn("SD Current", "A")
        val RMS_FIELD    = DoubleColumn("RMS Field Strength", "T")
        val FREQUENCY    = DoubleColumn("Field Frequency", "Hz")
        val X_VOLTAGE    = DoubleColumn("X Voltage", "V")
        val X_ERROR      = DoubleColumn("X Error", "V")
        val Y_VOLTAGE    = DoubleColumn("Y Voltage", "V")
        val Y_ERROR      = DoubleColumn("Y Error", "V")
        val HALL_VOLTAGE = DoubleColumn("Hall Voltage", "V")
        val HALL_ERROR   = DoubleColumn("Hall Voltage Error", "V")
        val TEMPERATURE  = DoubleColumn("Temperature", "K")

    }

    override fun getColumns(): Array<Column<*>> {

        return arrayOf(
            FARADAY,
            SD_VOLTAGE,
            SD_CURRENT,
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

        message("Setting up measurement.")

        fControl = FControl(lockIn, dcPower)

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats)
        results.setAttribute("Pre-Amp Gain", paGain)
        results.setAttribute("Extra Pre-Amp Gain", exGain)

        sdSMU.turnOff()
        gdSMU?.turnOff()

        // Configure the pre-amp
        preAmp?.coupling = Coupling.AC
        preAmp?.input    = Input.DIFF
        preAmp?.gain     = paGain

        // Initialise the SMUs
        sdSMU.current = currents.first()
        gdSMU?.voltage = 0.0

        // Configure the lock-in amplifier
        lockIn.refMode = LockIn.RefMode.EXTERNAL
        lockIn.timeConstant = intTime

        // Set everything going
        sdSMU.turnOn()
        gdSMU?.turnOn()
        fControl.start()

        // Prepare repeat measurements for X and Y voltage values
        val xValues = Repeat.prepare(repeats, 1000) { lockIn.lockedX / totGain }
        val yValues = Repeat.prepare(repeats, 1000) { lockIn.lockedY / totGain }

        message("Spinning up magnets to max frequency.")

        stageSpinUp.start();

        fControl.target = hallFrequencies.maxOrNull() ?: 1.0
        fControl.waitForStableFrequency(25.0, 60000)

        message("Auto ranging lock-in amplifier.")

        // Auto range and offset lock-in amplifier
        stageAutoRange.start()
        lockIn.autoRange(0.66)
        stageAutoRange.complete()

        stageSpinUp.complete()

        for (frequency in hallFrequencies) {

            message("Adjusting magnet frequency to $frequency Hz.")

            // Adjust magnet frequency and wait enough time to stabilise
            stageSpinUp.start();

            fControl.target = frequency
            fControl.waitForStableFrequency(25.0, 60000)

            stageSpinUp.complete()

            // Wait for lock-in to stabilise fully
            stageStabilise.start()
            sleep(spin)

            var startX: Double? = null
            var startY: Double? = null

            for (current in currents) {

                message("Sourcing current $current A.")

                sdSMU.current = current

                message("Waiting ${Util.msToString(delTime.toLong())} for lock-in to stabilise.")
                stageStabilise.start()
                sleep(delTime)
                stageStabilise.reset()

                message("Sampling locked voltages over ${Util.msToString(repeats * 1000L)}.")
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

                message("Sampling complete, writing data to table.")

                results.mapRow(
                    FARADAY      to false,
                    SD_VOLTAGE   to sdSMU.voltage,
                    SD_CURRENT   to current,
                    RMS_FIELD    to rmsField,
                    FREQUENCY    to frequency,
                    X_VOLTAGE    to (x - startX),
                    X_ERROR      to eX,
                    Y_VOLTAGE    to (y - startX),
                    Y_ERROR      to eY,
                    HALL_VOLTAGE to hallValue,
                    HALL_ERROR   to (if (hallError.isFinite()) hallError else (eX + eY) / 2.0),
                    TEMPERATURE  to (tMeter?.temperature ?: NaN)
                )

            }

        }

        message("Main measurement complete.")

        sdSMU.turnOff()

        if (doFaraday) {

            message("Starting Faraday sweep.")

            stageFaraday.start()

            lockIn.timeConstant = 10.0 / (faraFrequencies.minOrNull() ?: 1.0)

            val xValues = Repeat.prepare((lockIn.timeConstant * 10).toInt(), 1000) { lockIn.lockedX / totGain }
            val yValues = Repeat.prepare((lockIn.timeConstant * 10).toInt(), 1000) { lockIn.lockedY / totGain }

            for (frequency in faraFrequencies) {

                message("Adjusting magnet frequency to $frequency Hz.")

                fControl.target = frequency

                sleep(((lockIn.timeConstant * 10) * 1000).toInt())

                message("Sampling locked voltages over ${Util.msToString((lockIn.timeConstant * 10).toLong() * 1000L)}")

                Repeat.runTogether(xValues, yValues)

                message("Sampling complete, writing data to table.")

                results.mapRow(
                    FARADAY      to true,
                    SD_VOLTAGE   to NaN,
                    SD_CURRENT   to NaN,
                    RMS_FIELD    to rmsField,
                    FREQUENCY    to frequency,
                    X_VOLTAGE    to xValues.mean,
                    X_ERROR      to xValues.standardDeviation,
                    Y_VOLTAGE    to yValues.mean,
                    Y_ERROR      to yValues.standardDeviation,
                    HALL_VOLTAGE to NaN,
                    HALL_ERROR   to NaN,
                    TEMPERATURE  to (tMeter?.temperature ?: NaN)
                )

            }

            stageFaraday.complete()
            message("Faraday sweep complete.")

        }

    }

    override fun getActions(): List<MeasurementSubAction> {
        return if (doFaraday) {
            listOf(stageSpinUp, stageAutoRange, stageStabilise, stageMeasure, stageFaraday)
        } else {
            listOf(stageSpinUp, stageAutoRange, stageStabilise, stageMeasure)
        }
    }

    override fun onFinish() {

        runRegardless (
            { sdSMU.turnOff() },
            { gdSMU?.turnOff() },
            { fControl.stop() }
        )

        actions.forEach { it.reset() }

    }

    override fun processResults(data: ResultTable): ACHallResult {
        return ACHallResult(data)
    }

    override fun createDisplay(data: ResultTable): SimpleACHallPlot {
        return SimpleACHallPlot(data)
    }

}