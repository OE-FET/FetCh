package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.interfaces.EMController

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Colour
import jisa.gui.Series
import jisa.results.Column
import org.oefet.fetch.gui.elements.FetChPlot

class FieldChange : FetChAction("Change Field") {

    var task: RTask? = null

    val field    by input("Field", "Set-Point [T]", 1.0)
    val interval by input("Field", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val fControl by requiredConfig("EM Controller", EMController::class)

    companion object {
        val TIME  = DoubleColumn("Time","s")
        val FIELD = DoubleColumn("Field", "T")
    }

    override fun createPlot(data: ResultTable): FetChPlot {

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

        task = RTask(interval) { t -> results.addData(t.secFromStart, fControl.field) }
        task?.start()

        fControl.field = field

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Column<*>> {
        return arrayOf(TIME, FIELD)
    }

    override fun getLabel(): String {
        return "$field T"
    }

}