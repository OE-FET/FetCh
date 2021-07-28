package org.oefet.fetch.sweep

import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.action.FieldChange.Companion.FIELD
import org.oefet.fetch.action.FieldChange.Companion.TIME
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.tabs.Measure
import java.util.*

class FieldSweep : FetChSweep<Double>("Field Sweep", "B") {

    val fields    by input("Field", "Set-Points [T]", Range.step(-1, +1, 0.5))
    val interval  by input("Field", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val emControl by optionalConfig("Electromagnet Controller", EMController::class)

    override fun getValues(): List<Double> {
        return fields.array().toList();
    }

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<out Any>>()

        list += MeasurementAction(SweepPoint(value, interval, emControl)).apply { setOnMeasurementStart { Measure.display(it) }  }
        list += actions

        return list

    }

    override fun formatValue(value: Double): String = "$value T"

    class SweepPoint(val field: Double, val interval: Long, val fControl: EMController?) : FetChAction("Change Field") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            val plot = FetChPlot("Change Field to $field T", "Time [s]", "Field [T]")

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

        override fun getColumns(): Array<Column<*>> {

            return arrayOf(
                TIME,
                FIELD
            )

        }

        override fun getLabel(): String {
            return "$field T"
        }

    }

}