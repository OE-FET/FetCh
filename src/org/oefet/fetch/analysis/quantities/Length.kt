package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class Length(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Channel Length"
    override val symbol = "lc"
    override val unit   = "m"
    override val extra  = false

}