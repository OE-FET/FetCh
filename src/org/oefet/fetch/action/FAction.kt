package org.oefet.fetch.action

import jisa.devices.interfaces.Instrument
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.FEntity
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FAction(private val name: String) : FEntity() {


    override fun getName(): String {
        return this.name
    }

    override fun setLabel(value: String) {
    }

    override fun onError() {

    }

    override fun onInterrupt() {

    }

}