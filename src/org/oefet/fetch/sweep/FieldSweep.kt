package org.oefet.fetch.sweep

import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import org.oefet.fetch.action.FAction
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.tabs.Measure
import java.util.*

class FieldSweep : Sweep<Double>("Field Sweep") {

    val fields    by input("Field", "Set-Points [T]", Range.step(-1, +1, 0.5))
    val interval  by input("Field", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val emControl by optionalConfig("Electromagnet Controller", EMController::class)

    class SweepPoint(val field: Double, val interval: Long, val fControl: EMController?) : FAction("Change Field") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            val plot = FetChPlot("Change Field to $field T", "Time [s]", "Field [T]")

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
                Col("Time", "s"),
                Col("Field", "T")
            )

        }

        override fun getLabel(): String {
            return "$field T"
        }

    }

    override fun getValues(): List<Double> {
        return fields.array().toList();
    }

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<out Any>>()

        list += MeasurementAction(SweepPoint(value, interval, emControl)).apply { setOnMeasurementStart { Measure.display(it) }  }
        list += actions

        list.forEach {
            it.setAttribute("B", "$value T")
            it.addTag("B = $value T")
        }

        return list

    }

    override fun formatValue(value: Double): String = "$value T"

}