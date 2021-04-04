package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.Range
import org.oefet.fetch.FEntity
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ResultFile
import org.oefet.fetch.results.SimpleResultFile
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FMeasurement(private val name: String, label: String, val type: String) : FEntity() {

    private   val labelProperty = StringParameter("Basic", "Name", null, label)

    open fun processResults(data: ResultTable, extra: List<Quantity>): ResultFile {
        return SimpleResultFile(name, data, extra)
    }

    override fun start() {
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

    override fun onError() {

    }

    override fun onInterrupt() {

    }

}

