package org.oefet.fetch.sweep

import javafx.scene.paint.Color
import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.TC
import jisa.experiment.ActionQueue
import jisa.experiment.Col
import jisa.experiment.ResultTable
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
    val emControl by optionalConfig("Electromagnet Controller", EMController::class)

    override fun generateActions(): List<ActionQueue.Action> {

        val list = LinkedList<ActionQueue.Action>()

        for (field in fields) {

            list += ActionQueue.MeasureAction("$field T", SweepPoint(field, emControl), {}, {})
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

    class SweepPoint(val field: Double, val fControl: EMController?) : Action("Change Field") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            return FetChPlot("Change Temperature to $field K", "Time [s]", "Temperature [K]").apply {
                createSeries().watch(data, 0, 1).setMarkerVisible(false).setColour(Color.PURPLE)
                isLegendVisible = false
            }

        }

        override fun run(results: ResultTable) {

            if (fControl == null) {
                throw Exception("EM Controller is not configured.")
            }

            task = RTask(2500) { t -> results.addData(t.secFromStart, fControl.field) }
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