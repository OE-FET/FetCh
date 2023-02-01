package org.oefet.fetch.action

import javafx.scene.image.Image
import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.SMU
import jisa.gui.Colour
import jisa.gui.GUI
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class VoltageHold : FetChAction("Hold", Image(GUI::class.java.getResource("images/smu.png")?.toString())) {

    var task: RTask? = null

    // Parameters
    val time      by userTimeInput("Basic", "Hold Time", 600000)
    val interval  by userTimeInput("Basic", "Logging Interval", 500)
    val useSD     by userInput("Source-Drain", "Enabled", false)
    val sdVoltage by userInput("Source-Drain", "Voltage [V]", 50.0)
    val useSG     by userInput("Source-Gate", "Enabled", false)
    val sgVoltage by userInput("Source-Gate", "Voltage [V]", 50.0)

    // Instruments
    val sdSMU by optionalInstrument("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { useSG }

    // Data columns
    companion object : Columns() {
        val TIME       = decimalColumn("Time", "s")
        val SD_VOLTAGE = decimalColumn("Source-Drain Voltage", "V")
        val SG_VOLTAGE = decimalColumn("Source-Gate Voltage", "V")
    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        return FetChPlot("Hold Voltages", "Time [s]", "Voltage [V]").apply {

            createSeries()
                .watch(data, TIME, SD_VOLTAGE)
                .setName("Source-Drain")
                .setMarkerVisible(false)
                .setColour(Colour.ORANGERED)

            createSeries()
                .watch(data, TIME, SG_VOLTAGE)
                .setName("Source-Gate")
                .setMarkerVisible(false)
                .setColour(Colour.CORNFLOWERBLUE)

            isLegendVisible = true

        }

    }

    override fun run(results: ResultTable) {

        task = RTask(interval.toLong()) { t ->

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

    override fun getLabel(): String {

        return buildString {
            if (useSD) append("SD = $sdVoltage V ")
            if (useSG) append("SG = $sgVoltage V ")
            append("for ")
            append(Util.msToString(time.toLong()))
        }

    }

}