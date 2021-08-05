package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class FourPointProbe(
    override val value: Boolean,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : Quantity<Boolean> {
    override val error     = false
    override val name      = "Four Point Probe"
    override val symbol    = "FPP"
    override val unit      = ""
    override val important = false
}