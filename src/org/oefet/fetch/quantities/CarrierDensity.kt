package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class CarrierDensity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Carrier Density"
    override val symbol = "n"
    override val unit   = "cm^-3"
    override val important  = false

}