package org.oefet.fetch

import jisa.devices.Configuration
import jisa.devices.interfaces.Instrument
import jisa.experiment.Measurement
import jisa.gui.CheckGrid
import jisa.gui.Element
import jisa.gui.Field
import jisa.gui.Fields
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FetChEntity : Measurement() {

    private    val errors  = LinkedList<String>()
    protected  val setters = LinkedList<() -> Unit>()

    open fun createDisplay(data: ResultTable): Element {
        return FetChPlot(name).apply {
            createSeries().watchAll(data)
        }
    }

    fun ResultTable.mapRow(vararg data: Pair<Column<*>, Any>) {
        addData(mapOf(*data))
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

    open fun getCustomParams(): List<Element> {
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

    fun <I : Instrument> optionalInstrument(name: String, type: KClass<I>): ODelegate<I?> {

        val config = addInstrument(name, type.java)
        val del = ODelegate<I?>(config)

        setters += { del.setNow(errors) }

        return del

    }

    fun <I : Instrument> requiredInstrument(name: String, type: KClass<I>): RDelegate<I> {

        val config = addInstrument(name, type.java)
        val del    = RDelegate<I>(config)

        setters += { del.setNow(errors) }

        return del

    }

    fun <T> userInput(parameter: Parameter<T>): PDelegate<T> {
        return PDelegate(parameter)
    }

    fun userInput(section: String, name: String, defaultValue: String): PDelegate<String> {
        return userInput(StringParameter(section, name, null, defaultValue))
    }

    fun userInput(name: String, defaultValue: String): PDelegate<String> {
        return userInput("Basic", name, defaultValue)
    }

    fun userInput(section: String, name: String, defaultValue: Double): PDelegate<Double> {
        return userInput(DoubleParameter(section, name, null, defaultValue))
    }

    fun userInput(name: String, defaultValue: Double): PDelegate<Double> {
        return userInput("Basic", name, defaultValue)
    }

    fun userInput(section: String, name: String, defaultValue: Int): PDelegate<Int> {
        return userInput(IntegerParameter(section, name, null, defaultValue))

    }

    fun userInput(name: String, defaultValue: Int): PDelegate<Int> {
        return userInput("Basic", name, defaultValue)
    }

    fun userInput(section: String, name: String, defaultValue: Boolean): PDelegate<Boolean> {
        return userInput(BooleanParameter(section, name, null, defaultValue))

    }

    fun userInput(name: String, defaultValue: Boolean): PDelegate<Boolean> {
        return userInput("Basic", name, defaultValue)
    }

    fun userChoice(section: String, name: String, vararg options: String): PDelegate<Int> {
        return userInput(ChoiceParameter(section, name, 0, *options))
    }

    fun userInput(section: String, name: String, defaultValue: Range<Double>): PDelegate<Range<Double>> {
        return userInput(RangeParameter(section, name, null, defaultValue))
    }

    fun userInput(name: String, defaultValue: Range<Double>): PDelegate<Range<Double>> {
        return userInput("Basic", name, defaultValue)
    }

    fun <I> customInput(tag: String, element: Element, getter: () -> I, setter: (I) -> Unit) : PDelegate<I> {
        return userInput(CustomParameter(tag, element, getter, setter))
    }

    fun <I> customInput(tag: String, element: Element, getter: () -> I, setter: (I) -> Unit, reader: (String?) -> I?, writer: (I) -> String) : PDelegate<I> {
        return userInput(CustomParameter(tag, element, getter, setter, { b -> reader(b.stringValue(tag).getOrDefault(null)) ?: getter() }, { b, v -> b.stringValue(tag).set(writer(v)) }))
    }

    fun customInput(checkGrid: CheckGrid) : PDelegate<Array<BooleanArray>> {

        return customInput(checkGrid.title, checkGrid, checkGrid::getValues, checkGrid::setValues,
            { it?.split(";")?.map{ it.split(",").map(String::toBoolean).toBooleanArray() }?.toTypedArray() },
            { it.joinToString(";") { it.joinToString(",") } }
        )

    }

    fun <I> customInput(fields: Fields, field: Field<I>) : PDelegate<I> {
        return customInput(field.text, fields, field::get, field::set)
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
            if (!set) throw Exception("${conf.name} is not configured")
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