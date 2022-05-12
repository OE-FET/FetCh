package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Repeat
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementSubAction
import jisa.gui.Colour
import jisa.gui.Doc
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.DCHallPlot
import org.oefet.fetch.results.DCHallResult

/**
 * Measurement class for DC Hall measurements.
 */
class DCHall : FetChMeasurement("DC Hall Measurement", "DCHall", "DC Hall") {

    // Notice display to show when magnet is ramping down
    private val notice = Doc("Ramping Down").apply {

        addHeading("Ramping Down Magnet")
            .setAlignment(Doc.Align.CENTRE)
            .setColour(Colour.RED)

        addText("This measurement has been interrupted and so the electromagnet\nis now safely returning itself to a zero current state.")
            .setAlignment(Doc.Align.CENTRE)

        addText("Please Wait...")
            .setAlignment(Doc.Align.CENTRE)

    }

    // Parameter inputs to ask the user for
    private val delTime  by userInput("Basic", "Delay Time [s]", 0.5) map { (it * 1e3).toInt() }
    private val repTime  by userInput("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats  by userInput("Basic", "Repeats", 50)
    private val fields   by userInput("Magnet", "Field [T]", Range.linear(-1.0, +1.0, 11))
    private val currents by userInput("Source-Drain", "Current [A]", Range.linear(-50e-6, +50e-6, 11))
    private val gates    by userInput("Source-Gate", "Voltage [V]", Range.manual(0.0))

    // Instrument configurations to ask user for
    private val gdSMU  by optionalInstrument("Ground Channel (SPA)", SMU::class)
    private val sdSMU  by requiredInstrument("Source-Drain Channel", SMU::class)
    private val sgSMU  by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { gates.any { it != 0.0 } }
    private val hvm1   by requiredInstrument("Hall Voltmeter 1", VMeter::class)
    private val hvm2   by optionalInstrument("Hall Voltmeter 2", VMeter::class)
    private val hvm3   by optionalInstrument("Hall Voltmeter 3", VMeter::class)
    private val hvm4   by optionalInstrument("Hall Voltmeter 4", VMeter::class)
    private val tMeter by optionalInstrument("Thermometer", TMeter::class)
    private val magnet by optionalInstrument("Magnet Controller", EMController::class) requiredIf { fields.distinct().size > 1 }

    private val actionMagnet  = MeasurementSubAction("Ramp Magnet")
    private val actionCurrent = MeasurementSubAction("Sweep Current")

    /**
     * Constants to refer to columns in this measurement's result table
     */
    companion object {

        val SET_SD_CURRENT = DoubleColumn("Set SD Current", "A")
        val SET_SG_VOLTAGE = DoubleColumn("Set SG Voltage", "V")
        val SD_VOLTAGE     = DoubleColumn("SD Voltage", "V")
        val SD_CURRENT     = DoubleColumn("SD Current", "A")
        val SG_VOLTAGE     = DoubleColumn("SG Voltage", "V")
        val SG_CURRENT     = DoubleColumn("SG Current", "A")
        val FIELD          = DoubleColumn("Field Strength", "T")
        val HALL_1         = DoubleColumn("Hall Voltage 1", "V")
        val HALL_1_ERROR   = DoubleColumn("Hall Voltage 1 Error", "V")
        val HALL_2         = DoubleColumn("Hall Voltage 2", "V")
        val HALL_2_ERROR   = DoubleColumn("Hall Voltage 2 Error", "V")
        val HALL_3         = DoubleColumn("Hall Voltage 3", "V")
        val HALL_3_ERROR   = DoubleColumn("Hall Voltage 3 Error", "V")
        val HALL_4         = DoubleColumn("Hall Voltage 4", "V")
        val HALL_4_ERROR   = DoubleColumn("Hall Voltage 4 Error", "V")
        val TEMPERATURE    = DoubleColumn("Temperature", "K")

    }

    /**
     * This method defines what plot should be made from a given set of DC Hall data.
     */
    override fun createDisplay(data: ResultTable): DCHallPlot {
        return DCHallPlot(data)
    }

    /**
     * This method defined how to process a given set of DC Hall data.
     */
    override fun processResults(data: ResultTable): DCHallResult {
        return DCHallResult(data)
    }

    /**
     * Defines the structure of the result table for this measurement - i.e. it returns the columns that the results
     * table should have.
     */
    override fun getColumns(): Array<Column<*>> {

        return arrayOf(
            SET_SD_CURRENT,
            SET_SG_VOLTAGE,
            SD_VOLTAGE,
            SD_CURRENT,
            SG_VOLTAGE,
            SG_CURRENT,
            FIELD,
            HALL_1,
            HALL_1_ERROR,
            HALL_2,
            HALL_2_ERROR,
            HALL_3,
            HALL_3_ERROR,
            HALL_4,
            HALL_4_ERROR,
            TEMPERATURE
        )

    }

    /**
     * The main bulk of the measurement control code - this is where the measurement happens. Is passed the ResultTable -
     * generated by using the columns returned by getColumns() above - as an argument.
     */
    override fun run(results: ResultTable) {

        // Save measurement parameters to result file
        results.setAttribute("Integration Time", "${hvm1.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())
        results.setAttribute("Averaging Delay", "$repTime ms")

        // Make sure everything starts in a safe off-state
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        hvm1.turnOff()
        hvm2?.turnOff()
        hvm3?.turnOff()
        hvm4?.turnOff()

        // Set the initial values of voltage and current
        gdSMU?.voltage = 0.0
        sdSMU.current  = currents.first()
        sgSMU?.voltage = gates.first()

        // Switch on everything
        gdSMU?.turnOn()
        sdSMU.turnOn()
        sgSMU?.turnOn()
        hvm1.turnOn()
        hvm2?.turnOn()
        hvm3?.turnOn()
        hvm4?.turnOn()

        // Prepare repeat measurements
        val hvm1Values = Repeat.prepare(repeats, repTime) { hvm1.voltage }
        val hvm2Values = Repeat.prepare(repeats, repTime) { hvm2?.voltage ?: Double.NaN }
        val hvm3Values = Repeat.prepare(repeats, repTime) { hvm3?.voltage ?: Double.NaN }
        val hvm4Values = Repeat.prepare(repeats, repTime) { hvm4?.voltage ?: Double.NaN }

        for (gate in gates) {

            gdSMU?.voltage = gate

            for (field in fields) {

                actionMagnet.start()
                magnet?.field = field
                actionMagnet.reset()

                actionCurrent.start()

                for (current in currents) {

                    sdSMU.current = current
                    sleep(delTime)

                    // Run all four repeat measurements side-by-side
                    Repeat.runTogether(hvm1Values, hvm2Values, hvm3Values, hvm4Values)

                    results.mapRow(
                        SET_SD_CURRENT to current,                             // Source-Drain Current (Set Value)
                        SET_SG_VOLTAGE to gate,                                // Source-Gate Voltage (Set Value)
                        SD_VOLTAGE     to sdSMU.voltage,                       // Source-Drain Voltage
                        SD_CURRENT     to sdSMU.current,                       // Source-Drain Current (Measured Value)
                        SG_VOLTAGE     to (sgSMU?.voltage ?: Double.NaN),      // Source-Gate Voltage (Measured Value) - NaN if not used
                        SG_CURRENT     to (sgSMU?.current ?: Double.NaN),      // Source-Gate Current - NaN if not used
                        FIELD          to (magnet?.field ?: fields.first()),   // Magnetic field
                        HALL_1         to hvm1Values.mean,                     // Hall voltage 1 value (mean)
                        HALL_1_ERROR   to hvm1Values.standardDeviation,        // Hall voltage 1 error (std. deviation)
                        HALL_2         to hvm2Values.mean,                     // Hall voltage 2 value (mean)
                        HALL_2_ERROR   to hvm2Values.standardDeviation,        // Hall voltage 2 error (std. deviation)
                        HALL_3         to hvm3Values.mean,                     // Hall voltage 3 value (mean)
                        HALL_3_ERROR   to hvm3Values.standardDeviation,        // Hall voltage 3 error (std. deviation)
                        HALL_4         to hvm4Values.mean,                     // Hall voltage 4 value (mean)
                        HALL_4_ERROR   to hvm4Values.standardDeviation,        // Hall voltage 4 error (std. deviation)
                        TEMPERATURE    to (tMeter?.temperature ?: Double.NaN)  // Temperature - NaN if not used
                    )

                }

                actionCurrent.reset()

            }

        }

    }

    /**
     * Code to run if the measurement is stopped before completion - this happens when the user presses "stop"
     */
    override fun onInterrupt() {

        Util.errLog.println("DC Hall Measurement Interrupted.")

        if (magnet != null) {
            notice.show()
        }

    }

    /**
     * Code that always runs after the measurement has finished - this will always run regardless of whether the
     * measurement finished successfully, was interrupted or failed with an error. Generally used to make sure everything
     * gets switched off etc.
     */
    override fun onFinish() {

        actions.forEach { it.reset() }
        actionMagnet.start()

        // "runRegardless" just makes sure any error given by any of these commands is ignored, otherwise one of them
        // failing would prevent the rest from running.
        runRegardless (
            { sdSMU.turnOff() },
            { gdSMU?.turnOff() },
            { sgSMU?.turnOff() },
            { hvm1.turnOff() },
            { hvm2?.turnOff() },
            { hvm3?.turnOff() },
            { hvm4?.turnOff() },
            { magnet?.turnOff() }
        )

        notice.close()
        actionMagnet.reset()

    }

    /**
     * Custom modification so that when a new result file is created, it records whether this measurement will be
     * sweeping field or not - this is used by the plot to see whether it should plot against field or current. Normally
     * overriding this is not needed.
     */
    override fun newResults(path: String?): ResultTable {
        val results =  super.newResults(path)
        results.setAttribute("Field Sweep", if (fields.maxOrNull() != fields.minOrNull()) "true" else "false")
        return results
    }

    override fun getActions(): List<Action<*>> {
        return listOf(actionMagnet, actionCurrent)
    }

}