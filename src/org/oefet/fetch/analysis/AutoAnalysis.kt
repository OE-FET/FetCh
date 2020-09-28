package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.Combination
import jisa.experiment.ResultList
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series
import org.oefet.fetch.analysis.Analysis.Tabulated
import org.oefet.fetch.analysis.quantities.Quantity
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KClass

object AutoAnalysis : Analysis {

    val colours = Series.defaultColours

    private fun tabulate(quantities: List<Quantity>): List<Tabulated> {

        val tables      = LinkedList<Tabulated>()
        val quantitySet = quantities.map { it::class }.toSet().sortedBy { it.simpleName }

        for (quantityClass in quantitySet) {

            // Determine all the types of parameters varied for this type of quantity
            val filtered     = quantities.filter { it::class == quantityClass }
            val instance     = filtered.first()
            val parameters   = filtered.flatMap { it.parameters }
            val parameterSet = parameters.map { it::class }.toSet()
            val pColumns     = parameterSet.map { c -> parameters.first { p -> p::class == c } }
            val columns      = pColumns.map { Col(it.name, it.unit) }.toMutableList()

            // Define the final two columns as being for the quantity values and their error values
            columns += Col(instance.name, instance.unit)
            columns += Col("${instance.name} Error", instance.unit)

            val table = ResultList(*columns.toTypedArray())

            for (value in filtered) {

                // Populate the first columns in the table with parameters, using NaN if N/A to this row
                val row = parameterSet
                    .map { c -> value.parameters.find { p -> p::class == c } }
                    .map { it?.value ?: Double.NaN }
                    .toMutableList()

                // Add the quantity and its error into the final two columns
                row += value.value
                row += value.error

                // Add row to the table
                table.addData(*row.toDoubleArray())

            }

            tables += Tabulated(pColumns, instance, table)

        }

        return tables.sortedBy { it.quantity.name }

    }

    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Analysis.Output {

        val processed = tabulate(quantities)
        val plots     = LinkedList<Plot>()

        for (table in processed) {

            // The columns indices for the value and its error (final two columns)
            val valueIndex = table.parameters.size
            val errorIndex = valueIndex + 1

            // Loop over all the parameter columns in the table
            for ((paramIndex, parameter) in table.parameters.withIndex()) {

                // If the quantity isn't varied or is not meant to be displayed as a number, then skip it
                if (labels.containsKey(parameter::class)) continue
                if (table.table.getUniqueValues(paramIndex).size < 2) continue

                val splits     = LinkedList<Int>()
                val names      = LinkedHashMap<Int, Quantity>()
                var splitCount = 1

                // Loop over all other varied parameters in the table
                for ((splitIndex, splitParam) in table.parameters.withIndex()) {

                    if (splitIndex != paramIndex && table.table.getUniqueValues(splitIndex).size > 1) {
                        splits            += splitIndex
                        names[splitIndex]  = splitParam
                        splitCount        *= table.table.getUniqueValues(splitIndex).size
                    }

                }

                // Don't plot if more values in legend than x-axis
                if ((table.table.getUniqueValues(paramIndex).size) < splitCount) continue

                // Create the plot and the data series
                val line   = table.table.getUniqueValues(paramIndex).size > 20
                val plot   = FetChPlot("${table.quantity.name} vs ${parameter.name}")
                val series = plot.createSeries()
                    .watch(table.table, paramIndex, valueIndex, errorIndex)
                    .setColour(colours[plots.size % colours.size])
                    .setMarkerVisible(!line)
                    .setLineVisible(line)

                if (splits.isNotEmpty()) {

                    series.split(

                        // Split by the unique combination of all varied parameters not on the x-axis
                        { row -> Combination(*splits.map { if (row[it].isFinite()) row[it] else Double.NEGATIVE_INFINITY }.toTypedArray()) },

                        // Label each legend item with any pre-defined labels, or default to x = n.nnn U
                        { row -> names.entries.joinToString(" \t ") { labels[it.value::class]?.get(row[it.key]) ?: "%s = %.4g %s".format(it.value.symbol, row[it.key], it.value.unit) } }

                    )

                    plot.isLegendVisible = true

                } else {

                    plot.isLegendVisible = false

                }

                // Make sure the plot is user-interactive via the mouse
                plot.isMouseEnabled = true
                plot.autoLimits()

                plots += plot

            }

        }

        return Analysis.Output(processed, plots)

    }

}