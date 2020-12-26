package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.Combination
import jisa.experiment.ResultList
import jisa.gui.Plot
import org.oefet.fetch.quantities.Device
import org.oefet.fetch.quantities.Gate
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.quantities.Temperature
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.reflect.KClass

object HallAnalysis : Analysis {
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
                    .setColour(AutoAnalysis.colours[plots.size % AutoAnalysis.colours.size])
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

    private fun tabulate(quantities: List<Quantity>): List<Analysis.Tabulated> {

        val tables      = LinkedList<Analysis.Tabulated>()
        val quantitySet = quantities.map { it::class }.toSet()

        for (quantityClass in quantitySet) {

            val filtered     = quantities.filter { it::class == quantityClass }
            val instance     = filtered.first()

            val table = ResultList(Col("Temperature", "K"), Col("Gate", "V"), Col("Device"), Col(instance.name, instance.unit), Col("${instance.name} Error", instance.unit))

            for (value in filtered) {

                val temperature = value.parameters.find { it is Temperature } ?: continue
                val gate        = value.parameters.find { it is Gate }
                val device      = value.parameters.find { it is Device }

                table.addData(temperature.value, gate?.value ?: 0.0, device?.value ?: 0.0, value.value, value.error)

            }

            tables += Analysis.Tabulated(listOf(Temperature(0.0, 0.0), Gate(0.0, 0.0), Device(0.0)), instance, table)

        }

        return tables.sortedBy { it.quantity.name }

    }

}