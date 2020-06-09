package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class DThickness(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Dielectric Thickness"
    override val symbol = "td"
    override val unit   = "m"
    override val extra  = false

}