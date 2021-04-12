package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Thickness(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Channel Thickness"
    override val symbol = "tc"
    override val unit   = "m"
    override val important  = false

}