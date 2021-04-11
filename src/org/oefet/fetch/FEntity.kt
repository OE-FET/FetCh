package org.oefet.fetch

import jisa.devices.interfaces.Instrument
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FEntity : Measurement() {

    private   val errors        = LinkedList<String>()
    protected val setters       = LinkedList<() -> Unit>()

    open fun createPlot(data: ResultTable): FetChPlot {
        return FetChPlot(name).apply {
            createSeries().watchAll(data)
        }
    }

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

        super.start()

    }

    override fun onError() {

    }

    override fun onInterrupt() {

    }

    fun <I: Instrument> optionalConfig(name: String, type: KClass<I>): ODelegate<I?> {

        val config = addInstrument(name, type.java)
        val del    = ODelegate<I?>(name, errors)

        setters += {del.setValue(config.instrument)}

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

    fun input(section: String, name: String, defaultValue: String) : PDelegate<String> {
        return input(StringParameter(section, name, null, defaultValue))
    }

    fun input (name: String, defaultValue: String) : PDelegate<String> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Double) : PDelegate<Double> {
        return input(DoubleParameter(section, name, null, defaultValue))
    }

    fun input (name: String, defaultValue: Double) : PDelegate<Double> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Int) : PDelegate<Int> {
        return input(IntegerParameter(section, name, null, defaultValue))

    }

    fun input (name: String, defaultValue: Int) : PDelegate<Int> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Boolean) : PDelegate<Boolean> {
        return input(BooleanParameter(section, name, null, defaultValue))

    }

    fun input (name: String, defaultValue: Boolean) : PDelegate<Boolean> {
        return input("Basic", name, defaultValue)
    }

    fun choice(section: String, name: String, vararg options: String) : PDelegate<Int> {
        return input(ChoiceParameter(section, name, 0, *options))

    }

    fun input(section: String, name: String, defaultValue: Range<Double>) : PDelegate<Range<Double>> {
        return input(RangeParameter(section, name, null, defaultValue))
    }

    fun input (name: String, defaultValue: Range<Double>) : PDelegate<Range<Double>> {
        return input("Basic", name, defaultValue)
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

        infix fun <M> map(mapper: (I) -> M): MDelegate<I,M> {
            return MDelegate(parameter, mapper)
        }

    }

    class ODelegate<I: Instrument?>(private val name: String, private val errors: MutableList<String>) {

        private var instrument: I? = null
        private var condition: () -> Boolean = { false }

        fun setValue(instrument: I?) {

            this.instrument = instrument

            if (condition() && instrument == null) {
                errors.add("$name is not configured")
            }

        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I? {
            return instrument
        }

        infix fun requiredIf(condition: () -> Boolean) : ODelegate<I> {
            this.condition = condition
            return this
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

    fun Double.toMSec(): Int {
        return (1e3 * this).toInt()
    }

    fun Int.toSeconds(): Double {
        return this.toDouble() / 1e3
    }



}