package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class StripPowerCoefficient(
    override val value: Double, override val error: Double, override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) :
    DoubleQuantity {

    override val name   = "Strip Power Coefficient"
    override val unit   = "Ohm/W"
    override val symbol = "SPC"
    override val important  = false

}