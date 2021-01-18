package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class BandLikeDensity(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Band-Like Carrier Density"
    override val unit   = "cm^-3"
    override val symbol = "n0"
    override val extra  = false

}