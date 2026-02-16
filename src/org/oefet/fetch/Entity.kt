package org.oefet.fetch

import jisa.experiment.Measurement
import jisa.gui.Doc
import jisa.gui.Element
import jisa.gui.measurement.MeasurementSetup
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Entity<T>(name: String) : Measurement<T>(name, "") {

    var currentFile: String = ""
    val setup by lazy { MeasurementSetup(this) }
    var label by userInput("Basic", "Name", name)

    fun applyAll() { setup.apply() }

    override fun getLabel(): String {
        return label
    }

    override fun setLabel(value: String) {
        label = value
    }

    open fun createDisplay(data: ResultTable): Element {

        return if (data.numericColumns.size >= 2) {

            FetChPlot(name).apply {
                createSeries().watch(data, data.getNthNumericColumn(0), data.getNthNumericColumn(1))
            }

        } else {

            Doc(name).apply {
                addHeading(name).setAlignment(Doc.Align.CENTRE)
            }

        }

    }

    fun <T> userInput(section: String, name: String, defaultValue: T): PDelegate<T> {

        val delegate = PDelegate<T>(defaultValue)
        addParameter(
            section,
            name,
            defaultValue!!.javaClass,
            Type.AUTO,
            defaultValue,
            { delegate.value!! },
            { delegate.value = it })
        return delegate

    }

    fun <T> customInput(name: String, defaultValue: T, getter: () -> T, setter: (T) -> Unit): PDelegate<T> {

        val delegate = PDelegate<T>(defaultValue)
        addParameter(
            name,
            name,
            defaultValue!!.javaClass,
            Type.CUSTOM,
            defaultValue,
            { delegate.value = getter() },
            { setter(delegate.value) })
        return delegate

    }

    fun userChoice(section: String, name: String, vararg options: String): PDelegate<Int> {

        val delegate = PDelegate<Int>(0)
        addParameter(
            section,
            name,
            Int::class.java,
            Type.AUTO,
            0,
            { delegate.value },
            { delegate.value = it },
            *options
        )
        return delegate

    }

    fun userTimeInput(section: String, name: String, defaultValue: Int): PDelegate<Int> {

        val delegate = PDelegate<Int>(defaultValue)
        addParameter(
            section,
            name,
            Int::class.java,
            Type.TIME,
            defaultValue,
            { delegate.value },
            { delegate.value = it })
        return delegate

    }

    fun <I : jisa.devices.Instrument> requiredInstrument(name: String, type: KClass<I>): RIDelegate<I> {

        val delegate = RIDelegate<I>()
        addInstrument(name, type.java, { delegate.instrument }, { delegate.instrument = it }, true)
        return delegate

    }

    fun <I : jisa.devices.Instrument> optionalInstrument(name: String, type: KClass<I>): OIDelegate<I> {

        val delegate = OIDelegate<I>(name)
        addInstrument(name, type.java, { delegate.instrument }, { delegate.instrument = it }, false)
        return delegate

    }

    open class Columns {

        protected fun decimalColumn(name: String, units: String? = null) =
            Column.ofDecimals(name, units).also { COLUMNS += it }

        protected fun integerColumn(name: String, units: String? = null) =
            Column.ofIntegers(name, units).also { COLUMNS += it }

        protected fun longColumn(name: String, units: String? = null) =
            Column.ofLongs(name, units).also { COLUMNS += it }

        protected fun textColumn(name: String, units: String? = null) =
            Column.ofText(name, units).also { COLUMNS += it }

        protected fun booleanColumn(name: String, units: String? = null) =
            Column.ofBooleans(name, units).also { COLUMNS += it }

        private val COLUMNS = ArrayList<Column<*>>()

        fun getColumns(): Array<Column<*>> = COLUMNS.toTypedArray()

    }

    open class PDelegate<I>(var value: I) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: I) {
            this.value = value
        }

        infix fun <R> map(transform: (I) -> R): MDelegate<I, R> {
            return MDelegate(this, transform)
        }

    }

    class MDelegate<I, R>(val pDelegate: PDelegate<I>, val map: (I) -> R) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): R {
            return map(pDelegate.getValue(thisRef, property))
        }

    }

    class RIDelegate<I : jisa.devices.Instrument>(var instrument: I? = null) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return instrument!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: I) {
            instrument = value
        }

    }

    class OIDelegate<I : jisa.devices.Instrument>(val name: String, var instrument: I? = null) {

        private var predicate: () -> Boolean = { false }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I? {
            return instrument
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: I?) {

            if (predicate() && value == null) {
                throw MissingInstrumentException("The instrument \"$name\" is required under these circumstances.")
            }

            instrument = value

        }

        infix fun requiredIf(predicate: () -> Boolean): OIDelegate<I> {
            this.predicate = predicate
            return this
        }

    }

    fun Double.toMSec(): Int {
        return (1e3 * this).toInt()
    }

    fun Int.toSeconds(): Double {
        return this.toDouble() / 1e3
    }

    fun runRegardless(vararg toRun: () -> Unit) {
        for (run in toRun) {
            try {
                run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

}