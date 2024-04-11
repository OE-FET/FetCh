package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Gate(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Source-Gate Voltage"
    override val symbol = "Vsg"
    override val unit   = "V"
    override val important  = true

}

class Voltage(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Voltage"
    override val symbol = "V"
    override val unit   = "V"
    override val important  = true

}

class Current(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Current"
    override val symbol = "I"
    override val unit   = "A"
    override val important  = true

}