package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.LongColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class TemperatureChange : FetChAction("Change Temperature") {

    var task: RTask? = null

    val temperature by userInput("Temperature", "Set-Point [K]", 100.0)
    val interval by userInput("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct by userInput("Stability", "Stays within [%]", 1.0)
    val stabilityTime by userInput("Stability", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl by requiredInstrument("Temperature Controller", TC::class)

    private var series: Series? = null

    companion object {
        val TIME        = LongColumn("Time", "UTC ms")
        val TEMPERATURE = DoubleColumn("Temperature", "K")
    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        val plot = FetChPlot("Change Temperature to $temperature K", "Time", "Temperature [K]")

        plot.isLegendVisible = false
        plot.xAxisType = Plot.AxisType.TIME

        plot.createSeries()
            .watch(data, { it[TIME] / 1000.0 }, { (1 + (stabilityPct / 100.0)) * temperature })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, { it[TIME] / 1000.0 }, { (1 - (stabilityPct / 100.0)) * temperature })
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

        task = RTask(interval) { _ ->

            val t = tControl.temperature

            results.addData(System.currentTimeMillis(), t)

            if (Util.isBetween(
                    t,
                    (1 - (stabilityPct / 100.0)) * temperature,
                    (1 + (stabilityPct / 100.0)) * temperature
                )
            ) {
                series?.colour = Colour.TEAL
            } else {
                series?.colour = Colour.RED
            }

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