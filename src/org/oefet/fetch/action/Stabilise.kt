package org.oefet.fetch.action

import javafx.scene.image.Image
import jisa.Util
import jisa.control.RTask
import jisa.devices.meter.IMeter
import jisa.devices.source.VSource
import jisa.gui.Colour
import jisa.gui.Element
import jisa.gui.GUI
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class Stabilise : FetChAction("Current Stabilisation", Image(GUI::class.java.getResource("images/smu.png").toString())) {

    private val setVoltage by userInput("Set Voltage [V]", 1.0)
    private val pctRange   by userInput("Stable to [%]", 1.0)
    private val stabTime   by userTimeInput("For at least", 600000)
    private val logTime    by userTimeInput("Logging Interval", 1000)
    private val autoOff    by userInput("Auto Off?", true)

    private val vSource    by optionalInstrument("Voltage Source", VSource::class)
    private val iMeter     by requiredInstrument("Ammeter", IMeter::class)

    private var logger     = RTask(1000) { _ -> }

    companion object {
        val TIME    = DoubleColumn("Time", "s")
        val CURRENT = DoubleColumn("Current", "A")
    }

    override fun getColumns(): Array<Column<*>> = arrayOf(TIME, CURRENT)

    override fun createDisplay(data: ResultTable): Element {

        val plot = FetChPlot("Current Stabilisation", "Time [s]", "Current [A]")

        plot.createSeries()
            .watch(data, TIME, CURRENT)
            .setColour(Colour.BLUE)
            .setMarkerVisible(false)
            .setLineVisible(true)

        plot.isLegendVisible = false

        return plot

    }

    override fun run(results: ResultTable) {

        vSource?.voltage = setVoltage
        vSource?.turnOn()
        iMeter.turnOn()

        logger = RTask(logTime.toLong()) { task -> results.mapRow(
            TIME    to task.secFromStart,
            CURRENT to iMeter.current
        ) }

        logger.start()

        iMeter.waitForStableCurrent(pctRange, logTime, stabTime)

    }

    override fun onFinish() {

        logger.stop()

        if (autoOff) {
            vSource?.turnOff()
            iMeter.turnOff()
        }

    }

    override fun getLabel(): String {
        return "$pctRange% for ${Util.msToString(stabTime.toLong())}"
    }

}