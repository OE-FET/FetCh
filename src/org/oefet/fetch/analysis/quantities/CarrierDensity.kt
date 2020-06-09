package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class CarrierDensity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Carrier Density"
    override val symbol = "n"
    override val unit   = "cm^-3"
    override val extra  = false

}