package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Colour
import jisa.gui.Series
import jisa.results.Column
import org.oefet.fetch.gui.elements.FetChPlot

class TemperatureChange : FetChAction("Change Temperature") {

    var task: RTask? = null

    val temperature   by userInput("Temperature", "Set-Point [K]", 100.0)
    val interval      by userInput("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct  by userInput("Stability", "Stays within [%]", 1.0)
    val stabilityTime by userInput("Stability", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by requiredInstrument("Temperature Controller", TC::class)

    companion object {
        val TIME        = DoubleColumn("Time","s")
        val TEMPERATURE = DoubleColumn("Temperature", "K")
    }

    override fun createPlot(data: ResultTable): FetChPlot {

        val plot =  FetChPlot("Change Temperature to $temperature K", "Time [s]", "Temperature [K]")

        plot.createSeries()
            .watch(data, { it[TIME] }, { (1 + (stabilityPct / 100.0)) * temperature })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, { it[TIME] }, { (1 - (stabilityPct / 100.0)) * temperature })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, TIME, TEMPERATURE)
            .setMarkerVisible(false)
            .setColour(Colour.RED)

        plot.createSeries()
            .watch(data, TIME, TEMPERATURE)
            .filter { Util.isBetween(it[TEMPERATURE], (1 - (stabilityPct / 100.0)) * temperature, (1 + (stabilityPct / 100.0)) * temperature) }
            .setMarkerVisible(false)
            .setLineWidth(3.0)
            .setColour(Colour.MEDIUMSEAGREEN)

        plot.isLegendVisible = false

        return plot

    }

    override fun run(results: ResultTable) {

        task = RTask(interval) { t ->
            results.addData(t.secFromStart, tControl.temperature)
        }

        task?.start()

        tControl.temperature = temperature
        tControl.useAutoHeater()

        tControl.waitForStableTemperature(temperature, stabilityPct, stabilityTime)

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Column<*>> {
        return arrayOf(TIME, TEMPERATURE)
    }

    override fun getLabel(): String {
        return "$temperature K"
    }

}