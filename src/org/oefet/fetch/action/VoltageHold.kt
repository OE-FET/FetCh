package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.SMU
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import org.oefet.fetch.gui.elements.FetChPlot

class VoltageHold : Action("Hold") {

    var task: RTask? = null

    val time      by input("Basic", "Hold Time [s]", 600.0) map { it.toMSec() }
    val useSD     by input("Source-Drain", "Enabled", false)
    val sdVoltage by input("Source-Drain", "Voltage [V]", 50.0)
    val useSG     by input("Source-Gate", "Enabled", false)
    val sgVoltage by input("Source-Gate", "Voltage [V]", 50.0)

    val sdSMU by optionalConfig("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalConfig("Source-Gate Channel", SMU::class)  requiredIf { useSG }

    override fun createPlot(data: ResultTable): FetChPlot {

        return FetChPlot("Hold Voltages", "Time [s]", "Voltage [V]").apply {
            createSeries().watch(data, 0, 1).setName("Source-Drain").setMarkerVisible(false).setColour(Colour.ORANGERED)
            createSeries().watch(data, 0, 2).setName("Source-Gate").setMarkerVisible(false).setColour(Colour.CORNFLOWERBLUE)
            isLegendVisible = true
        }

    }

    override fun run(results: ResultTable) {

        task = RTask(2500) { t ->

            results.addData(
                t.secFromStart,
                sdSMU?.voltage ?: 0.0,
                sgSMU?.voltage ?: 0.0
            )

        }

        task?.start()

        sdSMU?.turnOff()
        sgSMU?.turnOff()

        sdSMU?.voltage = 0.0
        sgSMU?.voltage = 0.0

        if (useSD) {
            sdSMU?.voltage = sdVoltage
            sdSMU?.turnOn()
        }

        if (useSG) {
            sgSMU?.voltage = sgVoltage
            sgSMU?.turnOn()
        }

        sleep(time)

    }

    override fun onFinish() {
        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        task?.stop()
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Time","s"),
            Col("Source-Drain Voltage", "V"),
            Col("Source-Gate Voltage", "V")
        )

    }

    override fun getLabel(): String {
        return "${if (useSD) "SD = $sdVoltage V " else ""}${if (useSG) "SG = $sgVoltage V " else ""}for ${Util.msToString(time.toLong())}"
    }

}