package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.electromagnet.EMController
import jisa.enums.Icon
import jisa.gui.Colour
import jisa.gui.Series
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class FieldChange : FetChAction("Change Field", Icon.MAGNET.blackImage) {

    var task: RTask? = null

    val field    by userInput("Field", "Set-Point [T]", 1.0)
    val interval by userTimeInput("Field", "Logging Interval", 500)
    val fControl by requiredInstrument("EM Controller", EMController::class)

    companion object : Columns() {

        val TIME  = decimalColumn("Time","s")
        val FIELD = decimalColumn("Field", "T")

    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        val plot =  FetChPlot("Change Field to $field T", "Time [s]", "Field [T]")

        plot.createSeries()
            .watch(data, { it[TIME] }, { field })
            .setMarkerVisible(false)
            .setLineWidth(1.0)
            .setLineDash(Series.Dash.DASHED)
            .setColour(Colour.GREY)

        plot.createSeries()
            .watch(data, TIME, FIELD)
            .setMarkerVisible(false)
            .setColour(Colour.PURPLE)

        plot.isLegendVisible = false

        return plot

    }

    override fun run(results: ResultTable) {

        task = RTask(interval.toLong()) { t -> results.addData(t.secFromStart, fControl.field) }
        task?.start()

        fControl.field = field

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getLabel(): String {
        return "$field T"
    }

}