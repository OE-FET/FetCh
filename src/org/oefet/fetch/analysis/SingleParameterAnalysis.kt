package org.oefet.fetch.analysis


import jisa.gui.Plot
import jisa.gui.Series
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultList
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.quantities.Temperature
import java.util.*
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

            val table = ResultList(DoubleColumn(name, unit), DoubleColumn(instance.name, instance.unit), DoubleColumn("${instance.name} Error", instance.unit))

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

            val x = table.table.getColumn(0) as Column<Double>
            val y = table.table.getColumn(1) as Column<Double>
            val e = table.table.getColumn(2) as Column<Double>

            plot.createSeries()
                .watch(table.table, x, y, e)
                .setColour(Series.defaultColours[plots.size % Series.defaultColours.size])
                .setMarkerVisible(table.table.rowCount <= 20)
                .setLineVisible(table.table.rowCount > 20)

            plot.isLegendVisible = false
            plot.isMouseEnabled  = true
            plot.autoLimits()

            plots += plot

        }

        return Analysis.Output(processed, plots)

    }

}