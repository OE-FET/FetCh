package org.oefet.fetch.gui

import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid

object Repeat : Grid("Repeat", 2) {

    val basic = Fields("Repeat Parameters")
    val name  = basic.addTextField("Repeat Name")
    val times = basic.addIntegerField("Repeat Count")

    val subQueue  = ActionQueue()
    val queueList = FetChQueue("Repeat Actions", subQueue)

    init {
        addAll(basic, queueList)
        queueList.addRepeat.isDisabled = true
    }

    fun askForRepeat(queue: ActionQueue) {

        name.set("Repeat${queue.size}")

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

                        copy.setBefore { Measure.showMeasurement(copy); Results.addMeasurement(copy); }
                        copy.name = "${copy.name} (${name.get()} = $it)"
                    }

                    queue.addAction(copy)
                }

            }

        }

    }

}