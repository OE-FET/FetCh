package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class RMSField(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "RMS Field Strength"
    override val symbol = "B"
    override val unit   = "T"
    override val important  = false

}