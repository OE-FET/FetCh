package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.Combination
import jisa.experiment.ResultList
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series
import org.oefet.fetch.analysis.Analysis.Tabulated
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.tabs.FileLoad
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KClass
import kotlin.streams.toList

object AutoAnalysis : Analysis {

    private fun tabulate(quantities: List<Quantity>): List<Tabulated> {

        val tables = LinkedList<Tabulated>()

        val quantitySet = quantities.stream().map { it::class }.collect(Collectors.toSet())

        for (quantityClass in quantitySet) {

            val filtered = quantities.filter { it::class == quantityClass }
            val instance = filtered.first()
            val parameters = filtered.stream().flatMap { it.parameters.stream() }.toList()
            val parameterSet = parameters.stream().map { it::class }.collect(Collectors.toSet())
            val pColumns =
                parameterSet.stream().map { c -> parameters.first { p -> p::class == c } }.collect(Collectors.toList())
            val columns = pColumns.stream().map { Col(it.name, it.unit) }.collect(Collectors.toList())

            columns += Col(instance.name, instance.unit)
            columns += Col("${instance.name} Error", instance.unit)

            val table = ResultList(*columns.toTypedArray())

            for (value in filtered) {

                val row = parameterSet.stream().map { c ->

                    try {
                        value.parameters.first { p -> p::class == c }
                    } catch (e: Exception) {
                        null
                    }

                }.map { it?.value?.toDouble() ?: Double.NaN }.collect(Collectors.toList())

                row += value.value.toDouble()
                row += value.error

                table.addData(*row.toDoubleArray())

            }

            tables += Tabulated(pColumns, instance, table)

        }

        return tables

    }

    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Analysis.Output {

        val processed = tabulate(quantities)
        val plots = LinkedList<Plot>()

        for (table in processed) {

            val valueIndex = table.parameters.size
            val errorIndex = valueIndex + 1

            for ((paramIndex, parameter) in table.parameters.withIndex()) {

                if (labels.containsKey(parameter::class))             continue
                if (table.table.getUniqueValues(paramIndex).size < 2) continue

                val splits = LinkedList<Int>()
                val names = LinkedHashMap<Int, Quantity>()
                var splitCount = 1

                for ((splitIndex, splitParam) in table.parameters.withIndex()) {

                    if (splitIndex != paramIndex && table.table.getUniqueValues(splitIndex).size > 1) {
                        splits += splitIndex
                        names[splitIndex] = splitParam
                        splitCount *= table.table.getUniqueValues(splitIndex).size
                    }

                }

                if ((table.table.getUniqueValues(paramIndex).size) < splitCount) continue

                val plot = FetChPlot("${table.quantity.name} vs ${parameter.name}")

                val series = plot.createSeries()
                    .watch(table.table, paramIndex, valueIndex, errorIndex)
                    .setColour(Colour.CORNFLOWERBLUE)

                if (table.table.getUniqueValues(paramIndex).size > 10) {
                    series.showMarkers(false).showLine(true)
                } else {
                    series.showMarkers(true).showLine(false)
                }

                if (splits.isNotEmpty()) {

                    series.split(
                        { row -> Combination(*splits.stream().map { if (row[it].isFinite()) row[it] else Double.NEGATIVE_INFINITY }.toList().toTypedArray()) },
                        { row ->
                            names.entries.stream().map {
                                    labels[it.value::class]?.get(row[it.key]) ?: "%s = %.4g %s".format(it.value.symbol, row[it.key], it.value.unit)
                            }.toList().joinToString("\t")
                        }
                    )

                    plot.showLegend(true)

                } else {
                    plot.showLegend(false)
                }

                plot.useMouseCommands(true)
                plot.useAutoLimits()

                plots += plot

            }

        }

        return Analysis.Output(processed, plots)

    }

}