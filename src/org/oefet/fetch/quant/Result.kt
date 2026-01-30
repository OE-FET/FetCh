package org.oefet.fetch.quant

import kotlin.reflect.KClass

class Result(name: String, symbol: String, type: Type, value: Double, error: Double, val parameters: List<Quantity<*>>) : DoubleQuantity(name, symbol, type, value, error) {

    fun findParameter(name: String): Quantity<*>? {
        return parameters.find { it.name == name }
    }

    fun <T : Quantity<*>> findParameter(name: String, klass: KClass<T>): T? {
        return parameters.find { it.name == name && klass.isInstance(it) } as T?
    }

    fun <T: Quantity<*>> findParameters(type: Type, klass: KClass<T>): List<T> {
        return parameters.filter { klass.isInstance(it) && it.type == type }.map { it as T }
    }

    fun findParameters(type: Type): List<Quantity<*>> {
        return parameters.filter { it.type == type }
    }

    fun <T: Quantity<*>> findParameter(type: Type, klass: KClass<T>): T? {
        return parameters.find { klass.isInstance(it) && it.type == type } as T?
    }

    fun findParameter(type: Type): Quantity<*>? {
        return parameters.find { it.type == type }
    }

    fun findDoubleParameter(name: String) : DoubleQuantity? {
        return findParameter(name, DoubleQuantity::class)
    }

    fun findDoubleParameter(name: String, orElse: Double) : DoubleQuantity {
        return findParameter(name, DoubleQuantity::class) ?: DoubleQuantity(name, Type.UNKNOWN, orElse, 0.0)
    }

    fun hasParameter(name: String): Boolean {
        return parameters.any { it.name == name }
    }

    fun overlappingParametersMatch(other: Result, vararg exclude: String): Boolean {

        val overlap = parameters.filter { a -> other.parameters.any { b -> a.name == b.name && a.type == b.type } }.filter { it.name !in exclude }

        return overlap.all { a -> a.value == other.parameters.find { b -> a.name == b.name && a.type == b.type }!!.value }

    }

}