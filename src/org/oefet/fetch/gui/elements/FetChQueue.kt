package org.oefet.fetch.gui.elements

import jisa.experiment.ActionQueue
import jisa.gui.*
import org.oefet.fetch.analysis.OCurve
import org.oefet.fetch.analysis.TCurve
import org.oefet.fetch.gui.inputs.*
import org.oefet.fetch.measurement.FPPMeasurement
import org.oefet.fetch.measurement.OutputMeasurement
import org.oefet.fetch.measurement.SyncMeasurement
import org.oefet.fetch.measurement.TransferMeasurement

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
    val addStress   = addButton.addItem("Stress")               { Stress.ask(queue) }
    val addRepeat   = addButton.addItem("Repeat")               { Repeat.ask(queue) }

    val clearQueue  = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

    init {

        setDataDisplay { action ->

            if (action is ActionQueue.MeasureAction) {

                val params = Display("Parameters")

                for ((param, value) in action.data.attributes) params.addParameter(param, value)

                val table  = Table("Data", action.data)

                val plot = when (action.measurement) {

                    is OutputMeasurement   -> OutputPlot(OCurve(action.data))
                    is TransferMeasurement -> TransferPlot(TCurve(action.data))
                    is FPPMeasurement      -> FPPPlot(action.data)
                    is SyncMeasurement     -> SyncPlot(action.data)
                    else                   -> Plot("Unknown")

                }

                Grid(1, Grid(2, params, plot), table)

            } else {

                null

            }

        }

    }

}