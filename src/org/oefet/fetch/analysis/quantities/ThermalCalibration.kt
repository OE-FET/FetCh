package org.oefet.fetch.analysis.quantities

import kotlin.reflect.KClass

class ThermalCalibration(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name   = "Thermal Power Coefficient"
    override val unit   = "W/K"
    override val symbol = "TPC"
    override val extra  = false

}