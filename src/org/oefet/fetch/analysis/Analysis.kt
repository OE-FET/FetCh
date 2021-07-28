package org.oefet.fetch.analysis

import jisa.gui.Plot
import jisa.results.ResultTable
import org.oefet.fetch.quantities.Quantity

interface Analysis {

    fun analyse(quantities: List<Quantity<*>>) : Output

    class Output(val tables: List<Tabulated>, val plots: List<Plot>)

    class Tabulated(
        val parameters: List<Quantity<*>>,
        val quantity: Quantity<*>,
        val table: ResultTable
    )

}