package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range

class Output : FMeasurement() {

    override val type = "Output"

    // Measurement Parameters
    private val label = StringParameter("Basic", "Label", null, "Output")
    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val minVSDParam  = DoubleParameter("Source-Drain", "Start", "V", 0.0)
    private val maxVSDParam  = DoubleParameter("Source-Drain", "Stop", "V", 60.0)
    private val numVSDParam  = IntegerParameter("Source-Drain", "No. Steps", null, 61)
    private val symVSDParam  = BooleanParameter("Source-Drain", "Sweep Both Ways", null, true)
    private val minVSGParam  = DoubleParameter("Source-Gate", "Start", "V", 0.0)
    private val maxVSGParam  = DoubleParameter("Source-Gate", "Stop", "V", 60.0)
    private val numVSGParam  = IntegerParameter("Source-Gate", "No. Steps", null, 7)

    val intTime get() = intTimeParam.value
    val delTime get() = (1e3 * delTimeParam.value).toInt()
    val minVSD get()  = minVSDParam.value
    val maxVSD get()  = maxVSDParam.value
    val numVSD get()  = numVSDParam.value
    val symVSD get()  = symVSDParam.value
    val minVSG get()  = minVSGParam.value
    val maxVSG get()  = maxVSGParam.value
    val numVSG get()  = numVSGParam.value

    override fun loadInstruments() {

        gdSMU    = Instruments.gdSMU
        sdSMU    = Instruments.sdSMU
        sgSMU    = Instruments.sgSMU
        fpp1     = Instruments.fpp1
        fpp2     = Instruments.fpp2
        tMeter   = Instruments.tMeter

    }

    override fun checkForErrors() : List<String> {

        val errors = ArrayList<String>()

        if (sdSMU == null) errors += "SD channel not configured"
        if (sgSMU == null) errors += "SG channel not configured"

        return errors

    }

    override fun run(results: ResultTable) {

        // Assert that source-drain and source-gate must be connected
        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        val sdVoltages = if (symVSD) {
            Range.linear(minVSD, maxVSD, numVSD).mirror()
        } else {
            Range.linear(minVSD, maxVSD, numVSD)
        }

        val sgVoltages = Range.linear(minVSG, maxVSG, numVSG)

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSG
        gdSMU?.voltage = 0.0

        // Configure integration times
        sdSMU.integrationTime = intTime
        sgSMU.integrationTime = intTime
        fpp1?.integrationTime = intTime
        fpp2?.integrationTime = intTime

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSG in sgVoltages) {

            sgSMU.voltage = vSG

            for (vSD in sdVoltages) {

                sdSMU.voltage = vSD

                sleep(delTime)

                results.addData(
                    vSD, vSG,
                    sdSMU.voltage, sdSMU.current,
                    sgSMU.voltage, sgSMU.current,
                    fpp1?.voltage ?: Double.NaN, fpp2?.voltage ?: Double.NaN,
                    tMeter?.temperature ?: Double.NaN,
                    gdSMU?.current ?: Double.NaN
                )

            }

        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun getLabel(): String = label.value

    override fun getName(): String = "Output Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Set SD Voltage", "V"),
            Col("Set SG Voltage", "V"),
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Four Point Probe 1", "V"),
            Col("Four Point Probe 2", "V"),
            Col("Temperature", "K"),
            Col("Ground Current", "A")
        )

    }

    override fun onInterrupt() {

        Util.errLog.println("Transfer measurement interrupted.")

    }

}
