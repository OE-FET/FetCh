package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class Drain(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Source-Drain Voltage"
    override val symbol = "Vsd"
    override val unit   = "V"
    override val important  = true

}

class TrueSDVoltage(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Contact Resistance"
    override val symbol = "Vsd"
    override val unit   = "V"
    override val important  = true

}

class Power(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name   = "Power"
    override val symbol = "P"
    override val unit   = "W"
    override val important  = true

}