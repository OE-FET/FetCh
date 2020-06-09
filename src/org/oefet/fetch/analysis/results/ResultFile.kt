package org.oefet.fetch.analysis.results

import javafx.scene.image.Image
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.analysis.quantities.Quantity
import org.oefet.fetch.analysis.UnknownResultException
import java.util.*

interface ResultFile {

    val data: ResultTable
    val parameters: MutableList<Quantity>
    val quantities: MutableList<Quantity>
    val plot: Plot
    val name: String
    val image: Image

    fun calculateHybrids(quantities: List<Quantity>) : List<Quantity>

    fun getParameterString(): String {

        val parts = LinkedList<String>()

        for (parameter in parameters) {

            if (parameter.extra) parts += "%s = %s %s".format(parameter.name, parameter.value, parameter.unit)

        }

        return parts.joinToString(", ")

    }

    companion object {

        fun loadData(data: ResultTable, extra: List<Quantity> = emptyList()) : ResultFile {

            return when (data.getAttribute("Type")) {

                "Transfer"         -> TransferResult(data, extra)
                "Output"           -> OutputResult(data, extra)
                "FPP Conductivity" -> CondResult(data, extra)
                else               -> null

            } ?: throw UnknownResultException("Unknown measurement type")

        }

    }

}