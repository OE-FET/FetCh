package fetter.gui

import fetter.analysis.FETMeasurement
import fetter.analysis.OCurve
import fetter.analysis.TCurve
import fetter.measurement.Instruments
import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
import jisa.Util
import jisa.devices.TC
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.*
import java.io.File
import java.lang.Exception
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.reflect.KClass

object Measure : Grid("Measurement", 1) {

    val queue = ActionQueue()
    val queueList = ActionQueueDisplay("Measurements", queue)
    val basic = Fields("Data Output Settings")
    val bSection = Section(basic.title, Grid(2, basic, queueList))
    val cSection = Section("Current Measurement")
    val name = basic.addTextField("Name")
    val dir = basic.addDirectorySelect("Output Directory")

    val baseFile: String
        get() = Util.joinPath(dir.get(), name.get())

    init {
        basic.addSeparator()
    }

    val length     = basic.addDoubleField("Channel Length [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val thick      = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", "CYTOP", "PMMA", "Other")
    val dielConst  = basic.addDoubleField("Dielectric Constant", 1.0)

    init {
        basic.addSeparator()
    }

    val makeTables = basic.addCheckBox("Display Tables", true)
    val makePlots  = basic.addCheckBox("Display Plots", true)
    val start      = basic.addButton("Start Measurement", this::runMeasurement)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop = addToolbarButton("Stop", this::stopMeasurement)

    init {
        addToolbarSeparator()
    }

    val loadButton = addToolbarButton("Load Previous Measurement", this::loadMeasurement)

    val grid = Grid(1)
    val measurements = HashMap<String, Measurement>()

    init {

        queueList.addToolbarButton("Output") { Output.askForMeasurement(queue) }

        queueList.addToolbarButton("Transfer") { Transfer.askForMeasurement(queue) }

        queueList.addToolbarButton("Temperature") { Temperature.askForSingle(queue) }

        queueList.addToolbarButton("Temperature Sweep") { Temperature.askForSweep(queue) }

        queueList.addToolbarButton("Wait") { Time.askWait(queue) }

        queueList.addToolbarButton("Clear") { queue.clear() }

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        cSection.isVisible = false
        addAll(cSection, Grid(2, basic, queueList), grid)

        basic.loadFromConfig("measure-basic", Settings)

        dielectric.setOnChange(this::setDielectric)
        setDielectric()

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

        disable(true)

        when (queue.start()) {

            ActionQueue.Result.COMPLETED   -> GUI.infoAlert("Measurement sequence completed successfully")
            ActionQueue.Result.INTERRUPTED -> GUI.warningAlert("Measurement sequence was stopped before completion")
            ActionQueue.Result.ERROR       -> GUI.errorAlert("Error(s) were encountered during the measurement sequence")
            else                           -> {}

        }

        disable(false)

    }

    private fun loadMeasurement() {

        val file = File(GUI.openFileSelect() ?: return)

        if (!file.name.endsWith("-info.txt")) {
            GUI.errorAlert("That is not a measurement info file!\nPlease choose the *-info.txt file generated by the measurement you wish to load.")
            return
        }

        val progress = Progress("Loading")
        progress.setTitle("Loading Data")
        progress.setStatus("Please Wait...")
        progress.setProgress(-1.0)
        progress.show()
        bSection.isExpanded = false

        // Reset everything
        grid.clear()
        measurements.clear()

        for (data in FETMeasurement(file.name.removeSuffix("-info.txt"), file.parent)) {

            val temperature = data.temperature
            val sectionName = if (temperature > -1) "$temperature K" else "No Temperature Control"
            val cols = (if (makeTables.get()) 1 else 0) + (if (makePlots.get()) 1 else 0)
            val container = Grid(cols)

            if (data.output != null) {

                if (makeTables.get()) container.add(Table("Output Data", data.output.data))
                if (makePlots.get()) container.add(OutputPlot(data.output))

            }

            if (data.transfer != null) {

                if (makeTables.get()) container.add(Table("Transfer Data", data.transfer.data))
                if (makePlots.get()) container.add(TransferPlot(data.transfer))

            }

            grid.add(Section(sectionName, container))

        }

        progress.close()

    }


    private fun disable(flag: Boolean) {

        start.isDisabled = flag
        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled = !flag

        basic.setFieldsDisabled(flag)
        Temperature.disable(flag)
        Transfer.disable(flag)
        Output.disable(flag)

    }

    private fun stopMeasurement() {
        queue.stop()
    }

}