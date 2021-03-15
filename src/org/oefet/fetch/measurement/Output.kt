package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections

class Output : FMeasurement("Output Measurement", "Output", "Output") {

    // Measurement Parameters
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val sdvParam     = RangeParameter("Source-Drain", "Voltage", "V", 0.0, 60.0, 61, Range.Type.LINEAR, 1)
    private val symVSDParam  = BooleanParameter("Source-Drain", "Sweep Both Ways", null, true)
    private val sgvParam     = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 60.0, 7, Range.Type.LINEAR, 1)

    private val gdSMUConfig   = addInstrument("Ground Channel (SPA)", SMU::class.java)
    private val sdSMUConfig   = addInstrument("Source-Drain Channel", SMU::class.java)
    private val sgSMUConfig   = addInstrument("Source-Gate Channel", SMU::class.java)
    private val fpp1Config    = addInstrument("Four-Point Probe Channel 1", VMeter::class.java)
    private val fpp2Config    = addInstrument("Four-Point Probe Channel 2", VMeter::class.java)
    private val tMeterConfig  = addInstrument("Thermometer", TMeter::class.java)

    val delTime    get() = (1e3 * delTimeParam.value).toInt()
    val sdVoltages get() = sdvParam.value
    val symVSD     get() = symVSDParam.value
    val sgVoltages get() = sgvParam.value

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        sgSMU    = sgSMUConfig.get()
        fpp1     = fpp1Config.get()
        fpp2     = fpp2Config.get()
        tMeter   = tMeterConfig.get()

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

        results.setAttribute("Integration Time", "${sdSMU.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = sdVoltages.first()
        sgSMU.voltage = sgVoltages.first()
        gdSMU?.voltage = 0.0

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSG in sgVoltages) {

            sgSMU.voltage = vSG

            for (vSD in (if (symVSD) sdVoltages.mirror() else sdVoltages)) {

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

    override fun onError() {

    }

}
