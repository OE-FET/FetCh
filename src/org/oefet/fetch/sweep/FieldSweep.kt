package org.oefet.fetch.sweep

import javafx.scene.paint.Color
import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.TC
import jisa.experiment.ActionQueue
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import org.oefet.fetch.action.Action
import org.oefet.fetch.action.FieldChange
import org.oefet.fetch.action.TemperatureChange
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.FControl
import java.lang.Exception
import java.util.*

class FieldSweep : Sweep("Field Sweep") {

    val fields    by input ("Field", "Set-Points [T]", Range.step(-1, +1, 0.5))
    val interval  by input("Field", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val emControl by optionalConfig("Electromagnet Controller", EMController::class)

    override fun generateActions(): List<ActionQueue.Action> {

        val list = LinkedList<ActionQueue.Action>()

        for (field in fields) {

            list += ActionQueue.MeasureAction("$field T", SweepPoint(field, interval, emControl), {}, {})
            list += queue.getAlteredCopy { it.setAttribute("B", "$field T") }

        }

        return list

    }

    override fun run(results: ResultTable?) {

    }

    override fun onFinish() {

    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

    class SweepPoint(val field: Double, val interval: Long, val fControl: EMController?) : Action("Change Field") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            val plot =  FetChPlot("Change Field to $field T", "Time [s]", "Field [T]")

            plot.createSeries()
                .watch(data, { it[0] }, { field })
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

            if (fControl == null) {
                throw Exception("EM Controller is not configured.")
            }

            task = RTask(interval) { t -> results.addData(t.secFromStart, fControl.field) }
            task?.start()

            fControl.field = field

        }

        override fun onFinish() {
            task?.stop()
        }

        override fun getColumns(): Array<Col> {

            return arrayOf(
                Col("Time","s"),
                Col("Field", "T")
            )

        }

        override fun getLabel(): String {
            return "$field T"
        }

    }

}