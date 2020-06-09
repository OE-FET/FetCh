package org.oefet.fetch.analysis

import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.analysis.quantities.Quantity
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