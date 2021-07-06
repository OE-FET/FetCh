package org.oefet.fetch.gui.elements

import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.MeasurementAction
import jisa.gui.GUI
import jisa.gui.Grid
import jisa.gui.MeasurementConfigurator
import jisa.gui.queue.ActionQueueDisplay
import org.oefet.fetch.Actions
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.Sweeps
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.gui.tabs.FileLoad
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FetChMeasurement
import org.oefet.fetch.sweep.FetChSweep

class FetChQueue(name: String, private val queue: ActionQueue) : ActionQueueDisplay(name, queue) {

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

    private val upButton = addToolbarButton("▲") {

        val selected = selectedIndices.sorted()

        for (index in selected) {
            if (index > 0) queue.swapActions(index, index - 1)
        }

    }

    private val dnButton = addToolbarButton("▼") {

        val selected = selectedIndices.sortedDescending()

        for (index in selected) {
            if (index < queue.size() - 1) queue.swapActions(index, index + 1)
        }

    }

    private val rmButton = addToolbarButton("✕") {

        for (action in selectedActions) {
            queue.removeAction(action)
        }

    }

    init {
        addToolbarSeparator()
    }

    /**
     * Button for removing all actions from the queue
     */
    private val clearQueue = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

    init {
        addToolbarSeparator()
    }

    private val displayType = addToolbarMenuButton("Display").apply {
        addItem("Expand All") { setExpanded(true) }
        addItem("Collapse All") { setExpanded(false) }
    }


    var isDisabled: Boolean
        get() {
            return addButton.isDisabled
        }

        set(disabled) {
            addButton.isDisabled  = disabled
            clearQueue.isDisabled = disabled
            upButton.isDisabled   = disabled
            dnButton.isDisabled   = disabled
            rmButton.isDisabled   = disabled
        }

    private fun askMeasurement(measurement: FetChMeasurement) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            maxWindowHeight = 700.0
            linkToConfig(Settings.inputs)
        }

        input.addAll(measurement.getExtraTabs())

        if (input.showInput()) {

            val action = queue.addAction(MeasurementAction(measurement))

            action.setFileNameGenerator { params, label -> "${Measure.baseFile}-$params-$label.csv" }
            if (action is MeasurementAction) action.setOnMeasurementStart { Measure.display(it) }

            action.setOnFinish {
                FileLoad.addData(it.data)
                System.gc()
            }

            action.setOnEdit {
                input.showInput()
                it.name = "${measurement.name} (${measurement.label})"
            }

        }

    }

    private fun askAction(measurement: FetChAction) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            maxWindowHeight = 700.0
            linkToConfig(Settings.inputs)
        }

        input.addAll(measurement.getExtraTabs())

        if (input.showInput()) {

            val action = queue.addAction(MeasurementAction(measurement))

            action.setOnMeasurementStart { Measure.display(it) }
            action.setOnMeasurementFinish { System.gc() }
            action.setOnEdit {
                input.showInput()
                it.name = "${measurement.name} (${measurement.label})"
            }

        }

    }

    private fun <T> askSweep(measurement: FetChSweep<T>) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            linkToConfig(Settings.inputs)
        }

        input.addAll(measurement.getExtraTabs())

        val sweepQueue = FetChQueue("Interval Actions", measurement.queue)

        val grid = Grid(measurement.name, 2, input, sweepQueue).apply {
            maxWindowHeight = 700.0
        }

        if (grid.showAsConfirmation()) {

            input.update()
            measurement.loadInstruments()

            val multiAction = measurement.createSweepAction()
            measurement.loadInstruments()
            multiAction.setSweepValues(measurement.getValues())
            multiAction.addActions(measurement.queue.actions)

            multiAction.setOnEdit {

                measurement.queue.clear()
                measurement.queue.addActions(multiAction.actions)

                if (grid.showAsConfirmation()) {

                    input.update()
                    measurement.loadInstruments()
                    multiAction.clearActions()
                    multiAction.addActions(measurement.queue.actions)
                    multiAction.setSweepValues(measurement.getValues())

                    multiAction.children.forEach {
                        if (it is MeasurementAction) it.setOnMeasurementStart { Measure.display(it) }
                    }

                }

            }

            multiAction.children.forEach {
                if (it is MeasurementAction) it.setOnMeasurementStart { Measure.display(it) }
            }

            queue.addAction(multiAction)

        }

    }

}