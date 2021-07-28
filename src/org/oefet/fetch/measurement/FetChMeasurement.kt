package org.oefet.fetch.measurement

import jisa.results.ResultTable
import org.oefet.fetch.FetChEntity
import org.oefet.fetch.results.FetChResult
import org.oefet.fetch.results.SimpleResultFile

abstract class FetChMeasurement(private val name: String, label: String, val tag: String) : FetChEntity() {

    private val labelProperty = StringParameter("Basic", "Name", null, label)

    open fun processResults(data: ResultTable): FetChResult {
        return SimpleResultFile(name, tag, data)
    }

    override fun start() {
        results.setAttribute("Type", tag)
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

    override fun onError() {

    }

    override fun onInterrupt() {

    }

}

