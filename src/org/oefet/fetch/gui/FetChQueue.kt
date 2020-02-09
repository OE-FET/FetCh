package org.oefet.fetch.gui

import jisa.experiment.ActionQueue
import jisa.gui.ActionQueueDisplay
import jisa.gui.GUI

class FetChQueue(name: String, private val queue: ActionQueue) : ActionQueueDisplay(name, queue) {

    val addButton   = addToolbarMenuButton("Add Action")
    val addWait     = addButton.addItem("Wait") { Time.askWait(queue) }

    init { addButton.addSeparator() }

    val addOutput   = addButton.addItem("Output Measurement")   { Output.askForMeasurement(queue) }
    val addTransfer = addButton.addItem("Transfer Measurement") { Transfer.askForMeasurement(queue) }

    init { addButton.addSeparator() }

    val addVHold    = addButton.addItem("Hold Voltage") { Hold.askForHold(queue) }
    val addTChange  = addButton.addItem("Temperature Change")   { Temperature.askForSingle(queue) }

    init { addButton.addSeparator() }

    val addTSweep   = addButton.addItem("Temperature Sweep") { Temperature.askForSweep(queue) }
    val addRepeat   = addButton.addItem("Repeat")            { Repeat.askForRepeat(queue) }

    val clearQueue  = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

}