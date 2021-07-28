package org.oefet.fetch.quantities

import kotlin.reflect.KClass

open class Repeat(
    override val value: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val error  = 0.0
    override val name   = "Repeat No."
    override val symbol = "N"
    override val unit   = ""
    override val important  = true

}

class AveragingCount(value: Double) : Repeat(value) {

    override val name   = "Averaging Count"
    override val symbol = "N_a"
    override val important  = false

}