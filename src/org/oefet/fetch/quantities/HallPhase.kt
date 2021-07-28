package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class HallPhase(
    override val value: Double, override val error: Double, override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) :
    DoubleQuantity {

    override val name   = "Hall Phase Offset"
    override val unit   = "rad"
    override val symbol = "ϕ"
    override val important  = false

}