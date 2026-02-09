package org.oefet.fetch.analysis

import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.quant.Result

interface AnalysisOld {

    fun analyse(quantities: List<Result>) : Output

    class Output(val tables: List<Tabulated>, val plots: List<Element>)

    class Tabulated(
        val parameters: List<org.oefet.fetch.quant.Quantity<*>>,
        val quantity: org.oefet.fetch.quant.Quantity<*>,
        val table: ResultTable
    )

}