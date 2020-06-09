package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class BwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Backward Linear Mobility"

}