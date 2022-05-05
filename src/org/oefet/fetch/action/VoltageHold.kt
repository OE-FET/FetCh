package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.SMU

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Colour
import jisa.results.Column
import org.oefet.fetch.gui.elements.FetChPlot

class VoltageHold : FetChAction("Hold") {

    var task: RTask? = null

    val time      by userInput("Basic", "Hold Time [s]", 600.0) map { it.toMSec() }
    val interval  by userInput("Basic", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val useSD     by userInput("Source-Drain", "Enabled", false)
    val sdVoltage by userInput("Source-Drain", "Voltage [V]", 50.0)
    val useSG     by userInput("Source-Gate", "Enabled", false)
    val sgVoltage by userInput("Source-Gate", "Voltage [V]", 50.0)

    val sdSMU by optionalInstrument("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalInstrument("Source-Gate Channel", SMU::class)  requiredIf { useSG }

    companion object {
        val TIME       = DoubleColumn("Time","s")
        val SD_VOLTAGE = DoubleColumn("Source-Drain Voltage", "V")
        val SG_VOLTAGE = DoubleColumn("Source-Gate Voltage", "V")
    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        return FetChPlot("Hold Voltages", "Time [s]", "Voltage [V]").apply {
            createSeries().watch(data, TIME, SD_VOLTAGE).setName("Source-Drain").setMarkerVisible(false).setColour(Colour.ORANGERED)
            createSeries().watch(data, TIME, SG_VOLTAGE).setName("Source-Gate").setMarkerVisible(false).setColour(Colour.CORNFLOWERBLUE)
            isLegendVisible = true
        }

    }

    override fun run(results: ResultTable) {

        task = RTask(interval) { t ->

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

    override fun getColumns(): Array<Column<*>> {
        return arrayOf(TIME, SD_VOLTAGE, SG_VOLTAGE)
    }

    override fun getLabel(): String {
        return "${if (useSD) "SD = $sdVoltage V " else ""}${if (useSG) "SG = $sgVoltage V " else ""}for ${Util.msToString(time.toLong())}"
    }

}