package org.oefet.fetch

import jisa.devices.Configuration
import jisa.devices.interfaces.Instrument
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.Element
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FetChEntity : Measurement() {

    private val errors = LinkedList<String>()
    protected val setters = LinkedList<() -> Unit>()

    open fun createPlot(data: ResultTable): FetChPlot {
        return FetChPlot(name).apply {
            createSeries().watchAll(data)
        }
    }

    /**
     * Checks that everything required for this measurement is present. Returns all missing instrument errors as
     * a list of strings. Measurement will only go ahead if this list is empty.
     */
    open fun checkForErrors(): List<String> = emptyList()

    open fun loadInstruments(errors: Boolean = false) {

        setters.forEach { it() }

        if (!errors) {
            this.errors.clear()
        }

    }

    open fun getExtraTabs(): List<Element> {
        return emptyList()
    }

    override fun start() {

        errors.clear()
        loadInstruments(true)

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

    fun <I : Instrument> optionalConfig(name: String, type: KClass<I>): ODelegate<I?> {

        val config = addInstrument(name, type.java)
        val del = ODelegate<I?>(config)

        setters += { del.setNow(errors) }

        return del

    }

    fun <I : Instrument> requiredConfig(name: String, type: KClass<I>): RDelegate<I> {

        val config = addInstrument(name, type.java)
        val del    = RDelegate<I>(config)

        setters += { del.setNow(errors) }

        return del

    }

    fun <T> input(parameter: Parameter<T>): PDelegate<T> {
        return PDelegate(parameter)
    }

    fun input(section: String, name: String, defaultValue: String): PDelegate<String> {
        return input(StringParameter(section, name, null, defaultValue))
    }

    fun input(name: String, defaultValue: String): PDelegate<String> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Double): PDelegate<Double> {
        return input(DoubleParameter(section, name, null, defaultValue))
    }

    fun input(name: String, defaultValue: Double): PDelegate<Double> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Int): PDelegate<Int> {
        return input(IntegerParameter(section, name, null, defaultValue))

    }

    fun input(name: String, defaultValue: Int): PDelegate<Int> {
        return input("Basic", name, defaultValue)
    }

    fun input(section: String, name: String, defaultValue: Boolean): PDelegate<Boolean> {
        return input(BooleanParameter(section, name, null, defaultValue))

    }

    fun input(name: String, defaultValue: Boolean): PDelegate<Boolean> {
        return input("Basic", name, defaultValue)
    }

    fun choice(section: String, name: String, vararg options: String): PDelegate<Int> {
        return input(ChoiceParameter(section, name, 0, *options))

    }

    fun input(section: String, name: String, defaultValue: Range<Double>): PDelegate<Range<Double>> {
        return input(RangeParameter(section, name, null, defaultValue))
    }

    fun input(name: String, defaultValue: Range<Double>): PDelegate<Range<Double>> {
        return input("Basic", name, defaultValue)
    }

    fun runRegardless(toRun: () -> Unit) {

        try {
            toRun()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    class MDelegate<T, M>(private val parameter: Parameter<T>, private val map: (T) -> M) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): M {
            return map(parameter.value)
        }

    }

    class PDelegate<I>(private val parameter: Parameter<I>) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return parameter.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: I) {
            parameter.value = value
        }

        infix fun <M> map(mapper: (I) -> M): MDelegate<I, M> {
            return MDelegate(parameter, mapper)
        }

    }

    class ODelegate<I : Instrument?>(private val conf: Configuration<I>) {

        private var instrument: I? = null
        private var condition: () -> Boolean = { false }
        private var set: Boolean = false

        init {
            conf.addChangeListener { set = false }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I? {
            if (!set) setNow()
            return instrument
        }

        infix fun requiredIf(condition: () -> Boolean): ODelegate<I> {
            this.condition = condition
            return this
        }

        fun setNow(errors: MutableList<String> = ArrayList()) {

            this.instrument = conf.instrument

            if (condition() && instrument == null) {
                errors.add("${conf.name} is not configured")
            }

            set = true;

        }

    }

    class RDelegate<I : Instrument>(private val conf: Configuration<I>) {

        private lateinit var instrument: I
        private var set: Boolean = false

        init {
            conf.addChangeListener { set = false }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            if (!set) setNow()
            return instrument
        }

        fun setNow(errors: MutableList<String> = ArrayList()) {

            val instrument = conf.instrument

            if (instrument == null) {
                errors.add("${conf.name} is not configured")
            } else {
                this.instrument = instrument
                set = true;
            }

        }

    }

    fun Double.toMSec(): Int {
        return (1e3 * this).toInt()
    }

    fun Int.toSeconds(): Double {
        return this.toDouble() / 1e3
    }


}