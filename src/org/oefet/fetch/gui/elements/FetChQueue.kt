package org.oefet.fetch.gui.elements

import jisa.experiment.ActionQueue
import jisa.gui.ActionQueueDisplay
import jisa.gui.GUI
import org.oefet.fetch.gui.inputs.*

class FetChQueue(name: String, private val queue: ActionQueue) : ActionQueueDisplay(name, queue) {

    val addButton   = addToolbarMenuButton("Add Action")
    val addWait     = addButton.addItem("Wait") { Time.askWait(queue) }

    init { addButton.addSeparator() }

    val addOutput   = addButton.addItem("Output Measurement")   { Output.ask(queue) }
    val addTransfer = addButton.addItem("Transfer Measurement") { Transfer.ask(queue) }
    val addSync     = addButton.addItem("Synced Voltage Measurement") { Sync.ask(queue) }
    val addFPP      = addButton.addItem("FPP Conductivity Measurement") { FPP.ask(queue) }

    init { addButton.addSeparator() }

    val addVHold    = addButton.addItem("Hold Voltage")         { Hold.ask(queue) }
    val addTChange  = addButton.addItem("Temperature Change")   { TemperatureChange.ask(queue) }

    init { addButton.addSeparator() }

    val addTSweep   = addButton.addItem("Temperature Sweep")    { TemperatureSweep.ask(queue) }
    val addRepeat   = addButton.addItem("Repeat")               { Repeat.ask(queue) }

    val clearQueue  = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

}