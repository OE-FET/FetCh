package org.oefet.fetch.analysis

import java.util.*
import kotlin.reflect.KClass

interface Quantity {

    val value: Number
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
                "rep" -> Repeat(parts[0].toInt())
                else  -> null

            }

        }

    }

}

class Device(
    override val value: Int
) : Quantity {

    override val error: Double = 0.0
    override val parameters: List<Quantity> = emptyList()
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()

    override val name = "Device"
    override val symbol = "Dev"
    override val unit = ""
    override val extra = true

}

class Temperature(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Temperature"
    override val symbol = "T"
    override val unit = "K"
    override val extra = true

}

class Gate(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Source-Gate Voltage"
    override val symbol = "Vsg"
    override val unit = "V"
    override val extra = true

}

class Drain(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Source-Drain Voltage"
    override val symbol = "Vsd"
    override val unit = "V"
    override val extra = true

}

class Frequency(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Frequency"
    override val symbol = "f"
    override val unit = "Hz"
    override val extra = true

}

class RMSField(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "RMS Field Strength"
    override val symbol = "B"
    override val unit = "T"
    override val extra = false

}

class Repeat(
    override val value: Int,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val error = 0.0
    override val name = "Repeat No."
    override val symbol = "N"
    override val unit = ""
    override val extra = true

}

class Time(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Time"
    override val symbol = "t"
    override val unit = "s"
    override val extra = true

}

class HallCoefficient(
    override val value: Double, override val error: Double, override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) :
    Quantity {

    override val name = "Hall Coefficient"
    override val unit = "m^3/C"
    override val symbol = "Rh"
    override val extra = false

}

class Conductivity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Conductivity"
    override val symbol = "σ"
    override val unit = "S/cm"
    override val extra = false

}

abstract class Mobility : Quantity {

    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class FwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Forward Linear Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class BwdLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Backward Linear Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class FwdSatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Forward Saturation Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class BwdSatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Backward Saturation Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class MaxLinMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Max Linear Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class MaxSatMobility(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Mobility() {

    override val name = "Max Saturation Mobility"
    override val symbol = "μ"
    override val unit = "cm^2/Vs"
    override val extra = false

}

class SweepDirection(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Sweep Direction"
    override val symbol = "dir"
    override val unit = ""
    override val extra = false

}

class CarrierDensity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Carrier Density"
    override val symbol = "n"
    override val unit = "cm^-3"
    override val extra = false

}

class Length(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Channel Length"
    override val symbol = "lc"
    override val unit = "m"
    override val extra = false

}

class FPPSeparation(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "FPP Separation"
    override val symbol = "lf"
    override val unit = "m"
    override val extra = false

}

class Width(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Channel Width"
    override val symbol = "wc"
    override val unit = "m"
    override val extra = false

}

class Thickness(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Channel Thickness"
    override val symbol = "tc"
    override val unit = "m"
    override val extra = false

}

class DThickness(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Dielectric Thickness"
    override val symbol = "td"
    override val unit = "m"
    override val extra = false

}

class Permittivity(
    override val value: Double,
    override val error: Double,
    override val parameters: List<Quantity> = emptyList(),
    override val possibleParameters: List<KClass<out Quantity>> = emptyList()
) : Quantity {

    override val name = "Dielectric Permittivity"
    override val symbol = "ε"
    override val unit = ""
    override val extra = false

}