package org.oefet.fetch.gui.tabs

import org.oefet.fetch.gui.tabs.Measure.addToolbarButton
import jisa.Util
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.ActionQueue.Result.*
import jisa.experiment.ResultStream
import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.*
import org.oefet.fetch.measurement.*

object Measure : Grid("Measurement", 1) {

    val queue     = ActionQueue()
    val queueList = FetChQueue("Measurements", queue)
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

    }

    fun display(action: ActionQueue.MeasureAction) {

        action.setAttribute("Name", name.get())
        action.setAttribute("Length", "${length.value} m")
        action.setAttribute("FPP Separation", "${fppLength.value} m")
        action.setAttribute("Width", "${width.value} m")
        action.setAttribute("Thickness", "${cThick.value} m")
        action.setAttribute("Dielectric Thickness", "${dThick.value} m")
        action.setAttribute("Dielectric Permittivity", dielConst.value)

        val table = Table("Data", action.data)
        val plot  = Measurements.createPlot(action.measurement) ?: FetChPlot("Unknown Measurement Plot", "X", "Y")

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
                dielConst.value = 2.05
            }

            1 -> {
                dielConst.isDisabled = true
                dielConst.value = 2.22
            }

            2 -> {
                dielConst.isDisabled = false
            }

        }

    }

    private fun runMeasurement() {

        Log.start("${baseFile}-${System.currentTimeMillis()}-log.csv")

        if (queue.size < 1) {
            GUI.errorAlert("Measurement sequence is empty!")
            return
        }

        disable(true)

        try {

            val result = queue.start()
            Log.stop()

            when (result) {

                COMPLETED   -> GUI.infoAlert("Measurement sequence completed successfully")
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
        queueList.isDisabled    =  flag

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