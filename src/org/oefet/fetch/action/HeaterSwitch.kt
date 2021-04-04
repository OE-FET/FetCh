package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.power.IPS120
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FetChPlot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class HeaterSwitch : Action("Heater Switch") {

    var task: RTask? = null

    val on  by input ("Basic", "On", false)
    val ips by requiredConfig("IPS", IPS120::class)

    val plot = FetChPlot("Heater Switch").apply { isLegendVisible = false; pointOrdering = Plot.Sort.ORDER_ADDED; }

    override fun createPlot(data: ResultTable): FetChPlot = plot

    override fun run(results: ResultTable) {

        plot.clear()

        val background = plot.createSeries().setLineWidth(30.0).setColour(Colour.GREY).setMarkerVisible(false)
        val fill       = plot.createSeries().setLineWidth(20.0).setColour(Colour.BLUE).setMarkerVisible(false)

        background.addPoint(0.0, 0.0).addPoint(30.0, 0.0)

        task = RTask((300).toLong()) { t -> fill.addPoint(t.secFromStart, 0.0) }

        task?.start()

        ips.setHeater(on)

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

    override fun getLabel(): String {
        return if (on) "On" else "Off"
    }

}