package org.oefet.fetch.gui.tabs

import org.oefet.fetch.analysisold.OCurve
import org.oefet.fetch.analysisold.TCurve
import org.oefet.fetch.gui.tabs.Measure.addToolbarButton
import org.oefet.fetch.measurement.OutputMeasurement
import org.oefet.fetch.measurement.TransferMeasurement
import jisa.Util
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.*
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.*
import org.oefet.fetch.measurement.FPPMeasurement
import org.oefet.fetch.measurement.SyncMeasurement

object Measure : Grid("Measurement", 2) {

    val queue     = ActionQueue()
    val queueList = FetChQueue("Measurements", queue)
    val basic     = Fields("Measurement Parameters")
    val cSection  = Section("Current Measurement")
    val name      = basic.addTextField("Name")
    val dir       = basic.addDirectorySelect("Output Directory")

    init { basic.addSeparator() }

    val length     = basic.addDoubleField("Channel Length [m]")
    val fppLength  = basic.addDoubleField("FPP Separation [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val cThick     = basic.addDoubleField("Channel Thickness [m]")
    val dThick     = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", "CYTOP", "PMMA", "Other")
    val dielConst  = basic.addDoubleField("Dielectric Constant", 1.0)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)

    val baseFile: String get() = Util.joinPath(dir.get(), name.get())

    var table: Table? = null
    var plot: Plot? = null

    init {

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(basic, queueList)

        basic.linkConfig(Settings.measureBasic)

        dielectric.setOnChange(this::setDielectric)
        setDielectric()

    }

    fun display(action: ActionQueue.MeasureAction) {

        action.setAttribute("Name", name.get())
        action.setAttribute("Length", "${length.get()} m")
        action.setAttribute("FPP Separation", "${fppLength.get()} m")
        action.setAttribute("Width", "${width.get()} m")
        action.setAttribute("Thickness", "${cThick.get()} m")
        action.setAttribute("Dielectric Thickness", "${dThick.get()} m")
        action.setAttribute("Dielectric Permittivity", dielConst.get())

        val table = Table("Data", action.data)


        val plot = when (action.measurement) {

            is OutputMeasurement   -> OutputPlot(OCurve(action.data))
            is TransferMeasurement -> TransferPlot(TCurve(action.data))
            is FPPMeasurement      -> FPPPlot(action.data)
            is SyncMeasurement     -> SyncPlot(action.data)
            else                   -> Plot("Unknown")

        }

        removeAll(this.table, this.plot)
        this.table = table
        this.plot  = plot
        addAll(table, plot)

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
        if (!flag) setDielectric()

    }

    private fun stopMeasurement() {
        queue.stop()
    }

}