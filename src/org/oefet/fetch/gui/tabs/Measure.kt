package org.oefet.fetch.gui.tabs

import jisa.Util
import jisa.enums.Icon
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.ActionQueue.Result.*
import jisa.experiment.queue.MeasurementAction
import jisa.gui.*
import jisa.results.ResultTable
import org.oefet.fetch.*
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.measurement.Log
import java.util.*

object Measure : Grid("Measurement", 1) {

    val materials = mapOf(
        "CYTOP" to 2.05,
        "PMMA"  to 2.22,
        "SiO2"  to 3.9,
        "Other" to Double.NaN
    )

    val queue     = ActionQueue()
    val queueList = FetChQueue("Measurements", queue).apply { maxHeight = 500.0 }
    val bigQueue  = FetChQueue("Measurements", queue)
    val basic     = Fields("Measurement Parameters")
    val name      = basic.addTextField("Name")
    val dir       = basic.addDirectorySelect("Output Directory")
    val topRow    = SwapGrid("Top Row")
    val bottomRow = Grid(1)

    init {
        basic.addSeparator()
    }

    val length     = basic.addDoubleField("Channel Length [m]")
    val fppLength  = basic.addDoubleField("FPP Separation [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val cThick     = basic.addDoubleField("Channel Thickness [m]")
    val dThick     = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", *materials.keys.toTypedArray())
    val dielConst  = basic.addDoubleField("Dielectric Constant", 1.0)

    val bigQueueButton: Button

    val toolbarStart = addToolbarButton("Start", ::runMeasurement)
    val toolbarStop = addToolbarButton("Stop", ::stopMeasurement)

    init {
        addToolbarSeparator()
    }

    val hidden = addToolbarButton("Hidden Actions", ::editHidden)

    val baseFile: String get() = Util.joinPath(dir.value.trim(), name.value.trim())
    var table:    Table?       = null
    var element:  Element?     = null

    private var log: ResultTable? = null

    init {

        topRow.add(basic, 0)
        topRow.add(queueList, 0, 1)

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(topRow, bottomRow)

        basic.linkToConfig(Settings.measureBasic)

        dielectric.setOnChange(::setDielectric)
        setDielectric()

        queueList.addToolbarSeparator()

        bigQueueButton = queueList.addToolbarButton("⛶") {
            bigQueue.close()
            bigQueue.isMaximised = true
            bigQueue.show()
        }

    }

    fun editHidden() {

        val measurements = Fields("Hidden Measurements")
        val actions      = Fields("Hidden Actions")
        val sweeps       = Fields("Hidden Sweeps")

        for (type in Measurements.types) {
            measurements.addCheckBox(type.name, Settings.hidden.booleanValue(type.name).getOrDefault(false))
        }

        for (type in Actions.types) {
            actions.addCheckBox(type.name, Settings.hidden.booleanValue(type.name).getOrDefault(false))
        }

        for (type in Sweeps.types) {
            sweeps.addCheckBox(type.name, Settings.hidden.booleanValue(type.name).getOrDefault(false))
        }

        val grid = Grid("Configure Hidden Types", 3, measurements, actions, sweeps)

        if (grid.showAsConfirmation()) {

            for (field in measurements) {

                if (field.value is Boolean) {
                    Settings.hidden.booleanValue(field.text).set(field.value)
                }

            }

            for (field in actions) {

                if (field.value is Boolean) {
                    Settings.hidden.booleanValue(field.text).set(field.value)
                }

            }

            for (field in sweeps) {

                if (field.value is Boolean) {
                    Settings.hidden.booleanValue(field.text).set(field.value)
                }

            }


        }

    }

    fun display(action: MeasurementAction) {

        action.data.setAttribute("Name", name.get())
        action.data.setAttribute("Length", "${length.value} m")
        action.data.setAttribute("FPP Separation", "${fppLength.value} m")
        action.data.setAttribute("Width", "${width.value} m")
        action.data.setAttribute("Thickness", "${cThick.value} m")
        action.data.setAttribute("Dielectric Thickness", "${dThick.value} m")
        action.data.setAttribute("Dielectric Permittivity", dielConst.value)

        val table   = Table("Data", action.data)
        val element = (action.measurement as FetChEntity).createDisplay(action.data)

        topRow.remove(this.element)
        bottomRow.remove(this.table)
        this.table = table
        this.element = element
        topRow.add(element, 1)
        bottomRow.add(table)

    }

    private fun setDielectric() {

        val value = materials.values.toList()[dielectric.value]

        if (value.isFinite()) {
            dielConst.isDisabled = true
            dielConst.value      = value
        } else {
            dielConst.isDisabled = false
        }

    }

    private fun runMeasurement() {

        try {

            val errors = LinkedList<String>()

            if (name.value.isBlank()) {
                errors.add("Name is blank")
            }

            if (dir.value.isBlank()) {
                errors.add("Output directory is blank")
            }

            if (queue.actions.size < 1) {
                errors.add("Measurement sequence is empty")
            }

            if (errors.isNotEmpty()) {
                throw Exception("The following problem(s) occurred when trying to run:\n\n" + errors.joinToString("\n") { "- $it" })
            }

            queueList.setSelectedActions()

            val resume = if (queue.isInterrupted) {

                GUI.choiceWindow(
                    "Start Point Selection",
                    "Start Point Selection",
                    "The measurement sequence was previously interrupted.\n\nPlease select how you wish to proceed:",
                    "Start at the first item in the sequence",
                    "Start at the previously interrupted item"
                ) == 1

            } else {
                false
            }

            disable(true)

            Log.start("${baseFile}-${System.currentTimeMillis()}-log.csv")

            val result = if (resume) queue.resume() else queue.start()

            Log.stop()

            when (result) {

                SUCCESS     -> GUI.infoAlert("Measurement sequence completed successfully")
                INTERRUPTED -> GUI.warningAlert("Measurement sequence was stopped before completion")
                ERROR       -> GUI.errorAlert("Measurement sequence completed with error(s)")
                else        -> GUI.errorAlert("Unknown queue result")

            }

        } catch (e: Throwable) {

            e.printStackTrace()
            Log.stop()
            GUI.errorAlert(e.message)

        } finally {

            System.gc()
            disable(false)
            Log.stop()

        }

    }


    private fun disable(flag: Boolean) {

        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled  = !flag
        queueList.isDisabled    = flag
        bigQueue.isDisabled     = flag

        if (flag) {

            topRow.remove(element)
            bottomRow.remove(table)

            element = Plot("Results", "", "")
            table   = Table("Results")

            topRow.add(element, 1)
            bottomRow.add(table)
            topRow.configuration = 1

        } else {

            topRow.configuration = 0
            bottomRow.remove(table)

        }

        basic.setFieldsDisabled(flag)
        if (!flag) setDielectric()

    }

    private fun stopMeasurement() {
        queue.stop()
    }

}