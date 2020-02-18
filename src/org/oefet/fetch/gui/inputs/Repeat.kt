package org.oefet.fetch.gui.inputs

import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.Settings

object Repeat : Grid("Repeat", 2) {

    val basic = Fields("Repeat Parameters")
    val name  = basic.addTextField("Repeat Name")
    val times = basic.addIntegerField("Repeat Count", 5)

    val subQueue  = ActionQueue()
    val queueList = FetChQueue("Repeat Actions", subQueue)

    init {
        addAll(basic, queueList)
        queueList.addRepeat.isDisabled = true
        queueList.addTSweep.isDisabled = true
        queueList.addStress.isDisabled = true
        basic.linkConfig(Settings.repeatBasic)
    }

    fun ask(queue: ActionQueue) {

        var i = 0
        while (queue.getVariableCount("N${if (i > 0) i.toString() else ""}") > 0) i++
        name.set("N${if (i > 0) i.toString() else ""}")

        subQueue.clear()

        if (showAndWait()) {

            val name  = name.get()
            val times = times.get()

            repeat(times) { n ->

                for (action in subQueue) {

                    val copy = action.copy()
                    copy.setVariable(name, n.toString())
                    if (copy is ActionQueue.MeasureAction) copy.setAttribute(name, n)

                    queue.addAction(copy)

                }

            }

        }

    }

}