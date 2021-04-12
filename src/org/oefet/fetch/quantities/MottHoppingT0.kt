package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class MottHoppingT0(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "T0"
    override val unit   = "K"
    override val symbol = "T0"
    override val important  = false

}