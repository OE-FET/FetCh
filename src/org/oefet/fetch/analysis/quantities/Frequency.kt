package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class Frequency(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Frequency"
    override val symbol = "f"
    override val unit   = "Hz"
    override val extra  = true

}