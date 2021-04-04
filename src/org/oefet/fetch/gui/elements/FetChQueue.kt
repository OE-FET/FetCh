package org.oefet.fetch.gui.elements

import jisa.experiment.ActionQueue
import jisa.gui.ActionQueueDisplay
import jisa.gui.GUI
import jisa.gui.Grid
import jisa.gui.MeasurementConfigurator
import org.oefet.fetch.Actions
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.Sweeps
import org.oefet.fetch.action.Action
import org.oefet.fetch.gui.tabs.FileLoad
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FMeasurement
import org.oefet.fetch.sweep.Sweep

class FetChQueue(name: String, private val queue: ActionQueue) : ActionQueueDisplay(name, queue) {

    init {

        setOnDoubleClick {

            if (!addButton.isDisabled) {

                when (it) {

                    is ActionQueue.MeasureAction -> {

                        val measurement = it.measurement

                        val input = MeasurementConfigurator(measurement.name, measurement).apply {
                            windowHeight = 750.0
                            windowWidth = 1024.0
                        }

                        if (input.showInput()) {
                            it.name = measurement.label
                        }

                    }

                }

            }

        }
    }

    /**
     * Button for adding actions to the queue
     */
    private val addButton = addToolbarMenuButton("Add...").apply {

        addSeparator("Measurements")

        for (type in Measurements.types) addItem(type.name) { askMeasurement(type.createMeasurement()) }

        addSeparator("Actions")

        for (type in Actions.types) addItem(type.name) { askAction(type.create()) }

        addSeparator("Sweeps")

        for (type in Sweeps.types) addItem(type.name) { askSweep(type.create()) }

    }

    init { addToolbarSeparator() }

    private val upButton = addToolbarButton("▲") {

        val indices = selectedIndices.sorted()

        for (index in indices) {

            if (index > 0) {
                queue.swapOrder(index, index - 1)
            }

        }

    }

    private val dnButton = addToolbarButton("▼") {

        val indices = selectedIndices.sortedDescending()

        for (index in indices) {

            if (index < queue.size - 1) {
                queue.swapOrder(index, index + 1)
            }

        }

    }

    init { addToolbarSeparator() }

    private val rmButton = addToolbarButton("X") {

        for (action in selectedActions) {
            queue.removeAction(action)
        }

    }

    init { addToolbarSeparator() }

    /**
     * Button for removing all actions from the queue
     */
    private val clearQueue = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

    /**
     * Whether the buttons for adding/clearing actions are enabled or not
     */
    override fun setDisabled(disabled: Boolean) {
        addButton.isDisabled  = disabled
        clearQueue.isDisabled = disabled
        upButton.isDisabled   = disabled
        dnButton.isDisabled   = disabled
        rmButton.isDisabled   = disabled
    }

    private fun askMeasurement(measurement: FMeasurement) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            linkToConfig(Settings.inputs)
            windowHeight = 750.0
            windowWidth  = 1024.0
        }

        if (input.showInput()) {

            val action = queue.addMeasurement(measurement.label, measurement)

            action.setResultsPath { "${Measure.baseFile}-%s-${measurement.label}.csv" }
            action.setBefore { Measure.display(it) }
            action.setAfter {
                it.data.finalise()
                FileLoad.addData(it.data)
                System.gc()
            }

        }

    }

    private fun askAction(measurement: Action) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            linkToConfig(Settings.inputs)
        }

        if (input.showInput()) {

            val action = queue.addMeasurement(measurement.label, measurement)

            action.setBefore { Measure.display(it) }

            action.setAfter { System.gc() }

        }

    }

    private fun askSweep(measurement: Sweep) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            linkToConfig(Settings.inputs)
        }

        val sweepQueue = FetChQueue("Interval Actions", measurement.queue)

        val grid  = Grid(measurement.name, 2, input, sweepQueue)

        if (grid.showAsConfirmation()) {

            input.update()

            val multiAction = ActionQueue.MultiAction(measurement.name)

            measurement.loadInstruments()

            for (action in measurement.generateActions()) {

                if (action is ActionQueue.MeasureAction) {

                    action.setBefore { Measure.display(it) }

                    if (action.measurement is FMeasurement) {

                        action.setResultsPath { "${Measure.baseFile}-%s-${measurement.label}.csv" }

                        action.setAfter {
                            it.data.finalise()
                            FileLoad.addData(it.data)
                            System.gc()
                        }

                    }

                }

                multiAction.actions += action

            }

            queue.addAction(multiAction)

            setOnDoubleClick(multiAction) {

                if (!addButton.isDisabled && grid.showAsConfirmation()) {

                    input.update()

                    multiAction.name = measurement.name
                    multiAction.actions.clear()

                    measurement.loadInstruments()

                    for (action in measurement.generateActions()) {

                        if (action is ActionQueue.MeasureAction) {

                            action.setBefore { Measure.display(it) }

                            if (action.measurement is FMeasurement) {

                                action.setResultsPath { "${Measure.baseFile}-%s-${measurement.label}.csv" }

                                action.setAfter {
                                    it.data.finalise()
                                    FileLoad.addData(it.data)
                                    System.gc()
                                }

                            }

                        }

                        multiAction.actions += action

                    }

                }

            }

        }

    }

}