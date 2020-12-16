package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.EMController
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

    private val label         = StringParameter("Basic", "Name", null, "DCHall")
    private val intTimeParam  = DoubleParameter("Basic", "Integration Time", "s", 1.0 / 50.0)
    private val delTimeParam  = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val repeatsParam  = IntegerParameter("Basic", "Repeats", null, 50)
    private val minFieldParam = DoubleParameter("Magnets", "Start", "T", -1.0)
    private val maxFieldParam = DoubleParameter("Magnets", "Stop", "T", +1.0)
    private val numFieldParam = IntegerParameter("Magnets", "No. Steps", null, 11)
    private val minIParam     = DoubleParameter("Source-Drain", "Start", "A", 0.0)
    private val maxIParam     = DoubleParameter("Source-Drain", "Stop", "A", 50e-6)
    private val numIParam     = IntegerParameter("Source-Drain", "No. Steps", null, 11)
    private val minGParam     = DoubleParameter("Source-Gate", "Start", "V", 0.0)
    private val maxGParam     = DoubleParameter("Source-Gate", "Stop", "V", 0.0)
    private val numGParam     = IntegerParameter("Source-Gate", "No. Steps", null, 1)

    val intTime get()  = intTimeParam.value
    val delTime get()  = (1e3 * delTimeParam.value).toInt()
    val repeats get()  = repeatsParam.value
    val minField get() = minFieldParam.value
    val maxField get() = maxFieldParam.value
    val numField get() = numFieldParam.value
    val minI get()     = minIParam.value
    val maxI get()     = maxIParam.value
    val numI get()     = numIParam.value
    val minG get()     = minGParam.value
    val maxG get()     = maxGParam.value
    val numG get()     = numGParam.value

    private var hvm1: VMeter? = null
    private var hvm2: VMeter? = null
    private var magnet: EMController? = null

    private val gdSMUConfig = addInstrument(Configurator.SMU("Ground Channel (SPA)", Connections))
    private val sdSMUConfig = addInstrument(Configurator.SMU("Source-Drain Channel", Connections))
    private val sgSMUConfig = addInstrument(Configurator.SMU("Source-Gate Channel", Connections))
    private val hvm1Config  = addInstrument(Configurator.VMeter("Hall Voltmeter 1", Connections))
    private val hvm2Config  = addInstrument(Configurator.VMeter("Hall Voltmeter 2", Connections))
    private val fpp1Config  = addInstrument(Configurator.VMeter("Four-Point-Probe 1", Connections))
    private val fpp2Config  = addInstrument(Configurator.VMeter("Four-Point-Probe 2", Connections))

    override fun loadInstruments() {

        gdSMU  = gdSMUConfig.get()
        sdSMU  = sdSMUConfig.get()
        sgSMU  = sgSMUConfig.get()
        hvm1   = hvm1Config.get()
        hvm2   = hvm2Config.get()
        fpp1   = fpp1Config.get()
        fpp2   = fpp2Config.get()
        tMeter = Instruments.tMeter
        magnet = Instruments.magnet

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
        if (sgSMU == null && !(minG == 0.0 && maxG == 0.0)) errors += "Source-Gate channel is not configured"
        if (hvm1 == null && hvm2 == null) errors += "No Hall voltmeters are configured"
        if (magnet == null && minField != maxField) errors += "No electromagnet controller configured"

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
        sdSMU.integrationTime  = intTime
        sgSMU?.integrationTime = intTime
        hvm1?.integrationTime  = intTime
        hvm2?.integrationTime  = intTime

        gdSMU?.voltage = 0.0
        sdSMU.current  = minI
        sgSMU?.voltage = minG

        gdSMU?.turnOn()
        sdSMU.turnOn()
        sgSMU?.turnOn()

        for (gate in Range.linear(minG, maxG, numG)) {

            gdSMU?.voltage = gate

            for (field in Range.linear(minField, maxField, numField)) {

                magnet?.field = field

                for (current in Range.linear(minI, maxI, numI)) {

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
                        magnet?.field  ?: minField,
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

        const val SD_VOLTAGE   = 0
        const val SD_CURRENT   = 1
        const val SG_VOLTAGE   = 2
        const val SG_CURRENT   = 3
        const val FIELD        = 4
        const val HALL_1       = 5
        const val HALL_1_ERROR = 6
        const val HALL_2       = 7
        const val HALL_2_ERROR = 8
        const val TEMPERATURE  = 9

    }


}