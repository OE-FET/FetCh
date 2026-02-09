package org.oefet.fetch.analysis

import jisa.experiment.Combination
import jisa.gui.Element
import jisa.gui.HeatMap
import jisa.gui.Series
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultList
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.XYPoint
import org.oefet.fetch.quant.XYQuantity
import org.oefet.fetch.quant.XYZPoint

class SpecificAnalysis(vararg val types: String) : AnalysisOld {

    override fun analyse(quantities: List<Result>): AnalysisOld.Output {

        val grouped       = quantities.groupBy { it.name }
        val tabulated     = ArrayList<AnalysisOld.Tabulated>(grouped.size)
        val plots         = ArrayList<Element>(grouped.size)

        grouped.entries.parallelStream().forEach { (_, typeQuantities) ->

            val example        = typeQuantities.first()
            val flatMap        = typeQuantities.flatMap { it.parameters }
            val distinct       = flatMap.distinctBy { it.name }.filter { types.isEmpty() || it.name in types }
            val varied         = distinct.filter { p -> flatMap.filter { it.name == p.name }.distinctBy { it.value }.size > 1 }

            val columns = varied.flatMap {

                when (it.value) {

                    is String   -> listOf(Column.ofStrings(it.name, it.type.units))
                    is Int      -> listOf(Column.ofIntegers(it.name, it.type.units))
                    is Boolean  -> listOf(Column.ofBooleans(it.name, it.type.units))
                    is Number   -> listOf(Column.ofDoubles(it.name, it.type.units))
                    is XYPoint  -> listOf(Column.ofDoubles(it.name + " X", it.type.units), Column.ofDoubles(it.name + " Y", it.type.units))
                    is XYZPoint -> listOf(Column.ofDoubles(it.name + " X", it.type.units), Column.ofDoubles(it.name + " Y", it.type.units), Column.ofDoubles(it.name + " Z", it.type.units))
                    else        -> listOf(Column.ofStrings(it.name, it.type.units))

                } as List<Column<*>>

            }.toMutableList()

            val unit = example.type.units.ifBlank { null }

            val valueColumn = DoubleColumn(example.name, unit)
            val errorColumn = DoubleColumn("${example.name} Error", unit)

            columns += listOf(valueColumn)
            columns += listOf(errorColumn)

            val table = ResultList(columns)

            for (quantity in typeQuantities) {

                table.addRow { row ->

                    var index = 0
                    for (pType in varied) {

                        val param = quantity.findParameter(pType.name)

                        if (param is XYQuantity) {

                            val xColumn = columns[index]     as Column<Double>
                            val yColumn = columns[index + 1] as Column<Double>

                            row[xColumn] = param?.value?.x ?: Double.NaN
                            row[yColumn] = param?.value?.y ?: Double.NaN

                            index += 2

                        } else if (param?.value is XYZPoint) {

                            val xColumn = columns[index]     as Column<Double>
                            val yColumn = columns[index + 1] as Column<Double>
                            val zColumn = columns[index + 2] as Column<Double>

                            row[xColumn] = param?.value?.x ?: Double.NaN
                            row[yColumn] = param?.value?.y ?: Double.NaN
                            row[zColumn] = param?.value?.z ?: Double.NaN

                            index += 3

                        } else {

                            val column = columns[index] as Column<Any>

                            row[column] = when (param?.value) {

                                null                                     -> null
                                is String, is Int, is Boolean, is Number -> param.value
                                else                                     -> param.value.toString()

                            }

                            index++

                        }

                    }

                    row[valueColumn] = quantity.value
                    row[errorColumn] = quantity.error

                }

            }

            val processed = AnalysisOld.Tabulated(varied, example, table)
            val n         = tabulated.size

            tabulated += processed

            val params  = table.columns.subList(0, table.columns.lastIndex - 2)
            val value   = table.columns[table.columns.lastIndex - 1] as DoubleColumn
            val error   = table.columns.last() as DoubleColumn
            val xColumn = (params.filter { it is DoubleColumn }.maxByOrNull { table.getUniqueValues(it).size } ?: params.firstOrNull { it is DoubleColumn }) as DoubleColumn?

            if (xColumn != null) {

                val plot = FetChPlot("${value.name} vs ${xColumn.name}", xColumn.title, value.title)
                val splitBy = params.filter { it != xColumn }
                val symbols = processed.parameters.filterIndexed { i, _ -> params[i] != xColumn }.map { it.symbol }
                val series =
                    plot.createSeries().watch(table, xColumn, value, error).setName(value.name).setLineVisible(false)
                        .setColour(Series.defaultColours[n % Series.defaultColours.size])
                        .setPointOrder(Series.Ordering.X_AXIS)

                if (splitBy.isNotEmpty()) {

                    series.split(
                        { r -> Combination(*splitBy.map { r[it] }.toTypedArray()) },
                        { r ->
                            splitBy.mapIndexed { i, it -> "${symbols[i]} = ${r[it]} ${it.units}" }
                                .joinToString("\n") + "\n"
                        }
                    )

                    plot.isLegendVisible = true

                } else {
                    plot.isLegendVisible = false
                }

                if (table.getUniqueValues(xColumn).size >= 5 && table.max(error) == 0.0) {
                    series.setLineVisible(true).setMarkerVisible(false)
                }

                plots += plot

            }

            val xyPoints = processed.parameters.filter { it.value is XYPoint }

            for (parameter in xyPoints) {

                val plot    = HeatMap("${value.name} vs ${parameter.name}")
                val xColumn = table.findDoubleColumn(parameter.name + " X")
                val yColumn = table.findDoubleColumn(parameter.name + " Y")
                val x = table[xColumn]
                val y = table[yColumn]
                val v = table[value]

                plot.setColourMap(HeatMap.ColourMap.VIRIDIS)
                plot.drawMesh(x, y, v)

                plots += plot

            }

        }

        return AnalysisOld.Output(tabulated, plots)

    }

}