package org.oefet.fetch.analysis

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Plot
import org.oefet.fetch.quantities.Quantity
import kotlin.reflect.KClass

interface Analysis {

    fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>> = emptyMap()) : Output

    class Output(val tables: List<Tabulated>, val plots: List<Plot>)

    class Tabulated(
        val parameters: List<Quantity>,
        val quantity: Quantity,
        val table: ResultTable
    )

}