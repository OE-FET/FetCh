package org.oefet.fetch.sweep

import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.experiment.ActionQueue
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.maths.Range
import org.oefet.fetch.action.Action
import org.oefet.fetch.action.TemperatureChange
import org.oefet.fetch.gui.elements.FetChPlot
import java.lang.Exception
import java.util.*

class TemperatureSweep : Sweep("Temperature Sweep") {

    val temperatures  by input ("Temperature", "Set-Points [K]", Range.step(50, 300, 50))
    val stabilityPct  by input("Temperature", "Stays within [%]", 1.0)
    val stabilityTime by input("Temperature", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by optionalConfig("Temperature Controller", TC::class)

    override fun generateActions(): List<ActionQueue.Action> {

        val list = LinkedList<ActionQueue.Action>()

        for (temperature in temperatures) {

            list += ActionQueue.MeasureAction("$temperature K", SweepPoint(temperature, stabilityPct, stabilityTime, tControl), {}, {})
            list += queue.getAlteredCopy { it.setAttribute("T", "$temperature K") }

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

    class SweepPoint(val temperature: Double, val stabilityPct: Double, val stabilityTime: Long, val tControl: TC?) : Action("Change Temperature") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            return FetChPlot("Change Temperature to $temperature K", "Time [s]", "Temperature [K]").apply {
                createSeries().watch(data, 0, 1).setMarkerVisible(false).setColour(Colour.BLUE)
                isLegendVisible = false
            }

        }

        override fun run(results: ResultTable) {

            if (tControl == null) {
                throw Exception("Temperature Controller is not configured")
            }

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

}