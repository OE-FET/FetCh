package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Device(
    override val value: String
) : Quantity<String> {

    override val error: String = ""
    override val parameters: List<Quantity<*>> = emptyList()
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()

    override val name   = "Device"
    override val symbol = "D"
    override val unit   = ""
    override val important  = true

}