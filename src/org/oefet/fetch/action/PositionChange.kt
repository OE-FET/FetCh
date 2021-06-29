package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class PositionChange : FetChAction("Change Position") {

    var task: RTask? = null

    val interval      by input("Position", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val pControl      by requiredConfig("Position Controller", ProbeStation::class)
    val xposition    by input("Position", "x Position [TBD]", 1.0)
    val yposition    by input("Position", "y Position [TBD]", 1.0)

    override fun createPlot(data: ResultTable): FetChPlot {

        val plot =  FetChPlot("Change Position to ($yposition TBD, $xposition TBD)", "Time [s]", "Position")

        plot.createSeries()
            .watch(data, { it[0] }, { xposition })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, 0, 1)
            .setMarkerVisible(false)
            .setColour(Colour.PURPLE)


        plot.isLegendVisible = false

        return plot

    }

    override fun run(results: ResultTable) {

        task = RTask(interval) { t ->
            results.addData(t.secFromStart, pControl.xposition)
        }

        task?.start()


        pControl.ChuckGrossDown()
        pControl.ChuckFineDown()
        sleep(100)

        pControl.xposition = xposition
        pControl.yposition = yposition

        sleep(100)
        pControl.ChuckFineUp()
        pControl.ChuckGrossUp()





    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Time","s"),
            Col("Position", "TBD")
        )

    }

    override fun getLabel(): String {
        return "$xposition, $yposition"
    }

}