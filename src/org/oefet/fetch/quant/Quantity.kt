package org.oefet.fetch.quant

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

open class Quantity<T : Any>(val name: String, val symbol: String, val type: Type, val value: T, val error: T) {

    companion object {

        fun fromMap(map: Map<String, String>): Quantity<*> {

            val name   = map["name"] ?: "UNKNOWN"
            val symbol = map["symbol"] ?: name
            val data   = map["data"] ?: "UNKNOWN"
            val type   = Type.valueOf(map["type"] ?: "UNKNOWN")
            val value  = map["value"] ?: "UNKNOWN"
            val error  = map["error"] ?: "UNKNOWN"

            return when (data) {

                "Double"  -> DoubleQuantity(name, symbol, type, value.toDouble(), error.toDouble())

                "XYPoint" -> {
                    val xy  = value.removeSurrounding("(", ")").split(",").map { it.toDouble() }
                    val exy = error.removeSurrounding("(", ")").split(",").map { it.toDouble() }
                    XYQuantity(name, symbol, type, xy[0], xy[1], exy[0], exy[1])
                }

                "XYZPoint" -> {
                    val xyz  = value.removeSurrounding("(", ")").split(",").map { it.toDouble() }
                    val exyz = error.removeSurrounding("(", ")").split(",").map { it.toDouble() }
                    XYZQuantity(name, symbol, type, xyz[0], xyz[1], xyz[2], exyz[0], exyz[1], exyz[2])
                }

                else  -> StringQuantity(name, symbol, type, value)

            }

        }

    }

    fun toMap(): Map<String, String> {

        return mapOf(
            "name"   to name,
            "symbol" to symbol,
            "data"   to (value::class.simpleName ?: "String"),
            "type"   to type.toString(),
            "value"  to value.toString(),
            "error"  to error.toString(),
            "units"  to type.units
        )

    }

}

open class StringQuantity(name: String, symbol: String, type: Type, value: String) : Quantity<String>(name, symbol, type, value, "")

open class DoubleQuantity(name: String, symbol: String, type: Type, value: Double, error: Double) : Quantity<Double>(name, symbol, type, value, error) {

    constructor(name: String, type: Type, value: Double, error: Double) : this(name, name, type, value, error)

    constructor(name: String, symbol: String, value: Double, error: Double) : this(name, symbol, Type.UNKNOWN, value, error)

    constructor(name: String, symbol: String, value: Double) : this(name, symbol, value, 0.0)

    constructor(name: String, symbol: String, type: Type, value: Double) : this(name, symbol, type, value, 0.0)

    constructor(name: String, value: Double, error: Double) : this(name, name, Type.UNKNOWN, value, error)

    constructor(name: String, value: Double) : this(name, name, value, 0.0)

    constructor(name: String, type: Type, value: Double) : this(name, name, type, value, 0.0)

    operator fun plus(other: DoubleQuantity): DoubleQuantity {
        return DoubleQuantity(
            "$name + ${other.name}",
            type,
            value + other.value,
            sqrt(error.pow(2) + other.error.pow(2))
        )
    }

    operator fun plus(other: Double): DoubleQuantity {
        return DoubleQuantity("$name + $other", type, value + other, error)
    }

    operator fun minus(other: DoubleQuantity): DoubleQuantity {
        return DoubleQuantity(
            "$name - ${other.name}",
            type,
            value - other.value,
            sqrt(error.pow(2) + other.error.pow(2))
        )
    }

    operator fun minus(other: Double): DoubleQuantity {
        return DoubleQuantity("$name - $other", type, value - other, error)
    }

    operator fun times(other: DoubleQuantity): DoubleQuantity {
        return DoubleQuantity(
            "$name * ${other.name}",
            Type.UNKNOWN,
            value * other.value,
            value * other.value * sqrt((error / value).pow(2) + (other.error / other.value).pow(2))
        )
    }

    fun pow(exponent: Double): DoubleQuantity {
        return DoubleQuantity(
            "$name ^ ${exponent}",
            Type.UNKNOWN,
            value.pow(exponent),
            value.pow(exponent).absoluteValue * exponent.absoluteValue * (error.absoluteValue / value.absoluteValue),
        )
    }

    operator fun times(other: Double): DoubleQuantity {
        return DoubleQuantity("$other * $name", type, value * other, error * other)
    }

    operator fun div(other: DoubleQuantity): DoubleQuantity {
        return DoubleQuantity(
            "$name / ${other.name}",
            Type.UNKNOWN,
            value / other.value,
            (value / other.value) * sqrt((error / value).pow(2) + (other.error / other.value).pow(2))
        )
    }

    operator fun div(other: Double): DoubleQuantity {
        return DoubleQuantity("$name / $other", type, value / other, error / other)
    }

    fun abs(): DoubleQuantity {
        return DoubleQuantity("|$name|", type, value.absoluteValue, error.absoluteValue)
    }

    fun morph(name: String, symbol: String, type: Type): DoubleQuantity {
        return DoubleQuantity(name, symbol, type, value, error)
    }

    fun toResult(name: String, symbol: String, type: Type, parameters: List<Quantity<*>>): Result {
        return Result(name, symbol, type, value, error, parameters)
    }


}

data class XYPoint(val x: Double, val y: Double) {

    override fun toString(): String {
        return "($x,$y)"
    }

}

open class XYQuantity(name: String, symbol: String, type: Type, x: Double, y: Double, eX: Double, eY: Double) :
    Quantity<XYPoint>(name, symbol, type, XYPoint(x, y), XYPoint(eX, eY))

data class XYZPoint(val x: Double, val y: Double, val z: Double) {

    override fun toString(): String {
        return "($x,$y,$z)"
    }

}

open class XYZQuantity(
    name: String,
    symbol: String,
    type: Type,
    x: Double,
    y: Double,
    z: Double,
    eX: Double,
    eY: Double,
    eZ: Double
) : Quantity<XYZPoint>(name, symbol, type, XYZPoint(x, y, z), XYZPoint(eX, eY, eZ))