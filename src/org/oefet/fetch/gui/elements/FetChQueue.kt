package org.oefet.fetch.gui.elements

import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.MeasurementAction
import jisa.experiment.queue.SweepAction
import jisa.gui.GUI
import jisa.gui.Grid
import jisa.gui.ListDisplay
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

    private val measurements = ListDisplay<Measurements.Config>("Add Measurement").apply { minHeight = 500.0; minWidth = 500.0; }
    private val actions      = ListDisplay<Actions.Config>("Add Action").apply { minHeight = 500.0; minWidth = 500.0; }
    private val sweeps       = ListDisplay<Sweeps.Config<*>>("Add Sweep").apply { minHeight = 500.0; minWidth = 500.0; }

    /**
     * Button for adding actions to the queue
     */
    private val addButton = addToolbarMenuButton("Add...").apply {

        updateTypes()

        addItem("Action...") {

            if (actions.showAsConfirmation()) {
                askAction(actions.selected.getObject().create())
            }

        }

        addItem("Measurement...") {

            if (!measurements.isShowing) {

                if (measurements.showAsConfirmation()) {
                    askMeasurement(measurements.selected.getObject().createMeasurement())
                }

            } else {
                measurements.show()
            }

        }

        addItem("Sweep...") {

            if (sweeps.showAsConfirmation()) {
                askSweep(sweeps.selected.getObject().create())
            }

        }

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

    @Synchronized
    fun updateTypes() {

        measurements.clear()
        actions.clear()
        sweeps.clear()

        for (type in Measurements.types.filter { !Settings.hidden.booleanValue(it.name).getOrDefault(false) }) measurements.add(type, type.name, type.mClass.simpleName, type.image)
        for (type in Actions.types.filter { !Settings.hidden.booleanValue(it.name).getOrDefault(false) }) actions.add(type, type.name, type.mClass.simpleName, type.image)
        for (type in Sweeps.types.filter { !Settings.hidden.booleanValue(it.name).getOrDefault(false) }) sweeps.add(type, type.name, type.mClass.simpleName, type.image)

    }

    private fun askMeasurement(measurement: FetChMeasurement) {

        try {

            // Generate measurement parameter input GUI and make it remember values from last time
            val input = MeasurementConfigurator(measurement.name, measurement).apply {
                maxWindowHeight = 700.0
                linkToConfig(Settings.inputs)
            }

            input.addAll(measurement.getExtraTabs())
            (input.elements.filterIsInstance<Grid>().first()).addAll(measurement.getCustomParams())

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

        } catch (e: Throwable) {
            e.printStackTrace()
            GUI.errorAlert(e.message)
        }

    }

    private fun askAction(measurement: FetChAction) {

        try {

            // Generate measurement parameter input GUI and make it remember values from last time
            val input = MeasurementConfigurator(measurement.name, measurement).apply {
                maxWindowHeight = 700.0
                linkToConfig(Settings.inputs)
            }

            input.addAll(measurement.getExtraTabs())
            (input.elements.first() as Grid).addAll(measurement.getCustomParams())

            if (input.showInput()) {

                val action = queue.addAction(MeasurementAction(measurement))

                action.setOnMeasurementStart { Measure.display(it) }
                action.setOnMeasurementFinish { System.gc() }
                action.setOnEdit {
                    input.showInput()
                    it.name = "${measurement.name} (${measurement.label})"
                }

            }

        } catch (e: Throwable) {
            e.printStackTrace()
            GUI.errorAlert(e.message)
        }

    }

    private fun <T> askSweep(measurement: FetChSweep<T>) {

        try {
            // Generate measurement parameter input GUI and make it remember values from last time
            val input = MeasurementConfigurator(measurement.name, measurement).apply {
                linkToConfig(Settings.inputs)
            }

            input.addAll(measurement.getExtraTabs())
            (input.elements.first() as Grid).addAll(measurement.getCustomParams())

            val sweepQueue = FetChQueue("Interval Actions", measurement.queue)

            val grid = Grid(measurement.name, 2, input, sweepQueue).apply {
                maxWindowHeight = 700.0
            }

            if (grid.showAsConfirmation()) {

                input.update()

                val multiAction = measurement.createSweepAction()
                multiAction.setSweepValues(measurement.getValues())
                multiAction.addActions(measurement.queue.actions)

                multiAction.setOnEdit {

                    measurement.queue.clear()
                    measurement.queue.addActions(multiAction.actions)

                    if (grid.showAsConfirmation()) {

                        input.update()
                        multiAction.clearActions()
                        multiAction.clearFinalActions()
                        multiAction.addActions(measurement.queue.actions)
                        multiAction.addFinalActions(measurement.generateFinalActions())
                        multiAction.setSweepValues(measurement.getValues())

                        setUpDisplay(multiAction)

                    }

                }

                setUpDisplay(multiAction)

                queue.addAction(multiAction)

            }

        } catch (e: Throwable) {
            e.printStackTrace()
            GUI.errorAlert(e.message)
        }

    }

    private fun setUpDisplay(action: SweepAction<*>) {

        action.children.forEach {
            if (it is MeasurementAction) it.setOnMeasurementStart { Measure.display(it) }
            if (it is SweepAction<*>) setUpDisplay(it)
        }

    }

}