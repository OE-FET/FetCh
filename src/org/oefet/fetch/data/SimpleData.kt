package org.oefet.fetch.data

import jisa.gui.Element
import jisa.results.Column
import jisa.results.ResultTable
import jisa.results.RowEvaluable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quant.Result

class SimpleData(name: String, tag: String, data: ResultTable, private val toPlot: List<Pair<RowEvaluable<out Number>, RowEvaluable<out Number>>> = emptyList()) : FetChData(name, tag, data) {

    override fun processData(data: ResultTable): List<Result> {
        return emptyList()
    }

    override fun getDisplay(): Element {

        if (toPlot.isNotEmpty()) {

            return FetChPlot(name).apply {

                for ((x, y) in toPlot) {
                    createSeries().watch(data, x, y)
                }

            }

        } else {

            return FetChPlot(name).apply {

                val x = data.firstNumericColumn

                for (col in data.numericColumns) {

                    if (col == x) {
                        continue
                    }

                    createSeries().watch(data, x, col)

                }

            }

        }

    }

    override fun generateHybrids(results: List<Result>): List<Result> {
        return emptyList()
    }

}