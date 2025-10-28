package org.oefet.fetch

import jisa.experiment.queue.MeasurementAction
import jisa.experiment.queue.SweepAction
import org.json.JSONArray
import org.oefet.fetch.quant.Quantity
import org.oefet.fetch.sweep.FetChSweep
import java.util.*
import kotlin.reflect.KClass

class FetChEntityAction(name: String, val entity: FetChEntity) : MeasurementAction(name, entity) {

    private val parameters = LinkedList<Quantity<*>>()

    constructor(entity: FetChEntity) : this(entity.name, entity)

    fun addParameter(parameter: Quantity<*>) {

        val found = getParameter(parameter.name)

        if (found != null) {
            parameters.remove(found)
        }

        parameters += parameter

    }

    fun getParameter(name: String): Quantity<*>? {
        return parameters.find { it.name == name }
    }

    fun <T : Quantity<*>> getParameter(name: String, type: KClass<T>): T? {
        return parameters.find { it.name == name && type.isInstance(it) } as T?
    }

    fun getParameterJSON(): String {
        return JSONArray(parameters.map { it.toMap() }).toString()
    }

    override fun getAttributeString(delim: String, assign: String): String {
        return parameters.joinToString(delim) { it.name + assign + it.value.toString() }
    }

    override fun onStart() {
        super.onStart()
        measurement.results.setAttribute("parameters", parameters.map { it.toMap() })
    }

    override fun copy(): FetChEntityAction {

        val copy = FetChEntityAction(getName(), measurement as FetChEntity)
        copyBasicParametersTo(copy)
        copy.setFileNameGenerator(fileNameGenerator)
        parameters.forEach(copy::addParameter)
        return copy

    }

}

class FetChSweepAction<T>(name: String, values: Iterable<T>, val generator: ActionGenerator<T>, val sweep: FetChSweep<T>) : SweepAction<T>(name, values, generator) {

    override fun copy(): FetChSweepAction<T> {

        val copy = FetChSweepAction<T>(getName(), getSweepValues(), generator, sweep)
        getAttributes().forEach { (key: String?, value: String?) -> copy.setAttribute(key, value) }
        copy.addActions(actions)

        return copy

    }

}