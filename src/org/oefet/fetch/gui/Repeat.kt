package org.oefet.fetch.gui

import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid

object Repeat : Grid("Repeat", 2) {

    val basic = Fields("Repeat Parameters")
    val name  = basic.addTextField("Repeat Name")
    val times = basic.addIntegerField("Repeat Count", 5)

    val subQueue  = ActionQueue()
    val queueList = FetChQueue("Repeat Actions", subQueue)

    init {
        addAll(basic, queueList)
        queueList.addRepeat.isDisabled = true
        basic.linkConfig(Settings.repeatBasic)
    }

    fun askForRepeat(queue: ActionQueue) {

        name.set("N${if (queue.size > 0) queue.size.toString() else ""}")
        subQueue.clear()

        if (showAndWait()) {

            repeat(times.get()) {

                for (action in subQueue) {

                    val copy = action.copy()

                    if (action is ActionQueue.MeasureAction && copy is ActionQueue.MeasureAction) {

                        if (action.resultPath != null) copy.setResultsPath(
                            action.resultPath.replace(
                                Measure.baseFile,
                                "${Measure.baseFile}-${name.get()}=$it"
                            )
                        )

                        copy.setBefore { Measure.processMeasurement(copy); Results.addMeasurement(copy); }
                        copy.name = "${action.name} (${name.get()} = $it)"

                        copy.setAttribute(name.get(), it)
                    }

                    queue.addAction(copy)
                }

            }

        }

    }

}