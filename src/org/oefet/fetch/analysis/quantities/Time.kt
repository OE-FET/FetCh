package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class Time(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Time"
    override val symbol = "t"
    override val unit   = "s"
    override val extra  = true

}