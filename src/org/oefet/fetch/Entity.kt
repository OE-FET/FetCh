package org.oefet.fetch

import jisa.experiment.Measurement
import jisa.results.Column
import jisa.results.ResultStream
import jisa.results.ResultTable
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObjectInstance

abstract class Entity(name: String, label: String) : Measurement<ResultTable>(name, label) {

    var currentFile: String = ""

    override fun createData(): ResultTable? {

        val companion = this::class.companionObjectInstance

        if (companion is Columns) {
            return ResultStream(currentFile, *companion.getColumns())
        } else {
            return null
        }

    }

    fun <T> userInput(section: String, name: String, defaultValue: T): PDelegate<T> {

        val delegate = PDelegate<T>(defaultValue)
        addParameter(section, name, defaultValue!!.javaClass, Type.AUTO, defaultValue, { delegate.value!! }, { delegate.value = it })
        return delegate

    }

    fun userChoice(section: String, name: String, vararg options: String): PDelegate<Int> {

        val delegate = PDelegate<Int>(0)
        addParameter(section, name, Int::class.java, Type.AUTO, 0, { delegate.value }, { delegate.value = it }, *options)
        return delegate

    }

    fun userTimeInput(section: String, name: String, defaultValue: Int): PDelegate<Int> {

        val delegate = PDelegate<Int>(defaultValue)
        addParameter(section, name, Int::class.java, Type.TIME, defaultValue, { delegate.value }, { delegate.value = it })
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

    class PDelegate<I>(var value: I) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): I {
            return value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: I) {
            this.value = value
        }

    }


}