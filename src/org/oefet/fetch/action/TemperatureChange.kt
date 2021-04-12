package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class TemperatureChange : FetChAction("Change Temperature") {

    var task: RTask? = null

    val temperature   by input("Temperature", "Set-Point [K]", 100.0)
    val interval      by input("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct  by input("Stability", "Stays within [%]", 1.0)
    val stabilityTime by input("Stability", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by requiredConfig("Temperature Controller", TC::class)

    override fun createPlot(data: ResultTable): FetChPlot {

        val plot =  FetChPlot("Change Temperature to $temperature K", "Time [s]", "Temperature [K]")

        plot.createSeries()
            .watch(data, { it[0] }, { (1 + (stabilityPct / 100.0)) * temperature })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, { it[0] }, { (1 - (stabilityPct / 100.0)) * temperature })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, 0, 1)
            .setMarkerVisible(false)
            .setColour(Colour.RED)

        plot.createSeries()
            .watch(data, 0, 1)
            .filter { Util.isBetween(it[1], (1 - (stabilityPct / 100.0)) * temperature, (1 + (stabilityPct / 100.0)) * temperature) }
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

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Time","s"),
            Col("Temperature", "K")
        )

    }

    override fun getLabel(): String {
        return "$temperature K"
    }

}