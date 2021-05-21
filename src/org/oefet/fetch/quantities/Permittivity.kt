package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Permittivity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Dielectric Permittivity"
    override val symbol = "ε"
    override val unit   = ""
    override val important  = false

}