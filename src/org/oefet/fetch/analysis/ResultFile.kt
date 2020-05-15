package org.oefet.fetch.analysis

import jisa.experiment.ResultTable
import jisa.gui.Plot
import java.util.*
import kotlin.reflect.KClass

interface ResultFile {

    val data: ResultTable
    val parameters: MutableList<Quantity>
    val quantities: MutableList<Quantity>
    val plot: Plot
    val name: String

    fun calculateHybrids(quantities: List<Quantity>) : List<Quantity>

    fun getParameterString(): String {

        val parts = LinkedList<String>()

        for (parameter in parameters) {

            if (parameter.extra) parts += "%s = %s %s".format(parameter.name, parameter.value, parameter.unit)

        }

        return parts.joinToString(", ")

    }

}