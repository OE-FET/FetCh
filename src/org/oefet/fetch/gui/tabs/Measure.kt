package org.oefet.fetch.gui.tabs

import jisa.Util
import jisa.enums.Icon
import jisa.experiment.queue.ActionQueue.Result.*
import jisa.experiment.ResultTable
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.MeasurementAction
import jisa.gui.*
import org.oefet.fetch.FetChEntity
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.*
import org.oefet.fetch.gui.tabs.Measure.addToolbarButton
import org.oefet.fetch.measurement.*

object Measure : Grid("Measurement", 1) {

    val queue     = ActionQueue()
    val queueList = FetChQueue("Measurements", queue).apply { maxHeight = 500.0 }
    val bigQueue  = FetChQueue("Measurements", queue)
    val basic     = Fields("Measurement Parameters")
    val name      = basic.addTextField("Name")
    val dir       = basic.addDirectorySelect("Output Directory")
    val topRow    = SwapGrid("Top Row")
    val bottomRow = Grid(1)

    init { basic.addSeparator() }

    val length     = basic.addDoubleField("Channel Length [m]")
    val fppLength  = basic.addDoubleField("FPP Separation [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val cThick     = basic.addDoubleField("Channel Thickness [m]")
    val dThick     = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", "CYTOP", "PMMA", "Other")
    val dielConst  = basic.addDoubleField("Dielectric Constant", 1.0)

    val bigQueueButton: Button

    val toolbarStart = addToolbarButton("Start", ::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", ::stopMeasurement)

    val baseFile: String get() = Util.joinPath(dir.get(), name.get())
    var table:    Table?       = null
    var plot:     Plot?        = null

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

    fun display(action: MeasurementAction) {

        action.data.setAttribute("Name", name.get())
        action.data.setAttribute("Length", "${length.value} m")
        action.data.setAttribute("FPP Separation", "${fppLength.value} m")
        action.data.setAttribute("Width", "${width.value} m")
        action.data.setAttribute("Thickness", "${cThick.value} m")
        action.data.setAttribute("Dielectric Thickness", "${dThick.value} m")
        action.data.setAttribute("Dielectric Permittivity", dielConst.value)

        val table = Table("Data", action.data)
        val plot  = (action.measurement as FetChEntity).createPlot(action.data)

        topRow.remove(this.plot)
        bottomRow.remove(this.table)
        this.table = table
        this.plot  = plot
        topRow.add(plot, 1)
        bottomRow.add(table)

    }

    private fun setDielectric() {

        when (dielectric.get()) {

            0 -> {
                dielConst.isDisabled = true
                dielConst.value      = 2.05
            }

            1 -> {
                dielConst.isDisabled = true
                dielConst.value      = 2.22
            }

            2 -> {
                dielConst.isDisabled = false
            }

        }

    }

    private fun runMeasurement() {

        if (name.value.trim() == "") {
            GUI.errorAlert("Please enter a name for this measurement sequence.")
            return
        }

        if (dir.value.trim() == "") {
            GUI.errorAlert("Please select an output directory for this measurement sequence.")
            return
        }

        if (queue.actions.size < 1) {
            GUI.errorAlert("Measurement sequence is empty!")
            return
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

        try {

            val result = if (resume) queue.resume() else queue.start()

            Log.stop()

            when (result) {

                SUCCESS     -> GUI.infoAlert("Measurement sequence completed successfully")
                INTERRUPTED -> GUI.warningAlert("Measurement sequence was stopped before completion")
                ERROR       -> GUI.errorAlert("Measurement sequence completed with error(s)")
                else        -> GUI.errorAlert("Unknown queue result")

            }

        } catch (e: Exception) {

            e.printStackTrace()

        } finally {

            System.gc()
            disable(false)
            Log.stop()

        }

    }


    private fun disable(flag: Boolean) {

        toolbarStart.isDisabled =  flag
        toolbarStop.isDisabled  = !flag
        queueList.isDisabled    = flag
        bigQueue.isDisabled     = flag

        if (flag) {

            topRow.remove(plot)
            bottomRow.remove(table)

            plot  = Plot("Results")
            table = Table("Results")

            topRow.add(plot, 1)
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