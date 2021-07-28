package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class ThermalCalibration(
    override val value: Double, override val error: Double, override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) :
    DoubleQuantity {

    override val name   = "Thermal Power Coefficient"
    override val unit   = "W/K"
    override val symbol = "TPC"
    override val important  = false

}