package org.oefet.fetch.gui.tabs

import org.oefet.fetch.analysis.MeasurementFile
import org.oefet.fetch.analysis.OCurve
import org.oefet.fetch.analysis.TCurve
import org.oefet.fetch.gui.tabs.Measure.addToolbarButton
import org.oefet.fetch.measurement.OutputMeasurement
import org.oefet.fetch.measurement.TransferMeasurement
import jisa.Util
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.*
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.elements.TransferPlot

object Measure : Grid("Measurement", 1) {

    val queue     = ActionQueue()
    val queueList = FetChQueue("Measurements", queue)
    val basic     = Fields("Data Output Settings")
    val cSection  = Section("Current Measurement")
    val name      = basic.addTextField("Name")
    val dir       = basic.addDirectorySelect("Output Directory")

    init { basic.addSeparator() }

    val length     = basic.addDoubleField("Channel Length [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val thick      = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", "CYTOP", "PMMA", "Other")
    val dielConst  = basic.addDoubleField("Dielectric Constant", 1.0)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)

    val baseFile: String get() = Util.joinPath(dir.get(), name.get())

    init {

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(Grid(2,
            basic,
            queueList
        ), cSection
        )

        basic.linkConfig(Settings.measureBasic)

        dielectric.setOnChange(this::setDielectric)
        setDielectric()

    }

    fun display(action: ActionQueue.MeasureAction) {

        action.setAttribute("name", name.get())
        action.setAttribute("length", "${length.get()} m")
        action.setAttribute("width", "${width.get()} m")
        action.setAttribute("dielectricThickness", "${thick.get()} m")
        action.setAttribute("dielectricPermittivity", dielConst.get())

        val table = Table("Data", action.data)


        val plot = when (action.measurement) {

            is OutputMeasurement -> OutputPlot(OCurve(action.data))

            is TransferMeasurement -> TransferPlot(TCurve(action.data))

            else -> Plot("Unknown")

        }

        cSection.title = action.name
        cSection.setElement(Grid(2, table, plot))

    }

    private fun setDielectric() {

        when (dielectric.get()) {

            0 -> {
                dielConst.isDisabled = true
                dielConst.set(2.05)
            }

            1 -> {
                dielConst.isDisabled = true
                dielConst.set(2.22)
            }

            2 -> {
                dielConst.isDisabled = false
            }

        }

    }

    private fun runMeasurement() {

        if (queue.size < 1) {
            GUI.errorAlert("Measurement sequence is empty!")
            return
        }

        disable(true)
        Results.clear()

        when (queue.start()) {

            ActionQueue.Result.COMPLETED   -> GUI.infoAlert("Measurement sequence completed successfully")
            ActionQueue.Result.INTERRUPTED -> GUI.warningAlert("Measurement sequence was stopped before completion")
            ActionQueue.Result.ERROR       -> GUI.errorAlert("Measurement sequence completed with error(s)")
            else                           -> GUI.errorAlert("Unknown queue result")

        }



        System.gc()

        disable(false)

    }


    private fun disable(flag: Boolean) {

        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled = !flag

        basic.setFieldsDisabled(flag)

        setDielectric()

    }

    private fun stopMeasurement() {
        queue.stop()
    }

}