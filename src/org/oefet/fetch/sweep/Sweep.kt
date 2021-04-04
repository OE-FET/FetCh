package org.oefet.fetch.sweep

import jisa.devices.interfaces.Instrument
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.FEntity
import org.oefet.fetch.action.Action
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Sweep(private val name: String) : FEntity() {

    val queue = ActionQueue()

    override fun getName(): String {
        return this.name
    }

    override fun setLabel(value: String) {
    }

    override fun onError() {

    }

    override fun onInterrupt() {

    }

    abstract fun generateActions() : List<ActionQueue.Action>

}