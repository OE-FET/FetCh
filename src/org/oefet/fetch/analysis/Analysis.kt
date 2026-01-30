package org.oefet.fetch.analysis

import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.quant.*
import java.util.*

class Analysis(val name: String, val results: List<Result>) {

    private val grouped = results.groupBy { it.name }

    fun generateTables(): List<ResultTable> {

        val tables = LinkedList<ResultTable>()

        for ((_, values) in grouped) {

            val example = values.first()
            val params  = values.flatMap { it.parameters }.groupBy { it.name }.filter { (_, values) -> values.map { it.value }.distinct().size > 1 }.map { it.value.first() }

            val columns = params.flatMap {

                when (it.value) {
                    is Number   -> listOf(Column.ofDoubles(it.name, it.type.units))
                    is XYPoint  -> listOf(Column.ofDoubles(it.name + " X", it.type.units), Column.ofDoubles(it.name + " Y", it.type.units))
                    is XYZPoint -> listOf(Column.ofDoubles(it.name + " X", it.type.units), Column.ofDoubles(it.name + " Y", it.type.units), Column.ofDoubles(it.name + " Z", it.type.units))
                    else        -> listOf(Column.ofStrings(it.name, it.type.units))
                }

            } + Column.ofDoubles(example.name, example.type.units) + Column.ofDoubles(example.name + " Error", example.type.units)

            val table = ResultList(columns)

            for (value in values) {

                val row = params.flatMap {

                    when (it.value) {
                        is Number   -> listOf(value.findDoubleParameter(it.name)?.value ?: Double.NaN)

                        is XYPoint  -> {

                            val xy = value.findParameter(it.name, XYQuantity::class)

                            if (xy != null) {
                                listOf(xy.value.x, xy.value.y)
                            } else {
                                listOf(Double.NaN, Double.NaN)
                            }

                        }

                        is XYZPoint  -> {

                            val xyz = value.findParameter(it.name, XYZQuantity::class)

                            if (xyz != null) {
                                listOf(xyz.value.x, xyz.value.y, xyz.value.z)
                            } else {
                                listOf(Double.NaN, Double.NaN, Double.NaN)
                            }

                        }

                        else -> listOf(value.findParameter(it.name)?.value?.toString() ?: "")
                    }

                } + value.value + value.error

                table.mapRow(columns.zip(table).associate { it.first to it.second })

            }

            tables += table

        }

        return tables

    }

}