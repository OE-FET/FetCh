package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class Drain(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Source-Drain Voltage"
    override val symbol = "Vsd"
    override val unit   = "V"
    override val extra  = true

}