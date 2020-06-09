package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class Repeat(
    override val value: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val error  = 0.0
    override val name   = "Repeat No."
    override val symbol = "N"
    override val unit   = ""
    override val extra  = true

}