package org.oefet.fetch.gui.inputs

import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.MainWindow

class Repeat : Grid("Repeat", 2), SweepInput {

    val basic     = Fields("Repeat Parameters")
    val name      = basic.addTextField("Repeat Name")
    val times     = basic.addIntegerField("Repeat Count", 5)
    val subQueue  = ActionQueue()

    init {
        basic.linkConfig(Settings.repeatBasic)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

    override fun ask(queue: ActionQueue) {

        clear();
        addAll(basic, FetChQueue("Interval Actions", subQueue))

        var i = 0
        while (queue.getVariableCount("N${if (i > 0) i.toString() else ""}") > 0) i++
        name.set("N${if (i > 0) i.toString() else ""}")

        subQueue.clear()

        if (showAsConfirmation()) {

            basic.writeToConfig()

            val name  = name.get()
            val times = times.get()

            repeat(times) { n ->

                for (action in subQueue) {

                    val copy = action.copy()
                    copy.setVariable(name, n.toString())
                    if (copy is ActionQueue.MeasureAction) copy.setAttribute(name, "$n rep")

                    queue.addAction(copy)

                }

            }

        }

    }

}