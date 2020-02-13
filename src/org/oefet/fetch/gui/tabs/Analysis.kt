package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.experiment.Combination
import jisa.experiment.ResultList
import jisa.gui.*
import org.oefet.fetch.analysis.Curve
import org.oefet.fetch.analysis.MeasurementFile
import org.oefet.fetch.analysis.OCurve
import org.oefet.fetch.analysis.OCurve.Companion.MOBILITY
import org.oefet.fetch.analysis.OCurve.Companion.SD_VOLTAGE
import org.oefet.fetch.analysis.OCurve.Companion.SG_VOLTAGE
import org.oefet.fetch.analysis.TCurve
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.elements.TransferPlot
import java.io.File
import java.util.*
import kotlin.collections.HashSet

object Analysis : Grid("Analysis", 1) {

    private val addFileButton = addToolbarButton("Add File...") { addFile() }
    private val addFolderButton = addToolbarButton("Add Folder...") { addFolder() }

    init {
        addToolbarSeparator()
    }

    private val clearButton = addToolbarButton("Clear") { clearMeasurements() }

    private val curves = LinkedList<Curve>();
    private val dispGrid = Grid(1)
    private val sgPlotter = Fields("Mobility vs Gate Voltage")
    private val sgTitle = sgPlotter.addTextField("Title", "Mobility vs Gate Voltage")
    init { sgPlotter.addButton("Plot") { makeSGPlot() } }

    private val tPlotter = Fields("Mobility vs Temperature")
    private val tTitle = tPlotter.addTextField("Title", "Mobility vs Temperature")
    init { tPlotter.addButton("Plot") { makeTPlot() } }

    init {
        setGrowth(true, false)
        setIcon(Icon.PLOT)
        addAll(Grid(3, sgPlotter, tPlotter), dispGrid)
    }

    fun makeSGPlot() {

        val plot     = Plot(sgTitle.get(), "SG Voltage [V]", "Mobility [cm^2/Vs]")
        val plotData = ResultList("SG Voltage [V]", "Mobility [cm^2/Vs]", "SD Voltage [V]", "Temperature [K]")

        val series = plot.createSeries()
            .watch(plotData, 0, 1)
            .showMarkers(false)

        val set = HashSet<Double>()
        for (curve in curves) set.add(curve.temperature)

        if (set.size > 1) {
            series.split({ Combination(it[2], it[3]) }, { "SD = ${it[2]} V\tT = ${it[3]} K" })
        } else {
            series.split(2, "SD = %s V")
        }

        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)

        plot.addSaveButton("Save Plot")
        plot.addToolbarButton("Save Data") {
            val file = GUI.saveFileSelect()
            if (file != null) plotData.output(file)
        }

        plot.addToolbarSeparator()
        plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
        plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }

        plot.useMouseCommands(true)

        plot.show()

        for (curve in curves) {

            for (mob in arrayOf(curve.fwdMob.flippedCopy(), curve.bwdMob)) {

                for (row in mob) plotData.addData(row[SG_VOLTAGE], row[MOBILITY], row[SD_VOLTAGE], curve.temperature)

            }

        }


    }

    fun makeTPlot() {

        val plot     = Plot(tTitle.get(), "Temperature [K]", "Max Mobility [cm^2/Vs]")
        val plotData = ResultList("Temperature [K]", "Max Mobility [cm^2/Vs]", "SD Voltage [V]")

        plot.createSeries()
            .watch(plotData, 0, 1)
            .split(2 , "SD = %s V")
            .showMarkers(false)

        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)

        plot.addSaveButton("Save Plot")
        plot.addToolbarButton("Save Data") {
            val file = GUI.saveFileSelect()
            if (file != null) plotData.output(file)
        }

        plot.addToolbarSeparator()
        plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
        plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }

        plot.useMouseCommands(true)

        plot.show()

        val tempData = ResultList("SG", "SD", "M")
        for (curve in curves) {

            for (mob in arrayOf(curve.fwdMob.flippedCopy(), curve.bwdMob)) {

                for (row in mob) tempData.addData(row[SG_VOLTAGE], row[SD_VOLTAGE], row[MOBILITY])

            }

            for ((drain, data) in tempData.split(1)) plotData.addData(curve.temperature, data.getMax(2), drain)

        }


    }

    fun addFolder() {


        val prog = Progress("Loading")
        prog.title = "Loading Files"
        prog.setStatus("Please Wait...")
        prog.setProgress(-1.0)

        try {

            val folder = File(GUI.directorySelect() ?: return)

            prog.show()

            for (file in folder.listFiles().sorted()) {

                try {

                    val fileAttempt = MeasurementFile(file.absolutePath)

                    when (fileAttempt.getType()) {

                        MeasurementFile.CurveType.OUTPUT -> addOutput(fileAttempt.getOCurve())
                        MeasurementFile.CurveType.TRANSFER -> addTransfer(fileAttempt.getTCurve())
                        else -> throw Exception("Unable to determine measurement type")

                    }

                } catch (ignored: Exception) {
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
            prog.close()
            GUI.errorAlert(e.message)
        } finally {
            prog.close()
        }

    }

    fun clearMeasurements() {
        curves.clear()
        dispGrid.clear()
    }

    fun addFile() {

        try {

            val file = MeasurementFile(GUI.openFileSelect() ?: return)

            when (file.getType()) {

                MeasurementFile.CurveType.OUTPUT -> addOutput(file.getOCurve())
                MeasurementFile.CurveType.TRANSFER -> addTransfer(file.getTCurve())
                else -> throw Exception("Unable to determine measurement type")

            }

        } catch (e: Exception) {
            e.printStackTrace()
            GUI.errorAlert(e.message)
        }

    }

    private fun addOutput(curve: OCurve) {

        curves += curve

        val info = Display("Information")
        val temp = info.addParameter("Temperature", "${curve.temperature} K")
        val length = info.addParameter("Channel Length", "${curve.length} m")
        val width = info.addParameter("Channel Width", "${curve.length} m")
        val thick = info.addParameter("Dielectric Thickness", "${curve.thick} m")
        val perm = info.addParameter("Dielectric Permittivity", curve.permittivity)

        for ((name, value) in curve.variables) info.addParameter(name, value)

        val plot = OutputPlot(curve)
        val grid = Grid(1, Grid(2, info, plot), Table("Data", curve.data))
        val sect =
            Section("Output Measurement (${curve.name}) (${curve.variableString})", grid).apply { isExpanded = false }

        dispGrid.add(sect)

    }

    private fun addTransfer(curve: TCurve) {

        curves += curve

        val info = Display("Information")
        val temp = info.addParameter("Temperature", "${curve.temperature} K")
        val length = info.addParameter("Channel Length", "${curve.length} m")
        val width = info.addParameter("Channel Width", "${curve.length} m")
        val thick = info.addParameter("Dielectric Thickness", "${curve.thick} m")
        val perm = info.addParameter("Dielectric Permittivity", curve.permittivity)

        for ((name, value) in curve.variables) info.addParameter(name, value)

        val plot = TransferPlot(curve)
        val grid = Grid(1, Grid(2, info, plot), Table("Data", curve.data))
        val sect =
            Section("Transfer Measurement (${curve.name}) (${curve.variableString})", grid).apply { isExpanded = false }

        dispGrid.add(sect)

    }


}