package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Width(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Channel Width"
    override val symbol = "wc"
    override val unit   = "m"
    override val important  = false

}