package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Temperature(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Temperature"
    override val symbol = "T"
    override val unit   = "K"
    override val important  = true

}