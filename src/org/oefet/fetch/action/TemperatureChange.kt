package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.enums.Icon
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class TemperatureChange : FetChAction("Change Temperature", Icon.THERMOMETER.blackImage) {

    var task: RTask? = null

    val temperature   by userInput("Temperature", "Set-Point [K]", 100.0)
    val interval      by userTimeInput("Temperature", "Logging Interval", 500)
    val stabilityPct  by userInput("Stability", "Stays within [%]", 1.0)
    val stabilityTime by userTimeInput("Stability", "For at least", 600000)
    val tControl      by requiredInstrument("Temperature Controller", TC.Loop::class)

    private var series: Series? = null

    companion object : Columns() {
        val TIME        = longColumn("Time", "UTC ms")
        val TEMPERATURE = decimalColumn("Temperature", "K")
    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        val plot  = FetChPlot("Change Temperature to $temperature K", "Time", "Temperature [K]")
        val min   = (1 - (stabilityPct / 100.0)) * temperature
        val max   = (1 + (stabilityPct / 100.0)) * temperature

        plot.isLegendVisible = false
        plot.xAxisType       = Plot.AxisType.TIME

        plot.createSeries()
            .watch(data, { it[TIME] / 1000.0 }, { max })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, { it[TIME] / 1000.0 }, { min })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        series = plot.createSeries()
            .watch(data, { it[TIME] / 1000.0 }, { it[TEMPERATURE] })
            .setMarkerVisible(false)
            .setColour(Colour.RED)

        return plot

    }

    override fun run(results: ResultTable) {

        val input = tControl.input
        val min   = (1 - (stabilityPct / 100.0)) * temperature
        val max   = (1 + (stabilityPct / 100.0)) * temperature

        task = RTask(interval.toLong()) { _ ->

            val t = input.value

            results.addData(System.currentTimeMillis(), t)

            if (Util.isBetween(t, min, max)) {
                series?.colour = Colour.TEAL
            } else {
                series?.colour = Colour.RED
            }

        }

        task?.start()

        tControl.temperature  = temperature
        tControl.isPIDEnabled = true

        tControl.waitForStableTemperature(temperature, stabilityPct, stabilityTime.toLong())

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getLabel(): String {
        return "$temperature K"
    }

}