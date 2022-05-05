package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class UnscreenedHall(
    override val value: Double, override val error: Double, override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) :
    DoubleQuantity {

    override val name   = "Unscreened Hall Coefficient"
    override val unit   = "m^3/C"
    override val symbol = "RH0"
    override val important  = false

}