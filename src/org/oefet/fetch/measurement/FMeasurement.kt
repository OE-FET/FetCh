package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.Range
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ResultFile
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FMeasurement(private val name: String, label: String, val type: String) : Measurement() {

    private   val labelProperty = StringParameter("Basic", "Name", null, label)
    private   val errors        = LinkedList<String>()
    protected val setters       = LinkedList<() -> Unit>()

    abstract fun createPlot(data: ResultTable): Plot

    abstract fun processResults(data: ResultTable, extra: List<Quantity>): ResultFile

    /**
     * Checks that everything required for this measurement is present. Returns all missing instrument errors as
     * a list of strings. Measurement will only go ahead if this list is empty.
     */
    open fun checkForErrors() : List<String> = emptyList()

    open fun loadInstruments() {
        setters.forEach { it() }
    }

    override fun start() {

        errors.clear()
        loadInstruments()

        errors += checkForErrors()

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

        results.setAttribute("Type", type)

        super.start()

    }

    override fun getName(): String {
        return this.name
    }

    override fun getLabel(): String {
        return labelProperty.value
    }

    override fun setLabel(value: String) {
        labelProperty.value = value
    }

    fun <I: Instrument> optionalConfig(name: String, type: KClass<I>): ODelegate<I?> {

        val config = addInstrument(name, type.java)
        val del    = ODelegate<I?>()

        setters += { del.setValue(config.instrument) }

        return del

    }

    fun <I: Instrument> requiredConfig(name: String, type: KClass<I>): RDelegate<I> {

        val config = addInstrument(name, type.java)
        val del    = RDelegate<I>()

        setters += {

            val inst = config.instrument

            if (inst != null) {
                del.setValue(config.instrument)
            } else {
                errors += "$name is not configured"
            }

        }

        return del

    }

    fun <T> input(parameter: Parameter<T>) : PDelegate<T> {
        return PDelegate(parameter)
    }

    fun <T,M> input(parameter: Parameter<T>, map: (T) -> M) : MDelegate<T,M> {
        return MDelegate(parameter, map)
    }

    fun input(section: String, name: String, defaultValue: String) : PDelegate<String> {
        return input(StringParameter(section, name, null, defaultValue))
    }

    fun input(section: String, name: String, defaultValue: Double) : PDelegate<Double> {
        return input(DoubleParameter(section, name, null, defaultValue))
    }

    fun <M> input(section: String, name: String, defaultValue: Double, map: (Double) -> M) : MDelegate<Double, M> {
        return input(DoubleParameter(section, name, null, defaultValue), map)
    }

    fun input(section: String, name: String, defaultValue: Int) : PDelegate<Int> {
        return input(IntegerParameter(section, name, null, defaultValue))

    }

    fun input(section: String, name: String, defaultValue: Boolean) : PDelegate<Boolean> {
        return input(BooleanParameter(section, name, null, defaultValue))

    }

    fun choice(section: String, name: String, vararg options: String) : PDelegate<Int> {
        return input(ChoiceParameter(section, name, 0, *options))

    }

    fun input(section: String, name: String, defaultValue: Range<Double>) : PDelegate<Range<Double>> {
        return input(RangeParameter(section, name, null, defaultValue))
    }

    fun runRegardless (toRun: () -> Unit) {

        try {
            toRun()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    class MDelegate<T,M>(private val parameter: Parameter<T>, private val map: (T) -> M) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): M {
            return map(parameter.value)
        }

    }

    class PDelegate<I>(private val parameter: Parameter<I>) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return parameter.value
        }

    }

    class ODelegate<I: Instrument?> {

        private var instrument: I? = null

        fun setValue(instrument: I?) {
            this.instrument = instrument
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I? {
            return instrument
        }

    }

    class RDelegate<I: Instrument> {

        private lateinit var instrument: I

        fun setValue(instrument: I) {
            this.instrument = instrument
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return instrument
        }

    }

}

