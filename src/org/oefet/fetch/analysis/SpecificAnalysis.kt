package org.oefet.fetch.analysis

import jisa.experiment.Combination
import jisa.gui.Plot
import jisa.gui.Series
import jisa.results.*
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.DoubleQuantity
import org.oefet.fetch.quantities.Quantity
import java.util.*
import kotlin.reflect.KClass

class SpecificAnalysis(vararg val types: KClass<out Quantity<*>>) : Analysis {

    override fun analyse(quantities: List<Quantity<*>>): Analysis.Output {

        val distinctTypes = quantities.filter { it is DoubleQuantity }.map { it::class }.distinct()
        val tabulated     = LinkedList<Analysis.Tabulated>()

        for (type in distinctTypes) {

            val typeQuantities = quantities.filter { it::class == type }.map { it as DoubleQuantity }
            val example        = typeQuantities.first()
            val flatMap        = typeQuantities.flatMap { it.parameters }
            val distinct       = flatMap.distinctBy { it::class }.filter { types.isEmpty() || it::class in types }
            val varied         = distinct.filter { p -> flatMap.filter { it::class == p::class }.distinctBy { it.value }.size > 1 }

            val columns = varied.map {

                when (it.value) {

                    is String  -> StringColumn(it.name, it.unit)
                    is Int     -> IntColumn(it.name, it.unit)
                    is Boolean -> BooleanColumn(it.name, it.unit)
                    is Number  -> DoubleColumn(it.name, it.unit)
                    else       -> StringColumn(it.name, it.unit)

                } as Column<*>

            }.toMutableList()

            val unit = example.unit.ifBlank { null }

            val valueColumn = DoubleColumn(example.name, unit)
            val errorColumn = DoubleColumn("${example.name} Error", unit)

            columns += valueColumn
            columns += errorColumn

            val table = ResultList(columns)

            for (quantity in typeQuantities) {

                table.addRow { row ->

                    for ((index, pType) in varied.withIndex()) {

                        val column = columns[index] as Column<Any>
                        val param  = quantity.getParameter(pType::class)

                        row[column] = when (param?.value) {

                            null                                     -> null
                            is String, is Int, is Boolean, is Number -> param.value
                            else                                     -> param.value.toString()

                        }

                    }

                    row[valueColumn] = quantity.value
                    row[errorColumn] = quantity.error

                }

            }

            tabulated += Analysis.Tabulated(varied, example, table)

        }

        val plots = LinkedList<Plot>()

        for ((n, processed) in tabulated.withIndex()) {

            val table   = processed.table
            val params  = table.columns.subList(0, processed.parameters.size)
            val value   = table.columns[processed.parameters.size] as DoubleColumn
            val error   = table.columns.last() as DoubleColumn
            val xColumn = (params.filter { it is DoubleColumn }.maxByOrNull { table.getUniqueValues(it).size } ?: params.firstOrNull { it is DoubleColumn }) as DoubleColumn?

            if (xColumn != null) {

                val plot    = FetChPlot("${value.name} vs ${xColumn.name}", xColumn.title, value.title)
                val splitBy = params.filter { it != xColumn }
                val symbols = processed.parameters.filterIndexed { i, _ -> params[i] != xColumn }.map { it.symbol }
                val series  = plot.createSeries().watch(table, xColumn, value, error).setName(value.name).setLineVisible(false).setColour(Series.defaultColours[n % Series.defaultColours.size]).setPointOrder(Series.Ordering.X_AXIS)

                if (splitBy.isNotEmpty()) {

                    series.split(
                        { r -> Combination(*splitBy.map{ r[it] }.toTypedArray()) },
                        { r -> splitBy.mapIndexed { i, it -> "${symbols[i]} = ${r[it]} ${it.units}" }.joinToString("\n") + "\n" }
                    )

                    plot.isLegendVisible = true;

                } else {
                    plot.isLegendVisible = false;
                }

                if (table.getUniqueValues(xColumn).size >= 5 && table.max(error) == 0.0) {
                    series.setLineVisible(true).setMarkerVisible(false)
                }

                plots += plot

            }


        }

        return Analysis.Output(tabulated, plots)

    }

}