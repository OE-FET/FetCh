package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Measurement class for DC Hall measurements. Running the measurement generally goes like this:
 *
 * 1.   FetCh GUI asks the measurement for all "Parameters" and "Instruments" it has configured, so that it can draw
 *      the correct configuration window - these are defined at the top of this class and stored as private variables.
 *      e.g. DoubleParameter(sectionName, parameterName, units, defaultValue)
 * 2.   When it is time to run the measurement the loadInstruments() method is run first to load the instruments as
 *      as graphically configured by the user in the GUI.
 * 3.   Then checkForErrors() is run - if it returns anything other than an empty list of errors, the measurement will
 *      just return an error and not run any further.
 * 4.   A new ResultTable is generated based-off the columns returned by getColumns().
 * 5.   The measurement code in run() is called, being handed the new ResultTable generated in step 4.
 * 6.   The code in run() can either complete successfully, in error or be interrupted - if interrupted, the
 *      onInterrupt() method is called.
 * 7.   Regardless of how run() ended, the onFinish() method is then always called afterwards.
 */
class DCHall : FMeasurement("DC Hall Measurement", "DCHall", "DC Hall") {

    // Measurement parameters to ask user for when configuring measurement
    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 1.0 / 50.0)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val repeatsParam = IntegerParameter("Basic", "Repeats", null, 50)
    private val repTimeParam = DoubleParameter("Basic", "Repeat Time", "s", 0.0)
    private val fieldParam   = RangeParameter("Magnet", "Field", "T", -1.0, +1.0, 11, Range.Type.LINEAR, 1)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -50e-6, +50e-6, 11, Range.Type.LINEAR, 1)
    private val gateParam    = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 0.0, 1, Range.Type.LINEAR, 1)

    // Instrument configurations to ask user for
    private val magnetConfig = addInstrument("Magnet Controller", EMController::class)
    private val gdSMUConfig  = addInstrument("Ground Channel (SPA)", SMU::class)
    private val sdSMUConfig  = addInstrument("Source-Drain Channel", SMU::class)
    private val sgSMUConfig  = addInstrument("Source-Gate Channel", SMU::class)
    private val hvm1Config   = addInstrument("Hall Voltmeter 1", VMeter::class)
    private val hvm2Config   = addInstrument("Hall Voltmeter 2", VMeter::class)
    private val fpp1Config   = addInstrument("Four-Point Probe 1", VMeter::class)
    private val fpp2Config   = addInstrument("Four-Point Probe 2", VMeter::class)
    private val tMeterConfig = addInstrument("Thermometer", TMeter::class)

    // Getters to quickly retrieve parameter values - nice-to-have but not necessary (just makes the code look cleaner)
    private val intTime  get() = intTimeParam.value
    private val delTime  get() = (1e3 * delTimeParam.value).toInt() // Convert to milliseconds
    private val repTime  get() = (1e3 * repTimeParam.value).toInt() // Convert to milliseconds
    private val repeats  get() = repeatsParam.value
    private val fields   get() = fieldParam.value
    private val currents get() = currentParam.value
    private val gates    get() = gateParam.value

    // Class variables/properties to hold onto hall voltmeters and em controller objects
    private var hvm1:   VMeter?       = null
    private var hvm2:   VMeter?       = null
    private var magnet: EMController? = null

    /**
     * Constants to refer to columns in this measurement's result table
     */
    companion object {

        const val SET_SD_CURRENT = 0
        const val SET_SG_VOLTAGE = 1
        const val SD_VOLTAGE     = 2
        const val SD_CURRENT     = 3
        const val SG_VOLTAGE     = 4
        const val SG_CURRENT     = 5
        const val FIELD          = 6
        const val HALL_1         = 7
        const val HALL_1_ERROR   = 8
        const val HALL_2         = 9
        const val HALL_2_ERROR   = 10
        const val FPP_1          = 11
        const val FPP_2          = 12
        const val TEMPERATURE    = 13

    }

    /**
     * Loads/applies all the needed instrument configurations just before the measurement is run
     */
    override fun loadInstruments() {

        gdSMU  = gdSMUConfig.get()
        sdSMU  = sdSMUConfig.get()
        sgSMU  = sgSMUConfig.get()
        hvm1   = hvm1Config.get()
        hvm2   = hvm2Config.get()
        fpp1   = fpp1Config.get()
        fpp2   = fpp2Config.get()
        tMeter = tMeterConfig.get()
        magnet = magnetConfig.get()

    }

    /**
     * Checks to see if anything is amiss before run(...) is called. If this returns anything other than an empty list
     * the measurement will just return an error and not run. In this case, we are checking to make sure the needed
     * instruments for the configured measurement are present (e.g. is there a source-gate channel if a gate sweep is
     * configured - for instance).
     */
    override fun checkForErrors(): List<String> {

        val errors = ArrayList<String>()

        // Source-Drain channel is always required
        if (sdSMU == null) {
            errors += "Source-Drain channel is not configured"
        }

        // Source-Gate channel is required if a gate voltage is to be used
        if (sgSMU == null && !(gates.min() == 0.0 && gates.max() == 0.0)) {
            errors += "Source-Gate channel is not configured"
        }

        // We need at least one Hall voltmeter
        if (hvm1 == null && hvm2 == null) {
            errors += "No Hall voltmeters are configured"
        }

        // Electromagnet controller is needed if more than one field value is to be used
        if (magnet == null && fields.min() != fields.max()) {
            errors += "No electromagnet controller configured"
        }

        return errors

    }

    /**
     * Defines the structure of the result table for this measurement - i.e. it returns the columns that the results
     * table should have.
     */
    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Set SD Current", "A"),
            Col("Set SG Voltage", "V"),
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Field Strength", "T"),
            Col("Hall Voltage 1", "V"),
            Col("Hall Voltage 1 Error", "V"),
            Col("Hall Voltage 2", "V"),
            Col("Hall Voltage 2 Error", "V"),
            Col("Four-Point Probe 1", "V"),
            Col("Four-Point Probe 2", "V"),
            Col("Temperature", "K")
        )

    }

    /**
     * The main bulk of the measurement control code - this is where the measurement happens. Is passed the ResultTable -
     * generated by using the columns returned by getColumns() above - as an argument.
     */
    override fun run(results: ResultTable) {

        // Save measurement parameters to result file
        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())
        results.setAttribute("Averaging Delay", "$repTime ms")

        // Source-Drain channel MUST be present (cannot be null)
        val sdSMU = sdSMU!!

        // Make sure everything starts in a safe off-state
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        hvm1?.turnOff()
        hvm2?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Set the integration time on everything
        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime  = intTime
        sgSMU?.integrationTime = intTime
        hvm1?.integrationTime  = intTime
        hvm2?.integrationTime  = intTime
        fpp1?.integrationTime  = intTime
        fpp2?.integrationTime  = intTime

        // Set the initial values of voltage and current
        gdSMU?.voltage = 0.0
        sdSMU.current  = currents.first()
        sgSMU?.voltage = gates.first()

        // Switch on everything
        gdSMU?.turnOn()
        sdSMU.turnOn()
        sgSMU?.turnOn()
        hvm1?.turnOn()
        hvm2?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (gate in gates) {

            gdSMU?.voltage = gate

            for (field in fields) {

                magnet?.field = field

                for (current in currents) {

                    sdSMU.current = current
                    sleep(delTime)

                    // Create arrays to hold repeat values
                    val hvm1Values = Array(repeats) { 0.0 }
                    val hvm2Values = Array(repeats) { 0.0 }

                    // Take repeat measurements of Hall voltages
                    for (n in 0 until repeats) {
                        hvm1Values[n] = hvm1?.voltage ?: Double.NaN
                        hvm2Values[n] = hvm2?.voltage ?: Double.NaN
                        sleep(repTime)
                    }

                    results.addData(
                        current,                             // Source-Drain Current (Set Value)
                        gate,                                // Source-Gate Voltage (Set Value)
                        sdSMU.voltage,                       // Source-Drain Voltage
                        sdSMU.current,                       // Source-Drain Current (Measured Value)
                        sgSMU?.voltage ?: Double.NaN,        // Source-Gate Voltage (Measured Value) - NaN if not used
                        sgSMU?.current ?: Double.NaN,        // Source-Gate Current - NaN if not used
                        magnet?.field ?: fields.first(),     // Magnetic field
                        hvm1Values.average(),                // Hall voltage 1 value (mean)
                        hvm1Values.stdDeviation(),           // Hall voltage 1 error (std. deviation)
                        hvm2Values.average(),                // Hall voltage 2 value (mean)
                        hvm2Values.stdDeviation(),           // Hall voltage 2 error (std. deviation)
                        fpp1?.voltage ?: Double.NaN,         // FPP1 - NaN if not used
                        fpp2?.voltage ?: Double.NaN,         // FPP2 - NaN if not used
                        tMeter?.temperature ?: Double.NaN    // Temperature - NaN if not used
                    )

                }

            }

        }

    }

    /**
     * Code to run if the measurement is stopped before completion - this happens when the user presses "stop"
     */
    override fun onInterrupt() {
        Util.errLog.println("DC Hall Measurement Interrupted.")
    }

    /**
     * Code that always runs after the measurement has finished - this will always run regardless of whether the
     * measurement finished successfully, was interrupted or failed with an error. Generally used to make sure everything
     * gets switched off etc.
     */
    override fun onFinish() {

        // "runRegardless" just makes sure any error given by any of these commands is ignored, otherwise one of them
        // failing would prevent the rest from running.
        runRegardless { sdSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { hvm1?.turnOff() }
        runRegardless { hvm2?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }
        runRegardless { magnet?.turnOff() }

    }

    /**
     * Custom modification so that when a new result file is created, it records whether this measurement will be
     * sweeping field or not - this is used by the plot to see whether it should plot against field or current. Normally
     * overriding this is not needed.
     */
    override fun newResults(path: String?): ResultTable {
        val results =  super.newResults(path)
        results.setAttribute("Field Sweep", if (fields.max() != fields.min()) "true" else "false")
        return results
    }

    /**
     * Custom extension to arrays of doubles for calculating standard deviation.
     */
    private fun Array<out Double>.stdDeviation(): Double {

        if (size < 2) {
            return 0.0
        }

        val mean = average()
        var sum = 0.0

        for (value in this) sum += (value - mean).pow(2)

        return sqrt(sum / (size - 1))

    }


}