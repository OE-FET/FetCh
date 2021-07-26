package org.oefet.fetch.quantities

import kotlin.reflect.KClass

class XPosition(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "X Position"
    override val symbol = "x"
    override val unit = ""
    override val important = true

}

class YPosition(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Y Position"
    override val symbol = "y"
    override val unit = ""
    override val important = true

}