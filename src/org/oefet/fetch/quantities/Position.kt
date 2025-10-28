package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class XPosition(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name = "X Position"
    override val symbol = "X"
    override val unit = "m"
    override val important = true

}

class YPosition(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name = "Y Position"
    override val symbol = "Y"
    override val unit = "m"
    override val important = true

}

class ZPosition(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : DoubleQuantity {

    override val name = "Z Position"
    override val symbol = "Z"
    override val unit = "m"
    override val important = true

}

data class XYIndex(val x: Int, val y: Int)

class XYIndexQuantity(
    override val value: XYIndex,
    override val parameters: List<Quantity<*>> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
) : Quantity<XYIndex> {
    override val error = XYIndex(0, 0)
    override val name  = "X-Y Index"
    override val symbol = "XY"
    override val unit= ""
    override val important = true
}
