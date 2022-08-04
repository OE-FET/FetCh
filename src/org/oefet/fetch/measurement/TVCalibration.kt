package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVCPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TVCResult

class TVCalibration : FetChMeasurement("Thermal Voltage Calibration Measurement", "TVC", "Thermal Voltage Calibration", Images.getImage("calibration.png")) {

    // Parameters
    private val avgCount by userInput("Basic", "Averaging Count",1)
    private val avgDelay by userTimeInput("Basic", "Averaging Delay", 0)
    private val probe    by userChoice("Basic", "Strip", "Left", "Right")
    private val heaterV  by userInput("Heater", "Heater Voltage [V]", Range.polynomial(0, 5, 6, 2))
    private val holdHV   by userTimeInput("Heater", "Hold Time", 60000)
    private val currents by userInput("Resistive Thermometer", "Current [A]", Range.linear(0.0, 100e-6, 11))
    private val holdSI   by userTimeInput("Resistive Thermometer", "Hold Time", 500)

    // Instruments
    private val gdSMU  by optionalInstrument("Ground Channel (SPA)", SMU::class)
    private val heater by requiredInstrument("Heater Channel", SMU::class)
    private val sdSMU  by requiredInstrument("Strip Source-Drain Channel", SMU::class)
    private val fpp1   by optionalInstrument("Four-Point Probe Channel 1", VMeter::class)
    private val fpp2   by optionalInstrument("Four-Point Probe Channel 2", VMeter::class)
    private val tMeter by optionalInstrument("Thermometer", TMeter::class)

    private val actionHeater = MeasurementSubAction("Hold Heater")
    private val actionSweep  = MeasurementSubAction("Sweep Current")

    /** Result Table Columns */
    companion object Columns {

        val SET_HEATER_VOLTAGE  = DoubleColumn("Set Heater Voltage", "V")
        val SET_STRIP_CURRENT   = DoubleColumn("Set Strip Current", "A")
        val GROUND_CURRENT      = DoubleColumn("Ground Current", "A")
        val HEATER_VOLTAGE      = DoubleColumn("Heater Voltage", "V")
        val HEATER_CURRENT      = DoubleColumn("Heater Current", "A")
        val HEATER_POWER        = DoubleColumn("Heater Power", "W")
        val STRIP_VOLTAGE       = DoubleColumn("Strip Voltage", "V")
        val STRIP_VOLTAGE_ERROR = DoubleColumn("Strip Voltage Error", "V")
        val STRIP_CURRENT       = DoubleColumn("Strip Current", "A")
        val TEMPERATURE         = DoubleColumn("Temperature", "K")
        val COLUMN_ORDER        = arrayOf(
            SET_HEATER_VOLTAGE,
            SET_STRIP_CURRENT,
            GROUND_CURRENT,
            HEATER_VOLTAGE,
            HEATER_CURRENT,
            HEATER_POWER,
            STRIP_VOLTAGE,
            STRIP_VOLTAGE_ERROR,
            STRIP_CURRENT,
            TEMPERATURE
        )

    }

    /**
     * Sub-actions to show within the action in the queue GUI
     */
    override fun getActions(): List<Action<*>> {
        return listOf(actionHeater, actionSweep)
    }

    /**
     * What to show when running (plot)
     */
    override fun createDisplay(data: ResultTable): TVCPlot {
        return TVCPlot(data)
    }

    /**
     * What class should FetCh use to process data from this type of measurement?
     */
    override fun processResults(data: ResultTable): TVCResult {
        return TVCResult(data)
    }

    override fun run(results: ResultTable) {

        // Add measurement information to results file header
        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Averaging Count", avgCount.toString())
        results.setAttribute("Probe Number", probe.toString())
        results.setAttribute("Heater Hold Time", "$holdHV ms")
        results.setAttribute("Delay Time", "$holdSI ms")

        // Make sure it is all off to begin with
        gdSMU?.turnOff()
        heater.turnOff()
        sdSMU.turnOff()

        // Set initial values for everything
        gdSMU?.voltage          = 0.0
        heater.voltage          = heaterV.first()
        sdSMU.current           = currents.first()
        sdSMU.averageMode       = AMode.NONE
        sdSMU.averageCount      = 1

        // Turn on the ground channel if there is one
        gdSMU?.turnOn()

        // Determine how the FPP voltage should be determined based on which voltmeters have been configured
        val voltage = if (fpp1 != null && fpp2 != null) {
            { fpp2!!.voltage - fpp1!!.voltage }  // Two voltage probes: assume they measure a separate probe each, relative to a common potential/ground
        } else if (fpp1 != null) {
            { fpp1!!.voltage }                   // Single voltage probe: assume it measures the voltage between two probes
        } else if (fpp2 != null) {
            { fpp2!!.voltage }                   // Single voltage probe: assume it measures the voltage between two probes
        } else {
            { sdSMU.voltage }                   // No separate voltage probe: use voltage from source-drain SMU
        }

        // Prepare an averaging measurement for the voltage
        val stripMeasurement = Repeat.prepare(avgCount, avgDelay) { voltage() }

        for (heaterVoltage in heaterV) {

            // Tell the queue that we've started the heater portion of the measurement (to update GUI)
            actionHeater.start()

            // Set the heater voltage, turn on the heater and wait for the heater hold time
            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

            // Tell the queue that we've finished the heater portion
            actionHeater.reset()

            // Let the queue know we've started the current sweep portion
            actionSweep.start()

            for (stripCurrent in currents) {

                // Set current, turn on and wait for the current hold time
                sdSMU.current = stripCurrent
                sdSMU.turnOn()
                sleep(holdSI)

                // Perform the repeat/averaging measurement
                stripMeasurement.run()

                val hVoltage  = heater.voltage
                val hCurrent  = heater.current
                val hPower    = hVoltage * hCurrent

                results.mapRow(
                    SET_HEATER_VOLTAGE  to heaterVoltage,
                    SET_STRIP_CURRENT   to stripCurrent,
                    GROUND_CURRENT      to (gdSMU?.current ?: Double.NaN),
                    HEATER_VOLTAGE      to hVoltage,
                    HEATER_CURRENT      to hCurrent,
                    HEATER_POWER        to hPower,
                    STRIP_VOLTAGE       to stripMeasurement.mean,
                    STRIP_VOLTAGE_ERROR to stripMeasurement.standardDeviation,
                    STRIP_CURRENT       to sdSMU.current,
                    TEMPERATURE         to (tMeter?.temperature ?: Double.NaN)
                )

            }

            // Tell the queue that we've finished the sweep portion
            actionSweep.reset()

        }

    }

    /**
     * Gets run after measurement has finished regardless of whether it was successful, interrupted or threw an error.
     */
    override fun onFinish() {

        // Turn off everything, ignore exceptions
        runRegardless(
            { heater.turnOff() },
            { gdSMU?.turnOff() },
            { sdSMU.turnOff() }
        )

        // Reset all sub-actions
        actions.forEach { it.reset() }

    }

    override fun getColumns(): Array<DoubleColumn> {
        return COLUMN_ORDER
    }

    /**
     * Gets run if the measurement is interrupted
     */
    override fun onInterrupt() {
        // Nothing special to do here
    }

    /**
     * Gets run if the measurement encounters an error
     */
    override fun onError() {
        // Nothing special to do here
    }

}