package org.oefet.fetch.measurement

import jisa.results.ResultTable
import org.oefet.fetch.Entity
import org.oefet.fetch.data.FetChData
import org.oefet.fetch.data.SimpleData

abstract class Measurement(name: String, label: String, val tag: String) : Entity(name) {

    constructor(name: String, label: String) : this(name, label, name.replace(" ", ""))
    constructor(name: String) : this(name, name.replace(" ", ""), name.replace(" ", ""))

    init { this.label = label }

    open fun processResults(data: ResultTable): FetChData {
        return SimpleData(name, tag, data)
    }

    override fun createData(): ResultTable {
        val table = super.createData()
        table.setAttribute("Type", tag)
        return table
    }

    override fun interrupted(data: ResultTable?) { }

    override fun error(data: ResultTable?, exception: List<Throwable?>?) { }

}