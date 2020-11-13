package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.TMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Configurator
import jisa.maths.Range
import org.oefet.fetch.gui.tabs.Connections
import java.lang.Exception

class VSync : FMeasurement() {

    override val type = "Sync"

    private val label   = StringParameter("Basic", "Label", null, "Sync")
    private val paramIntTime = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    private val paramDelTime = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    private val paramMinVSD  = DoubleParameter("Source-Drain", "Start", "V", 0.0)
    private val paramMaxVSD  = DoubleParameter("Source-Drain", "Stop", "V", 60.0)
    private val paramNumVSD  = IntegerParameter("Source-Drain", "No. Steps", null, 7)
    private val paramSymVSD  = BooleanParameter("Source-Drain", "Sweep Both Ways", null, true)
    private val paramOffset  = DoubleParameter("Source-Gate", "Offset", "V", 0.0)

    val gdSMUConfig = addInstrument(Configurator.SMU("Ground Channel (SPA)", Connections))
    val sdSMUConfig = addInstrument(Configurator.SMU("Source-Drain Channel", Connections))
    val sgSMUConfig = addInstrument(Configurator.SMU("Source-Gate Channel", Connections))
    val fpp1Config  = addInstrument(Configurator.VMeter("Four-Point-Probe Channel 1", Connections))
    val fpp2Config  = addInstrument(Configurator.VMeter("Four-Point-Probe Channel 2", Connections))

    val intTime get() = paramIntTime.value
    val delTime get() = (paramDelTime.value * 1000).toInt()
    val minVSD  get() = paramMinVSD .value
    val maxVSD  get() = paramMaxVSD .value
    val numVSD  get() = paramNumVSD .value
    val symVSD  get() = paramSymVSD .value
    val offset  get() = paramOffset .value

    override fun loadInstruments() {

        gdSMU    = gdSMUConfig.get()
        sdSMU    = sdSMUConfig.get()
        sgSMU    = sgSMUConfig.get()
        fpp1     = fpp1Config.get()
        fpp2     = fpp2Config.get()
        tMeter   = Instruments.tMeter

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

        val voltages = if (symVSD) {
            Range.linear(minVSD, maxVSD, numVSD).mirror()
        } else {
            Range.linear(minVSD, maxVSD, numVSD)
        }

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSD + offset
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

        for (vSD in voltages) {

            val vSG = vSD + offset

            sdSMU.voltage = vSD
            sgSMU.voltage = vSG

            sleep(delTime)

            results.addData(
                vSD,
                vSG,
                sdSMU.voltage,
                sdSMU.current,
                sgSMU.voltage,
                sgSMU.current,
                fpp1?.voltage ?: Double.NaN,
                fpp2?.voltage ?: Double.NaN,
                tMeter?.temperature ?: Double.NaN,
                gdSMU?.current ?: Double.NaN
            )

        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun getLabel(): String = label.value

    override fun getName(): String = "Synced Voltage Measurement"

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

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun onInterrupt() {

        Util.errLog.println("Synced voltage measurement interrupted.")

    }

}
