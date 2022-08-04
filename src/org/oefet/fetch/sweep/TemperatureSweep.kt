package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.TC
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.action.TemperatureChange.Companion.TEMPERATURE
import org.oefet.fetch.action.TemperatureChange.Companion.TIME
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*

class TemperatureSweep : FetChSweep<Double>("Temperature Sweep", "T", Icon.THERMOMETER.blackImage) {

    val temperatures  by userInput("Temperature", "Set-Points [K]", Range.step(50, 300, 50))
    val interval      by userInput("Temperature", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val stabilityPct  by userInput("Temperature", "Stays within [%]", 1.0)
    val stabilityTime by userTimeInput("Temperature", "For at least", 600000)
    val tControl      by requiredInstrument("Temperature Controller", TC.Loop::class)


    override fun getValues(): List<Double> = temperatures.array().toList()

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<*>>()

        list += MeasurementAction(SweepPoint(value, interval, stabilityPct, stabilityTime.toLong(), tControl))
        list += actions

        return list

    }

    override fun formatValue(value: Double): String = "$value K"

    class SweepPoint(
        val temperature: Double,
        val interval: Long,
        val stabilityPct: Double,
        val stabilityTime: Long,
        val loop: TC.Loop?
    ) : FetChAction("Change Temperature", Icon.THERMOMETER.blackImage) {

        private var task:   RTask?  = null
        private var series: Series? = null

        override fun createDisplay(data: ResultTable): FetChPlot {

            val plot = FetChPlot("Change Temperature to $temperature K", "Time", "Temperature [K]")

            plot.isLegendVisible = false
            plot.xAxisType = Plot.AxisType.TIME

            plot.createSeries()
                .watch(data, { it[TIME] / 1000.0 }, { (1 + (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, { it[TIME] / 1000.0 }, { (1 - (stabilityPct / 100.0)) * temperature })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            series = plot.createSeries()
                .watch(data, { it[TIME] / 1000.0 }, { it[TEMPERATURE] })
                .setMarkerVisible(false)
                .setColour(Colour.RED)

            return plot

        }

        override fun run(results: ResultTable) {

            if (loop == null) {
                throw Exception("Temperature Controller is not configured")
            }

            val min = (1 - (stabilityPct / 100.0)) * temperature
            val max = (1 + (stabilityPct / 100.0)) * temperature

            task = RTask(interval) { _ ->

                val t = loop.temperature

                results.addData(System.currentTimeMillis(), t)

                series?.colour = if (Util.isBetween(t, min, max)) { Colour.TEAL } else { Colour.RED }

            }

            task?.start()

            loop.setPoint     = temperature
            loop.isPIDEnabled = true

            loop.waitForStableTemperature(temperature, stabilityPct, stabilityTime)

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