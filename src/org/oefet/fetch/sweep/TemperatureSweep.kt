package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*

class TemperatureSweep : FetChSweep<Double>("Temperature Sweep", "T") {

    val temperatures  by input("Temperature", "Set-Points [K]", Range.step(50, 300, 50))
    val interval      by input("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct  by input("Temperature", "Stays within [%]", 1.0)
    val stabilityTime by input("Temperature", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by optionalConfig("Temperature Controller", TC::class)


    override fun getValues(): List<Double> = temperatures.array().toList()

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<*>>()

        list += MeasurementAction(SweepPoint(value, interval, stabilityPct, stabilityTime, tControl))
        list += actions

        return list

    }

    override fun formatValue(value: Double): String = "$value K"

    class SweepPoint(val temperature: Double, val interval: Long, val stabilityPct: Double, val stabilityTime: Long, val tControl: TC?) : FetChAction("Change Temperature") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            val plot =  FetChPlot("Change Temperature to $temperature K", "Time [s]", "Temperature [K]")

            plot.createSeries()
                .watch(data, { it[0] }, { (1 + (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, { it[0] }, { (1 - (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, 0, 1)
                .setMarkerVisible(false)
                .setColour(Colour.RED)

            plot.createSeries()
                .watch(data, 0, 1)
                .filter { Util.isBetween(it[1], (1 - (stabilityPct / 100.0)) * temperature, (1 + (stabilityPct / 100.0)) * temperature) }
                .setMarkerVisible(false)
                .setLineWidth(3.0)
                .setColour(Colour.MEDIUMSEAGREEN)

            plot.isLegendVisible = false

            return plot

        }

        override fun run(results: ResultTable) {

            if (tControl == null) {
                throw Exception("Temperature Controller is not configured")
            }

            task = RTask(interval) { t -> results.addData(t.secFromStart, tControl.temperature) }
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