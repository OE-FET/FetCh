package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.gui.Plot
import jisa.gui.Series
import org.oefet.fetch.analysis.Analysis
import org.oefet.fetch.analysis.Analysis.*
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

object TemperatureAnalysis : Analysis {

    private fun tabulate(quantities: List<Quantity>): List<Tabulated> {

        val tables = LinkedList<Tabulated>()

        val quantitySet = quantities.stream().map { it::class }.collect(Collectors.toSet())

        for (quantityClass in quantitySet) {

            val filtered     = quantities.filter { it::class == quantityClass }
            val instance     = filtered.first()

            val table = ResultList(Col("Temperature", "K"), Col(instance.name, instance.unit), Col("${instance.name} Error", instance.unit))

            for (value in filtered) {

                val temperature = value.parameters.find { it is Temperature } ?: continue

                table.addData(temperature.value.toDouble(), value.value.toDouble(), value.error)

            }

            tables += Tabulated(listOf(Temperature(0.0, 0.0)), instance, table)

        }

        return tables

    }

    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Output {

        val processed = tabulate(quantities)
        val plots     = ArrayList<Plot>()

        for (table in processed) {

            val name = table.quantity.name
            val plot = Plot("$name vs Temperature")

            plot.createSeries()
                .watch(table.table, 0, 1, 2)
                .setColour(Series.defaultColours[plots.size % Series.defaultColours.size])
                .showLine(false)

            plot.showLegend(false)
            plot.useMouseCommands(true)
            plot.useAutoLimits()

            plots += plot

        }

        return Output(processed, plots)

    }

}