package org.oefet.fetch.results

import javafx.scene.image.Image
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.*
import java.util.*
import java.util.List.copyOf
import kotlin.reflect.KClass

abstract class FetChResult(name: String, val tag: String, val image: Image, val data: ResultTable, extraParams: List<Quantity>) {

    val name = name
        get() = "$field (${data.getAttribute("Name")})"

    val quantities = LinkedList<Quantity>()
    val parameters = LinkedList<Quantity>()

    var length       = data.getAttribute("Length").removeSuffix("m").toDouble()
    var separation   = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
    var width        = data.getAttribute("Width").removeSuffix("m").toDouble()
    var thickness    = data.getAttribute("Thickness").removeSuffix("m").toDouble()
    var dielectric   = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
    var permittivity = data.getAttribute("Dielectric Permittivity").toDouble()
    var temperature  = determineTemperature()
    var repeat       = data.getAttribute("N")?.toDouble() ?: 0.0
    var stress       = data.getAttribute("S")?.removeSuffix("s")?.toDouble() ?: 0.0
    var field        = data.getAttribute("B")?.removeSuffix("T")?.toDouble() ?: 0.0

    init {

        if (data.getAttribute("Type") != tag) {
            throw Exception("Supplied data is not $tag measurement data")
        }

        parameters += extraParams
        parameters += Length(length, 0.0)
        parameters += FPPSeparation(separation, 0.0)
        parameters += Width(width, 0.0)
        parameters += Thickness(thickness, 0.0)
        parameters += DThickness(dielectric, 0.0)
        parameters += Permittivity(permittivity, 0.0)
        parameters += Repeat(repeat)
        parameters += Time(stress, 0.0)
        parameters += BField(field, 0.0)

        if (temperature.isFinite()) {
            parameters += Temperature(temperature, 0.0)
        }

        val intTime = data.getAttribute("Integration Time")?.removeSuffix("s")?.toDouble() ?: Double.NaN
        val delTime = data.getAttribute("Delay Time")?.removeSuffix("ms")?.toDouble() ?: Double.NaN
        val hhTime  = data.getAttribute("Heater Hold Time")?.removeSuffix("ms")?.toDouble() ?: Double.NaN
        val ghTime  = data.getAttribute("Gate Hold Time")?.removeSuffix("ms")?.toDouble() ?: Double.NaN
        val aCount  = data.getAttribute("Averaging Count")?.toDouble() ?: Double.NaN

        if (intTime.isFinite()) parameters += IntegrationTime(intTime)
        if (delTime.isFinite()) parameters += DelayTime(delTime)
        if (hhTime.isFinite())  parameters += HeaterHoldTime(hhTime)
        if (ghTime.isFinite())  parameters += GateHoldTime(ghTime)
        if (aCount.isFinite())  parameters += AveragingCount(aCount)

    }

    abstract fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity>

    open fun getPlot(): FetChPlot? {
        return null
    }

    private fun determineTemperature(): Double {

        val attributes = data.attributes
        val column     = data.findColumn("Temperature")

        return when {

            attributes.containsKey("T")           -> data.getAttribute("T").removeSuffix("K").toDouble()
            attributes.containsKey("Temperature") -> data.getAttribute("Temperature").removeSuffix("K").toDouble()
            column > -1                           -> data.getMean(column)
            else                                  -> Double.NaN

        }

    }

    fun replaceParameter(quantity: Quantity) {
        parameters.removeIf { it::class == quantity::class }
        parameters += quantity
    }

    fun addParameter(parameter: Quantity) {
        parameters += parameter
    }

    fun addParameters(vararg parameters: Quantity) {
        this.parameters += parameters
    }

    fun replaceQuantity(quantity: Quantity) {
        quantities.removeIf { it::class == quantity::class }
        quantities += quantity
    }

    fun addQuantity(quantity: Quantity) {
        quantities += quantity
    }

    fun addQuantities(vararg quantities: Quantity) {
        this.quantities += quantities
    }

    fun getParameters(): List<Quantity> {
        return copyOf(parameters)
    }

    fun getQuantities(): List<Quantity> {
        return copyOf(quantities)
    }

    fun findQuantity(type: KClass<out Quantity>): Quantity? {
        return quantities.find { it::class == type }
    }

    fun findParameter(type: KClass<out Quantity>): Quantity? {
        return parameters.find { it::class == type }
    }

    fun getParameterString(): String {
        return parameters.filter { it.important }.joinToString(", ") { "${it.symbol} = ${it.value} ${it.unit}" }
    }

}