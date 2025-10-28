package org.oefet.fetch.sweep

import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.maths.Range
import org.oefet.fetch.quant.Type

class Repeat : FetChSweep<Int>("Repeat", "N", Type.INDEX, Icon.REPEAT.blackImage) {

    val count by userInput("Basic", "Count", 5)

    override fun getValues(): List<Int> {
        return Range.count(0, count-1).array().toList()
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {
        return actions
    }

    override fun formatValue(value: Int): String = "$value"

}