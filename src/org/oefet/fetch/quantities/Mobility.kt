package org.oefet.fetch.quantities

import kotlin.reflect.KClass

abstract class Mobility : DoubleQuantity {

    override val symbol = "μ"
    override val unit   = "cm^2/Vs"
    override val important  = false

}

open class LinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : Mobility() {

    override val name = "Linear Mobility"

}

open class SatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : Mobility() {

    override val name = "Saturation Mobility"

}

class FwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : LinMobility(value, error, parameters, possibleParameters) {

    override val name = "Forward Linear Mobility"

}

class FwdSatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : SatMobility(value, error, parameters, possibleParameters) {

    override val name = "Forward Saturation Mobility"

}

class BwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : LinMobility(value, error, parameters, possibleParameters) {

    override val name = "Backward Linear Mobility"

}

class BwdSatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : SatMobility(value, error, parameters, possibleParameters) {

    override val name = "Backward Saturation Mobility"

}