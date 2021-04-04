package org.oefet.fetch.sweep

import jisa.experiment.ActionQueue
import jisa.experiment.Col
import jisa.experiment.ResultTable
import java.util.*

class Repeat : Sweep("Repeat") {

    val count by input("Basic", "Count", 5)

    override fun generateActions(): List<ActionQueue.Action> {

        val list = LinkedList<ActionQueue.Action>()

        for (i in 0 until count) {

            list += queue.getAlteredCopy { it.setAttribute("N", "$i") }

        }

        return list

    }

    override fun run(results: ResultTable?) {

    }

    override fun onFinish() {

    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

}