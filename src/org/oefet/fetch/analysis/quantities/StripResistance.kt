package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class LeftStripResistance(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Strip Resistance"
    override val unit   = "Ohm"
    override val symbol = "Rs"
    override val extra  = false

}

class RightStripResistance(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Strip Resistance"
    override val unit   = "Ohm"
    override val symbol = "Rs"
    override val extra  = false

}