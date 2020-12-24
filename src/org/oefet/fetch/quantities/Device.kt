package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Device(
    override val value: Double
) : Quantity {

    override val error: Double = 0.0
    override val parameters: List<Quantity> = emptyList()
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()

    override val name   = "Device"
    override val symbol = "Device"
    override val unit   = ""
    override val extra  = true

}