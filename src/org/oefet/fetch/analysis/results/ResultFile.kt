package org.oefet.fetch.analysis.results

import javafx.scene.image.Image
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.analysis.UnknownResultException
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.measurement.Conductivity
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface ResultFile {

    val data: ResultTable
    val parameters: MutableList<Quantity>
    val quantities: MutableList<Quantity>
    val plot: Plot
    val name: String
    val image: Image
    val label: String

    var length:       Double
    var separation:   Double
    var width:        Double
    var thickness:    Double
    var dielectric:   Double
    var permittivity: Double

    fun calculateHybrids(quantities: List<Quantity>) : List<Quantity>

    fun getParameterString(): String {

        val parts = LinkedList<String>()

        for (parameter in parameters) {

            if (parameter.extra) parts += "%s = %s %s".format(parameter.name, parameter.value, parameter.unit)

        }

        return parts.joinToString(", ")

    }

    fun parseParameters(data: ResultTable, extra: List<Quantity> = emptyList()) {

        if (data.getAttribute("Type") != label) {
            throw Exception("That is not a $label measurement file")
        }

        length        = data.getAttribute("Length").removeSuffix("m").toDouble()
        separation    = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
        width         = data.getAttribute("Width").removeSuffix("m").toDouble()
        thickness     = data.getAttribute("Thickness").removeSuffix("m").toDouble()
        dielectric    = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
        permittivity  = data.getAttribute("Dielectric Permittivity").toDouble()

        parameters += Length(length, 0.0)
        parameters += FPPSeparation(separation, 0.0)
        parameters += Width(width, 0.0)
        parameters += Thickness(thickness, 0.0)
        parameters += DThickness(dielectric, 0.0)
        parameters += Permittivity(permittivity, 0.0)

        for ((_, value) in data.attributes) {
            parameters += Quantity.parseValue(value) ?: continue
        }

    }

}