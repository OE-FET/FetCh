package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import kotlin.math.pow
import kotlin.math.sqrt

class DCHall : FMeasurement("DC Hall Measurement", "DCHall", "DC Hall") {

    // Measurement parameters to ask user for
    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 1.0 / 50.0)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val repeatsParam = IntegerParameter("Basic", "Repeats", null, 50)
    private val repTimeParam = DoubleParameter("Basic", "Repeat Time", "s", 0.0)
    private val fieldParam   = RangeParameter("Magnet", "Field", "T", -1.0, +1.0, 11, Range.Type.LINEAR, 1)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -50e-6, +50e-6, 11, Range.Type.LINEAR, 1)
    private val gateParam    = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 0.0, 1, Range.Type.LINEAR, 1)

    // Getters to quickly retrieve parameter values
    private val intTime  get() = intTimeParam.value
    private val delTime  get() = (1e3 * delTimeParam.value).toInt()
    private val repTime  get() = (1e3 * repTimeParam.value).toInt()
    private val repeats  get() = repeatsParam.value
    private val fields   get() = fieldParam.value
    private val currents get() = currentParam.value
    private val gates    get() = gateParam.value

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

    private var hvm1:   VMeter?       = null
    private var hvm2:   VMeter?       = null
    private var magnet: EMController? = null

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
     * Checks to see if anything is amiss before run(...) is called.
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
     * The main bulk of the measurement control code
     */
    override fun run(results: ResultTable) {

        // Save measurement parameters to result file
        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())

        // Source-Drain channel MUST be present (cannot be null)
        val sdSMU = sdSMU!!

        // Make sure everything starts in a safe off-state
        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        hvm1?.turnOff()
        hvm2?.turnOff()
        magnet?.turnOff()

        // Set the integration time on everything
        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime  = intTime
        sgSMU?.integrationTime = intTime
        hvm1?.integrationTime  = intTime
        hvm2?.integrationTime  = intTime

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

        for (gate in gates) {

            gdSMU?.voltage = gate

            for (field in fields) {

                magnet?.field = field

                for (current in currents) {

                    sdSMU.current = current
                    sleep(delTime)

                    // Take repeat measurements of Hall voltages
                    val hvm1Values = Array(repeats) { 0.0 }
                    val hvm2Values = Array(repeats) { 0.0 }

                    repeat(repeats) { n ->
                        sleep(repTime)
                        hvm1Values[n] = hvm1?.voltage ?: Double.NaN
                        hvm2Values[n] = hvm2?.voltage ?: Double.NaN
                    }

                    results.addData(
                        current,
                        gate,
                        sdSMU.voltage,                       // Source-Drain Voltage
                        current,                             // Source-Drain Current
                        gate,                                // Source-Gate Voltage
                        sgSMU?.current ?: Double.NaN,        // Source-Gate Current
                        magnet?.field ?: fields.first(),     // Magnetic field
                        hvm1Values.average(),                // Hall voltage 1 value (mean)
                        hvm1Values.stdDeviation(),           // Hall voltage 1 error (std. deviation)
                        hvm2Values.average(),                // Hall voltage 2 value (mean)
                        hvm2Values.stdDeviation(),           // Hall voltage 2 error (std. deviation)
                        fpp1?.voltage ?: Double.NaN,         // FPP1
                        fpp2?.voltage ?: Double.NaN,         // FPP2
                        tMeter?.temperature ?: Double.NaN    // Temperature
                    )

                }

            }

        }

    }

    /**
     * Code to run if the measurement is stopped before completion
     */
    override fun onInterrupt() {
        Util.errLog.println("DC Hall Measurement Interrupted.")
    }

    /**
     * Code that always runs after the measurement has finished
     */
    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { hvm1?.turnOff() }
        runRegardless { hvm2?.turnOff() }
        runRegardless { magnet?.turnOff() }

    }

    /**
     * Defines the structure of the result table for this measurement
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
            Col("Temperature", "K"),
            Col("Four-Point Probe 1", "V"),
            Col("Four-Point Probe 2", "V")
        )

    }

    private fun Array<out Double>.stdDeviation(): Double {

        if (size < 2) {
            return 0.0
        }

        val mean = average()
        var sum = 0.0

        for (value in this) sum += (value - mean).pow(2)

        return sqrt(sum / (size - 1))

    }

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


}