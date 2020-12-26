package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.gui.Plot
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.quantities.Temperature
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

open class SingleParameterAnalysis(val parameterExample: Quantity) : Analysis {

    private fun tabulate(quantities: List<Quantity>): List<Analysis.Tabulated> {

        val name = parameterExample.name
        val unit = parameterExample.unit

        val tables      = LinkedList<Analysis.Tabulated>()
        val quantitySet = quantities.map { it::class }.toSet()

        for (quantityClass in quantitySet) {

            val filtered = quantities.filter { it::class == quantityClass }
            val instance = filtered.first()

            val table = ResultList(Col(name, unit), Col(instance.name, instance.unit), Col("${instance.name} Error", instance.unit))

            for (value in filtered) {

                val temperature = value.parameters.find { it::class == parameterExample::class } ?: continue

                table.addData(temperature.value, value.value, value.error)

            }

            tables += Analysis.Tabulated(listOf(Temperature(0.0, 0.0)), instance, table)

        }

        return tables.sortedBy { it.quantity.name }

    }

    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Analysis.Output {

        val processed = tabulate(quantities)
        val plots     = ArrayList<Plot>()

        for (table in processed) {

            val name = table.quantity.name
            val plot = FetChPlot("$name vs ${parameterExample.name}")

            plot.createSeries()
                .watch(table.table, 0, 1, 2)
                .setColour(Series.defaultColours[plots.size % Series.defaultColours.size])
                .setMarkerVisible(table.table.numRows <= 20)
                .setLineVisible(table.table.numRows > 20)

            plot.isLegendVisible = false
            plot.isMouseEnabled  = true
            plot.autoLimits()

            plots += plot

        }

        return Analysis.Output(processed, plots)

    }

}