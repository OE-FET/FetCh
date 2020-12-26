package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.EMController
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import kotlin.math.pow
import kotlin.math.sqrt

class DCHall : FMeasurement() {

    override val type: String = "DC Hall"

    private val label = StringParameter("Basic", "Name", null, "DCHall")
    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 1.0 / 50.0)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val repeatsParam = IntegerParameter("Basic", "Repeats", null, 50)
    private val fieldParam   = RangeParameter("Magnet", "Field", "T", -1.0, +1.0, 11, Range.Type.LINEAR, 1)
    private val currentParam = RangeParameter("Source-Drain", "Current", "A", -50e-6, +50e-6, 11, Range.Type.LINEAR, 1)
    private val gateParam    = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 0.0, 1, Range.Type.LINEAR, 1)

    val intTime  get() = intTimeParam.value
    val delTime  get() = (1e3 * delTimeParam.value).toInt()
    val repeats  get() = repeatsParam.value
    val fields   get() = fieldParam.value
    val currents get() = currentParam.value
    val gates    get() = gateParam.value

    private var hvm1: VMeter? = null
    private var hvm2: VMeter? = null
    private var magnet: EMController? = null


    private val magnetConfig = addInstrument("Magnet Controller", EMController::class.java)
    private val gdSMUConfig  = addInstrument("Ground Channel (SPA)", SMU::class.java)
    private val sdSMUConfig  = addInstrument("Source-Drain Channel", SMU::class.java)
    private val sgSMUConfig  = addInstrument("Source-Gate Channel", SMU::class.java)
    private val hvm1Config   = addInstrument("Hall Voltmeter 1", VMeter::class.java)
    private val hvm2Config   = addInstrument("Hall Voltmeter 2", VMeter::class.java)
    private val fpp1Config   = addInstrument("Four-Point Probe Channel 1", VMeter::class.java)
    private val fpp2Config   = addInstrument("Four-Point Probe Channel 2", VMeter::class.java)
    private val tMeterConfig = addInstrument("Thermometer", TMeter::class.java)

    override fun loadInstruments() {

        gdSMU = gdSMUConfig.get()
        sdSMU = sdSMUConfig.get()
        sgSMU = sgSMUConfig.get()
        hvm1 = hvm1Config.get()
        hvm2 = hvm2Config.get()
        fpp1 = fpp1Config.get()
        fpp2 = fpp2Config.get()
        tMeter = tMeterConfig.get()
        magnet = magnetConfig.get()

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

    override fun checkForErrors(): List<String> {

        val errors = ArrayList<String>()

        if (sdSMU == null) errors += "Source-Drain channel is not configured"
        if (sgSMU == null && !(gates.min() == 0.0 && gates.max() == 0.0)) errors += "Source-Gate channel is not configured"
        if (hvm1 == null && hvm2 == null) errors += "No Hall voltmeters are configured"
        if (magnet == null && fields.min() != fields.max()) errors += "No electromagnet controller configured"

        return errors

    }

    override fun getName(): String {
        return "DC Hall Measurement"
    }

    override fun getLabel(): String {
        return label.value
    }

    override fun setLabel(value: String) {
        label.value = value
    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")
        results.setAttribute("Averaging Count", repeats.toDouble())

        val sdSMU = sdSMU!!

        gdSMU?.turnOff()
        sdSMU.turnOff()
        sgSMU?.turnOff()
        hvm1?.turnOff()
        hvm2?.turnOff()
        magnet?.turnOff()

        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime = intTime
        sgSMU?.integrationTime = intTime
        hvm1?.integrationTime = intTime
        hvm2?.integrationTime = intTime

        gdSMU?.voltage = 0.0
        sdSMU.current  = currents.first()
        sgSMU?.voltage = gates.first()

        gdSMU?.turnOn()
        sdSMU.turnOn()
        sgSMU?.turnOn()

        for (gate in gates) {

            gdSMU?.voltage = gate

            for (field in fields) {

                magnet?.field = field

                for (current in currents) {

                    sdSMU.current = current
                    sleep(delTime)

                    val hvm1Values = Array(repeats) { 0.0 }
                    val hvm2Values = Array(repeats) { 0.0 }

                    repeat(repeats) { n ->

                        hvm1Values[n] = hvm1?.voltage ?: Double.NaN
                        hvm2Values[n] = hvm2?.voltage ?: Double.NaN

                    }



                    results.addData(
                        sdSMU.voltage,
                        current,
                        gate,
                        sgSMU?.current ?: Double.NaN,
                        magnet?.field ?: fields.first(),
                        hvm1Values.average(),
                        hvm1Values.stdDeviation(),
                        hvm2Values.average(),
                        hvm2Values.stdDeviation(),
                        tMeter?.temperature ?: Double.NaN
                    )

                }

            }

        }

    }

    override fun onInterrupt() {
        Util.errLog.println("DC Hall Measurement Interrupted.")
    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { hvm1?.turnOff() }
        runRegardless { hvm2?.turnOff() }
        runRegardless { magnet?.turnOff() }

    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Field Strength", "T"),
            Col("Hall Voltage 1", "V"),
            Col("Hall Voltage 1 Error", "V"),
            Col("Hall Voltage 2", "V"),
            Col("Hall Voltage 2 Error", "V"),
            Col("Temperature", "K")
        )

    }

    companion object {

        const val SD_VOLTAGE = 0
        const val SD_CURRENT = 1
        const val SG_VOLTAGE = 2
        const val SG_CURRENT = 3
        const val FIELD = 4
        const val HALL_1 = 5
        const val HALL_1_ERROR = 6
        const val HALL_2 = 7
        const val HALL_2_ERROR = 8
        const val TEMPERATURE = 9

    }


}