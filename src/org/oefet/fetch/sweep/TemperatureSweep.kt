package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.action.TemperatureChange.Companion.TEMPERATURE
import org.oefet.fetch.action.TemperatureChange.Companion.TIME
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*

class TemperatureSweep : FetChSweep<Double>("Temperature Sweep", "T") {

    val temperatures  by userInput("Temperature", "Set-Points [K]", Range.step(50, 300, 50))
    val interval      by userInput("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct  by userInput("Temperature", "Stays within [%]", 1.0)
    val stabilityTime by userInput("Temperature", "For at least [s]", 600.0) map { it.toMSec().toLong() }
    val tControl      by requiredInstrument("Temperature Controller", TC::class)


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
                .watch(data, { it[TIME] }, { (1 + (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, { it[TIME] }, { (1 - (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, TIME, TEMPERATURE)
                .setMarkerVisible(false)
                .setColour(Colour.RED)

            plot.createSeries()
                .watch(data, TIME, TEMPERATURE)
                .filter { Util.isBetween(it[TEMPERATURE], (1 - (stabilityPct / 100.0)) * temperature, (1 + (stabilityPct / 100.0)) * temperature) }
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

        override fun getColumns(): Array<Column<*>> {

            return arrayOf(
                TIME,
                TEMPERATURE
            )

        }

        override fun getLabel(): String {
            return "$temperature K"
        }

    }

}