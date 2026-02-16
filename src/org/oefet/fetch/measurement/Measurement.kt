package org.oefet.fetch.measurement

import jisa.results.ResultList
import jisa.results.ResultStream
import jisa.results.ResultTable
import org.oefet.fetch.Entity
import org.oefet.fetch.data.FetChData
import kotlin.reflect.full.companionObjectInstance

abstract class Measurement(name: String, label: String, val tag: String) : Entity<ResultTable>(name) {

    constructor(name: String, label: String) : this(name, label, name.replace(" ", ""))
    constructor(name: String) : this(name, name.replace(" ", ""), name.replace(" ", ""))

    init { this.label = label }

    override fun createData(): ResultTable {

        val companion = this::class.companionObjectInstance

        val data = if (companion is Columns) {
            ResultStream(currentFile, *companion.getColumns())
        } else {
            ResultList(emptyList())
        }

        data.setAttribute("Type", tag)

        return data

    }

    override fun interrupted(data: ResultTable) { }

    override fun error(data: ResultTable, exception: List<Throwable?>?) { }

    abstract fun processResults(data: ResultTable): FetChData?

    override fun handleData(data: ResultTable) {
        data.setAttribute("Instruments", instruments.associate { i -> i.name to mapOf("Instrument" to i.get().name) + i.get().allParameters.associate { p -> p.name to p.currentValue }})
        data.setAttribute("Values", parameters.associate { p -> p.name to p.get() })
    }

}