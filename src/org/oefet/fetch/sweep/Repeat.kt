package org.oefet.fetch.sweep

import jisa.experiment.queue.Action
import jisa.maths.Range

class Repeat : Sweep<Int>("Repeat") {

    val count by input("Basic", "Count", 5)

    override fun getValues(): List<Int> {
        return Range.count(0, count-1).array().toList()
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {

        actions.forEach {
            it.setAttribute("N", "$value")
            it.addTag("N = $value")
        }

        return actions

    }

    override fun formatValue(value: Int): String = "$value"

}