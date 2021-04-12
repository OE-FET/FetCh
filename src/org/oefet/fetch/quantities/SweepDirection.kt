package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class SweepDirection(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Sweep Direction"
    override val symbol = "dir"
    override val unit   = ""
    override val important  = false

}