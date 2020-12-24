package org.oefet.fetch.quantities

import java.util.*
import kotlin.reflect.KClass

interface Quantity {

    val value: Double
    val error: Double
    val name: String
    val symbol: String
    val unit: String
    val parameters: List<Quantity>
    val possibleParameters: List<KClass<out Quantity>>
    val extra: Boolean

    fun isCompatibleWith(other: Quantity): Boolean {

        val toCheck = LinkedList(possibleParameters).apply { retainAll(other.possibleParameters) }

        for (type in toCheck) {

            val thisParam  = this.parameters.find { it::class == type }
            val otherParam = other.parameters.find { it::class == type }

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

    companion object {

        fun parseValue(value: String): Quantity? {

            val parts = value.split(" ")

            if (parts.size < 2) return null

            return when (parts[1]) {

                "K"   -> Temperature(parts[0].toDouble(), 0.0)
                "Hz"  -> Frequency(parts[0].toDouble(), 0.0)
                "s"   -> Time(parts[0].toDouble(), 0.0)
                "T"   -> RMSField(parts[0].toDouble(), 0.0)
                "rep" -> Repeat(parts[0].toDouble())
                else  -> null

            }

        }

    }

}

