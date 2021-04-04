package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FetChPlot

class TemperatureChange : Action("Change Temperature") {

    var task: RTask? = null

    val temperature   by input("Temperature", "Set-Point [K]", 100.0)
    val stabilityPct  by input("Stability", "Stays within [%]", 1.0)
    val stabilityTime by input("Stability", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by requiredConfig("Temperature Controller", TC::class)

    override fun createPlot(data: ResultTable): FetChPlot {

        return FetChPlot("Change Temperature to $temperature K", "Time [s]", "Temperature [K]").apply {
            createSeries().watch(data, 0, 1).setMarkerVisible(false).setColour(Colour.BLUE)
            isLegendVisible = false
        }

    }

    override fun run(results: ResultTable) {

        task = RTask(2500) { t -> results.addData(t.secFromStart, tControl.temperature) }
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