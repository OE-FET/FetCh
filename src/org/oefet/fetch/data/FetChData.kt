package org.oefet.fetch.data

import javafx.scene.image.Image
import jisa.enums.Icon
import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Quantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.StringQuantity
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYQuantity
import org.oefet.fetch.quant.XYZQuantity
import java.util.*
import kotlin.reflect.KClass

abstract class FetChData(val name: String, val tag: String, val data: ResultTable, val icon: Image = Icon.DATA.blackImage) {

    val parameters = LinkedList<Quantity<*>>()

    val results by lazy { processData(data) }

    init {

        if (data.getAttribute("Type") != tag) {
            throw InvalidDataException(name, data.getAttribute("Type") ?: "null")
        }

        val parameters = data.getAttribute("parameters", List::class.java) as List<Map<String, String>>?

        if (parameters != null) {
            this.parameters.addAll(parameters.map { Quantity.fromMap(it) })
        } else {
            this.parameters.addAll(findOldParameters())
        }

        val version = data.getAttribute("FetCh Version", java.lang.Double::class.java)


    }

    protected fun findOldParameters(): List<Quantity<*>> {

        val list = mutableListOf<Quantity<*>>()

        val device       = data.getAttribute("Name") ?: "Unknown Device"
        val length       = data.getAttribute("Length")?.removeSuffix("m")?.toDouble() ?: 300e-6
        val separation   = data.getAttribute("FPP Separation")?.removeSuffix("m")?.toDouble() ?: 100e-6
        val width        = data.getAttribute("Width")?.removeSuffix("m")?.toDouble() ?: 100e-6
        val thickness    = data.getAttribute("Thickness")?.removeSuffix("m")?.toDouble() ?: 30e-9
        val dielectric   = data.getAttribute("Dielectric Thickness")?.removeSuffix("m")?.toDouble() ?: 400e-9
        val permittivity = data.getAttribute("Dielectric Permittivity")?.toDouble() ?: 1.49
        val repeat       = data.getAttribute("N")?.toDouble()
        val stress       = data.getAttribute("S")?.removeSuffix("s")?.toDouble()
        val field        = data.getAttribute("B")?.removeSuffix("T")?.toDouble()
        val positionX    = data.getAttribute("P")?.trim('(', ')')?.split(",")?.get(0)?.toDouble()
        val positionY    = data.getAttribute("P")?.trim('(', ')')?.split(",")?.get(1)?.toDouble()
        val voltage      = data.getAttribute("V")?.removeSuffix("V")?.toDouble()
        val current      = data.getAttribute("I")?.removeSuffix("A")?.toDouble()
        val position     = data.getAttribute("POS")?.split(", ")?.map { it.toDouble() }?.toDoubleArray()
        val xyIndex      = data.getAttribute("XY")?.split(", ")?.map { it.toInt() }?.toIntArray()


        val attributes = data.attributes
        val column     = data.findColumn("Temperature")

        val temperature = when {

            attributes.containsKey("T")           -> data.getAttribute("T").removeSuffix("K").toDouble()
            attributes.containsKey("Temperature") -> data.getAttribute("Temperature").removeSuffix("K").toDouble()
            column != null                        -> data.mean(column)
            else                                  -> Double.NaN

        }

        list += StringQuantity("Name", "dev", Type.LABEL, device)
        list += DoubleQuantity("Length", "l", Type.DISTANCE, length, 0.0)
        list += DoubleQuantity("FPP Separation", "l_fpp", Type.DISTANCE, separation, 0.0)
        list += DoubleQuantity("Width", "w", Type.DISTANCE, width, 0.0)
        list += DoubleQuantity("Thickness", "d", Type.DISTANCE, thickness, 0.0)
        list += DoubleQuantity("Dielectric Thickness", "d_diel", Type.DISTANCE, dielectric, 0.0)
        list += DoubleQuantity("Dielectric Permittivity", "ε_r", Type.RELATIVE_PERMITTIVITY, permittivity, 0.0)

        if (repeat != null)   list += DoubleQuantity("Repeat", "N", Type.INDEX, repeat, 0.0)
        if (stress != null)   list += DoubleQuantity("Time", "t", Type.TIME, stress, 0.0)
        if (field != null)    list += DoubleQuantity("B Field", "B", Type.B_FIELD, field, 0.0)
        if (voltage != null)  list += DoubleQuantity("Voltage", "V", Type.VOLTAGE, voltage, 0.0)
        if (current != null)  list += DoubleQuantity("Current", "I", Type.CURRENT, current, 0.0)

        if (position != null)
            list += XYZQuantity("XYZ Position", "XYZ", Type.DISTANCE, position[0], position[1], position[2], 0.0, 0.0, 0.0)

        if (positionX != null && positionY != null)
            list += XYQuantity("XY Position", "XY", Type.DISTANCE, positionX, positionY, 0.0, 0.0)

        return list

    }

    fun findParameter(name: String): Quantity<*>? {
        return parameters.find { it.name == name }
    }

    fun <T : Quantity<*>> findParameter(name: String, klass: KClass<T>): T? {
        return parameters.find { it.name == name && klass.isInstance(it) } as T?
    }

    fun findDoubleParameter(name: String) : DoubleQuantity? {
        return findParameter(name, DoubleQuantity::class)
    }

    fun findDoubleParameter(name: String, orElse: Double) : DoubleQuantity {
        return findParameter(name, DoubleQuantity::class) ?: DoubleQuantity(name, Type.UNKNOWN, orElse, 0.0)
    }

    fun <T: Quantity<*>> findParameters(type: Type, klass: KClass<T>): List<T> {
        return parameters.filter { klass.isInstance(it) && it.type == type }.map { it as T }
    }

    fun findParameters(type: Type): List<Quantity<*>> {
        return parameters.filter { it.type == type }
    }

    protected abstract fun processData(data: ResultTable): List<Result>

    abstract fun getDisplay(): Element

    abstract fun generateHybrids(results: List<Result>): List<Result>

}

class InvalidDataException(name: String, tag: String) : Exception("Data set contains invalid tag ($tag) for $name.")