package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class SeebeckPower(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Seebeck Power Coefficient"
    override val unit   = "V/W"
    override val symbol = "SP"
    override val extra  = false

}