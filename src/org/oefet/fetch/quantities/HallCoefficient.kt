package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class HallCoefficient(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Hall Coefficient"
    override val unit   = "m^3/C"
    override val symbol = "Rh"
    override val extra  = false

}