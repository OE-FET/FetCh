package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Gate(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Source-Gate Voltage"
    override val symbol = "Vsg"
    override val unit   = "V"
    override val important  = true

}