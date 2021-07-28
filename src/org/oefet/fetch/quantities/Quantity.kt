package org.oefet.fetch.quantities

import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass

interface Quantity<T> {

    val value: T
    val error: T
    val name: String
    val symbol: String
    val unit: String
    val parameters: List<Quantity<*>>
    val possibleParameters: List<KClass<out Quantity<*>>>
    val important: Boolean

    fun isCompatibleWith(other: Quantity<*>, excluded: List<KClass<out Quantity<*>>> = emptyList()): Boolean {

        val toCheck = LinkedList(possibleParameters).apply {
            retainAll(other.possibleParameters)
            removeAll(excluded)
        }

        for (type in toCheck) {

            val thisParam  = this.getParameter(type)
            val otherParam = other.getParameter(type)

            if (thisParam == null && otherParam == null) {
                continue
            } else if (thisParam == null || otherParam == null) {
                return false
            } else if (thisParam.value != otherParam.value) {
                return false
            }

        }

        return true

    }

    fun <S> hasParameter(type: KClass<out Quantity<out S>>): Boolean {
        return getParameter(type) != null
    }

    fun <S> getParameter(type: KClass<out Quantity<out S>>) : Quantity<S>? {
        return parameters.find { it::class == type } as Quantity<S>?
    }

}

interface DoubleQuantity : Quantity<Double> {

    operator fun plus(other: Quantity<Double>): SimpleQuantity {
        return SimpleQuantity(value + other.value, sqrt(error.pow(2) + other.error.pow(2)))
    }

    operator fun plus(other: Number) : SimpleQuantity {
        return SimpleQuantity(value + other.toDouble(), error)
    }

    operator fun minus(other: Quantity<Double>): SimpleQuantity {
        return SimpleQuantity(value - other.value, sqrt(error.pow(2) + other.error.pow(2)))
    }

    operator fun minus(other: Number) : SimpleQuantity {
        return SimpleQuantity(value - other.toDouble(), error)
    }

    operator fun times(other: Quantity<Double>): SimpleQuantity {
        return SimpleQuantity(
            value * other.value,
            abs(value * other.value) * sqrt((error / value).pow(2) + (other.error / other.value).pow(2))
        )
    }

    operator fun times(other: Number) : SimpleQuantity {
        return SimpleQuantity(value * other.toDouble(), error * other.toDouble())
    }

    operator fun div(other: Quantity<Double>): SimpleQuantity {
        return SimpleQuantity(
            value / other.value,
            abs(value * other.value) * sqrt((error / value).pow(2) + (other.error / other.value).pow(2))
        )
    }

    operator fun div(other: Number) : SimpleQuantity {
        return SimpleQuantity(value / other.toDouble(), error / other.toDouble())
    }

    fun pow(power: Int): SimpleQuantity {
        return SimpleQuantity(value.pow(power), abs(value.pow(power) * power * (error / value)))
    }

    fun pow(power: Double): SimpleQuantity {
        return SimpleQuantity(value.pow(power), abs(value.pow(power) * power * (error / value)))
    }

}

class SimpleQuantity(override val value: Double, override val error: Double) : DoubleQuantity {

    constructor(quantity: Quantity<Double>) : this(quantity.value, quantity.error)

    override val name                                              = "Simple Quantity"
    override val symbol                                            = "x"
    override val unit                                              = "-"
    override val parameters: List<Quantity<*>>                     = emptyList()
    override val possibleParameters: List<KClass<out Quantity<*>>> = emptyList()
    override val important                                         = false

}

