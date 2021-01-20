package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import java.lang.Exception

class Transfer : FMeasurement("Transfer Measurement", "Transfer", "Transfer") {

    private val intTimeParam = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val delTimeParam = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val sdvParam     = RangeParameter("Source-Drain", "Voltage", "V", 0.0, 60.0, 7, Range.Type.LINEAR, 1)
    private val sgvParam     = RangeParameter("Source-Gate", "Voltage", "V", 0.0, 60.0, 61, Range.Type.LINEAR, 1)
    private val symVSGParam  = BooleanParameter("Source-Gate", "Sweep Both Ways", null, true)

    val gdSMUConfig = addInstrument("Ground Channel (SPA)", SMU::class.java)
    val sdSMUConfig = addInstrument("Source-Drain Channel", SMU::class.java)
    val sgSMUConfig = addInstrument("Source-Gate Channel", SMU::class.java)
    val fpp1Config  = addInstrument("Four-Point Probe Channel 1", VMeter::class.java)
    val fpp2Config  = addInstrument("Four-Point Probe Channel 2", VMeter::class.java)
    private val tMeterConfig  = addInstrument("Thermometer", TMeter::class.java)


    val intTime get()    = intTimeParam.value
    val delTime get()    = (1e3 * delTimeParam.value).toInt()
    val sdVoltages get() = sdvParam.value
    val sgVoltages get() = if (symVSGParam.value) sgvParam.value.mirror() else sgvParam.value

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

        results.setAttribute("Integration Time", "$intTime s")
        results.setAttribute("Delay Time", "$delTime ms")

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = sdVoltages.first()
        sgSMU.voltage = sgVoltages.first()
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

        for (vSD in sdVoltages) {

            sdSMU.voltage = vSD

            for (vSG in sgVoltages) {

                sgSMU.voltage = vSG

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

}