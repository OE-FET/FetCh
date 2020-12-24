package org.oefet.fetch.quantities

import kotlin.reflect.KClass

open class Time(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name   = "Stress Time"
    override val symbol = "t"
    override val unit   = "s"
    override val extra  = true

}

class IntegrationTime(override val value: Double,
                      override val parameters: List<Quantity> = emptyList(),
                      override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Time(value, 0.0, parameters, possibleParameters) {

    override val name   = "Integration Time"
    override val symbol = "t_int"
    override val unit   = "s"
    override val extra  = false

}

class DelayTime(override val value: Double,
                      override val parameters: List<Quantity> = emptyList(),
                      override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Time(value, 0.0, parameters, possibleParameters) {

    override val name   = "Delay Time"
    override val symbol = "t_del"
    override val unit   = "ms"
    override val extra  = false

}

class HeaterHoldTime(override val value: Double,
                      override val parameters: List<Quantity> = emptyList(),
                      override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Time(value, 0.0, parameters, possibleParameters) {

    override val name   = "Heater Hold Time"
    override val symbol = "t_hh"
    override val unit   = "ms"
    override val extra  = false

}

class GateHoldTime(override val value: Double,
                      override val parameters: List<Quantity> = emptyList(),
                      override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Time(value, 0.0, parameters, possibleParameters) {

    override val name   = "Gate Hold Time"
    override val symbol = "t_gh"
    override val unit   = "ms"
    override val extra  = false

}