package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class SeebeckCoefficient(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Seebeck Coefficient"
    override val unit   = "V/K"
    override val symbol = "S"
    override val extra  = false

}