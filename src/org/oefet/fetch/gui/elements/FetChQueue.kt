package org.oefet.fetch.gui.elements

import jisa.experiment.ActionQueue
import jisa.gui.ActionQueueDisplay
import jisa.gui.GUI
import jisa.gui.MeasurementConfigurator
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.inputs.ActionInput
import org.oefet.fetch.gui.inputs.SweepInput
import org.oefet.fetch.gui.tabs.FileLoad
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FMeasurement

class FetChQueue(name: String, private val queue: ActionQueue) : ActionQueueDisplay(name, queue) {

    init {

        setOnDoubleClick {

            if (it is ActionQueue.MeasureAction) {

                val measurement = it.measurement

                val input = MeasurementConfigurator(measurement.name, measurement).apply {
                    windowHeight = 750.0
                    windowWidth  = 1024.0
                }

                input.showInput()

                it.name = measurement.label

            }

        }
    }

    /**
     * Button for adding actions to the queue
     */
    private val addButton = addToolbarMenuButton("Add...").apply {

        addItem("Measurements:") {}.apply { isDisabled = true }
        addSeparator()

        for (type in Measurements.types) addItem(type.name) { askMeasurement(type.createMeasurement()) }

        addSeparator()
        addItem("Actions:") {}.apply { isDisabled = true }
        addSeparator()

        for (type in ActionInput.types) addItem(type.name) { type.create().ask(queue) }

        addSeparator()
        addItem("Sweeps:") {}.apply { isDisabled = true }
        addSeparator()

        for (type in SweepInput.types) addItem(type.name) { type.create().ask(queue) }

    }

    /**
     * Button for removing all actions from the queue
     */
    private val clearQueue = addToolbarButton("Clear") {
        if (GUI.confirmWindow("Clear Queue", "Clear Queue", "Are you sure?")) queue.clear()
    }

    /**
     * Whether the buttons for adding/clearing actions are enabled or not
     */
    var isDisabled: Boolean
        get() = addButton.isDisabled
        set(value) {
            addButton.isDisabled = value
            clearQueue.isDisabled = value
        }


    private fun askMeasurement(measurement: FMeasurement) {

        // Generate measurement parameter input GUI and make it remember values from last time
        val input = MeasurementConfigurator(measurement.name, measurement).apply {
            linkConfig(Settings.inputs)
            windowHeight = 750.0
            windowWidth  = 1024.0
        }

        if (input.showInput()) {

            val action = queue.addMeasurement(measurement.label, measurement)

            action.setAttribute("Type", measurement.type)
            action.setResultsPath { "${Measure.baseFile}-%s-${measurement.label}.csv" }

            action.setBefore { Measure.display(it) }

            action.setAfter { FileLoad.addData(it.data) }

        }

    }

}