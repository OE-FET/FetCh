package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class FwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Forward Linear Mobility"

}