package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class Wait : FetChAction("Wait") {

    var task: RTask? = null

    val hours   by input ("Time", "Hours", 0)
    val minutes by input ("Time", "Minutes", 0)
    val seconds by input ("Time", "Seconds", 0)
    val millis  by input ("Time", "Milliseconds", 0)
    val time get() = millis + (seconds * 1000) + (minutes * 60 * 1000) + (hours * 60 * 60 * 1000)
    val plot       = FetChPlot("Wait").apply { isLegendVisible = false; pointOrdering = Plot.Sort.ORDER_ADDED; }

    override fun createPlot(data: ResultTable): FetChPlot {
        plot.title = "Wait for ${Util.msToString(time.toLong())}"
        return plot
    }

    override fun run(results: ResultTable) {

        plot.clear()

        val background = plot.createSeries().setLineWidth(30.0).setColour(Colour.GREY).setMarkerVisible(false)
        val fill       = plot.createSeries().setLineWidth(20.0).setColour(Colour.BLUE).setMarkerVisible(false)

        background.addPoint(0.0, 0.0).addPoint(time.toDouble() / 1000.0, 0.0)

        task = RTask((time / 100).toLong()) { t -> fill.addPoint(t.secFromStart, 0.0) }

        task?.start()

        sleep(time)

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Column<*>> {
        return emptyArray()
    }

    override fun getLabel(): String {
        return Util.msToString(time.toLong())
    }

}