package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Conductivity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Conductivity"
    override val symbol = "σ"
    override val unit   = "S/cm"
    override val important  = false

}

class MConductivity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Magneto-Conductivity"
    override val symbol = "σ"
    override val unit   = "S/cm"
    override val important  = false

}