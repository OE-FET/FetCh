package fetter.gui

import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
import javafx.scene.layout.Background
import jisa.Util
import jisa.control.RTask
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.gui.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

const val SET_SD     = 0
const val SET_SG     = 1
const val SD_VOLTAGE = 2
const val SD_CURRENT = 3
const val SG_VOLTAGE = 4
const val SG_CURRENT = 5

class Measure(private val mainWindow: MainWindow) : Grid("Measurement", 1) {

    val basic    = Fields("Data Output Settings").apply { getPane().style = "-fx-background-color: white;" }
    val bSection = Section(basic.title, basic)
    val name     = basic.addTextField("Name")
    val dir      = basic.addDirectorySelect("Output Directory")
    val sep      = basic.addSeparator()

    val length = basic.addDoubleField("Channel Length [m]")
    val width  = basic.addDoubleField("Channel Width [m]")
    val thick  = basic.addDoubleField("Channel Thickness [m]")

    val start        = basic.addButton("Start Measurement", this::runMeasurement)
    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)
    val savePlots    = addToolbarButton("Save Plots", this::writePlots)

    val grid  = Grid(1)
    val measurements   = HashMap<String, Measurement>()
    val generatedPlots = HashMap<String, Plot>()

    init {

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(bSection, grid)

        basic.loadFromConfig("measure-basic", mainWindow.config)

    }

    private fun runMeasurement() {

        // Reset everything
        grid.clear()
        generatedPlots.clear()
        measurements.clear()
        disable(true)

        // Get the configured instruments
        val gdSMU = mainWindow.configuration.ground.get()
        val sdSMU = mainWindow.configuration.sourceDrain.get()
        val sgSMU = mainWindow.configuration.sourceGate.get()
        val fpp1  = mainWindow.configuration.fourPP1.get()
        val fpp2  = mainWindow.configuration.fourPP2.get()
        val tc    = mainWindow.configuration.tControl.get()
        val tm    = mainWindow.configuration.tMeter.get()

        try {

            // Get file path and name to use, combine into single path String
            val fileName = name.get()
            val fileDir  = dir.get()
            val path     = Util.joinPath(fileDir, fileName)

            // Make sure something was actually written in those fields
            if (fileName == "" || fileDir == "") {
                throw Exception("You must specify an output name and directory.")
            }

            // Output size information to file
            val output   = File("$path-info.txt").printWriter()
            output.printf("Name: %s%n", name.get())
            output.printf("Length: %e%n", length.get())
            output.printf("Width: %e%n", width.get())
            output.printf("Thickness: %e%n", thick.get())
            output.close()

            // Get which measurements to do
            val doTemperature = mainWindow.temperature.enabled.get()
            val doTransfer    = mainWindow.transfer.enabled.get()
            val doOutput      = mainWindow.output.enabled.get()

            // If we are to use temperature set-points we need a temperature controller
            if (doTemperature && tc == null) {
                throw Exception("Temperature dependent measurements require a temperature controller.")
            }

            // Whatever we are doing, these two SMUs MUST be configured
            if (sdSMU == null || sgSMU == null) {
                throw Exception("Source-Drain and Source-Gate channels must be configured.")
            }

            // Check which measurements we are doing, pre-configure them
            if (doTransfer) {

                val tMeasure = TransferMeasurement(sdSMU, sgSMU, gdSMU, fpp1, fpp2, tm)

                tMeasure.configureSD(
                    mainWindow.transfer.minSDV.get(),
                    mainWindow.transfer.maxSDV.get(),
                    mainWindow.transfer.numSDV.get(),
                    mainWindow.transfer.symSDV.get()
                ).configureSG(
                    mainWindow.transfer.minSGV.get(),
                    mainWindow.transfer.maxSGV.get(),
                    mainWindow.transfer.numSGV.get(),
                    mainWindow.transfer.symSGV.get()
                ).configureTimes(
                    mainWindow.transfer.intTime.get(),
                    mainWindow.transfer.delTime.get()
                )

                measurements["Transfer"] = tMeasure

            }

            if (doOutput) {

                val oMeasure = OutputMeasurement(sdSMU, sgSMU, gdSMU, fpp1, fpp2, tm)

                oMeasure.configureSD(
                    mainWindow.output.minSDV.get(),
                    mainWindow.output.maxSDV.get(),
                    mainWindow.output.numSDV.get(),
                    mainWindow.output.symSDV.get()
                ).configureSG(
                    mainWindow.output.minSGV.get(),
                    mainWindow.output.maxSGV.get(),
                    mainWindow.output.numSGV.get(),
                    mainWindow.output.symSGV.get()
                ).configureTimes(
                    mainWindow.output.intTime.get(),
                    mainWindow.output.delTime.get()
                )

                measurements["Output"] = oMeasure

            }

            // If we're controlling temperature, then we will need to loop over all temperature set-points
            if (doTemperature) {

                val temperatures = Util.makeLinearArray(
                    mainWindow.temperature.minT.get(),
                    mainWindow.temperature.maxT.get(),
                    mainWindow.temperature.numT.get()
                )

                val stabPerc = mainWindow.temperature.stabPerc.get()
                val stabTime = (mainWindow.temperature.stabTime.get() * 1000).toLong()

                for (T in temperatures) {

                    val plots   = Grid(2)
                    val section = Section("%s K".format(T), plots)

                    val stabNotice = Doc("Temperature Stabilising");

                    stabNotice.addHeading("Temperature Change in Progress")
                        .setAlignment(Doc.Align.CENTRE)
                    stabNotice.addText("Waiting for the temperature controller to report a stable temperature within range of the current set-point before continuing...")
                        .setAlignment(Doc.Align.CENTRE)

                    val stabPlot   = Plot("Temperature", "Time [s]", "Temperature [K]")
                    val tempSeries = stabPlot.createSeries().setName("Temperature").showMarkers(false).setColour(Colour.BLUE)
                    val targSeries = stabPlot.createSeries().setName("Set-Point").showMarkers(false).setColour(Colour.CORNFLOWERBLUE).setLineDash(Series.Dash.DOTTED)

                    val task = RTask(1000) { task ->

                        tempSeries.addPoint(task.secFromStart, tc.temperature)
                        targSeries.addPoint(task.secFromStart, T)

                    }

                    task.start()

                    plots.addAll(stabNotice, stabPlot)

                    grid.add(section)

                    // Change temperature and wait for stability
                    tc.targetTemperature = T
                    tc.useAutoHeater()
                    tc.waitForStableTemperature(T, stabPerc, stabTime)
                    task.stop()
                    plots.clear()
                    singleMeasurement(T, plots, fileName, path)

                }

            } else {

                val plots   = Grid(2)
                val section = Section("No Temperature Control", plots)

                grid.add(section)

                singleMeasurement(-1.0, plots, fileName, path)

            }

            GUI.infoAlert("Measurement Complete", "The measurement completed without error.")

        } catch (e: InterruptedException) {
            GUI.warningAlert("Measurement Stopped", "The measurement was stopped before completion.")
        } catch (e: Exception) {
            GUI.errorAlert("Measurement Error", "Measurement Error", e.message, 600.0)
        } finally {
            disable(false)
        }

    }

    private fun singleMeasurement(T: Double, plots: Container, fileName: String, path: String) {

        for ((name, measurement) in measurements) {

            val pattern = if (T > -1) "%s-%sK-%s".format(path, T, name) else  "%s-%s".format(path, name)
            val results = measurement.newResults("$pattern.csv")
            val table   = Table("$name Data", results)
            val plot    = Plot("$name Curve")
            val drain    = plot.createSeries().showMarkers(false)
            val gate    = plot.createSeries().showMarkers(false).setLineDash(Series.Dash.DOTTED)

            // Which type of measurement are we doing (need to plot different columns depending on which)
            when (measurement) {

                is OutputMeasurement -> {

                    drain.watch(results, { row -> row[SD_VOLTAGE] }, { row -> abs(row[SD_CURRENT]) })
                          .split(SET_SG, "D (SG: %s V)")

                    gate.watch(results, { row -> row[SD_VOLTAGE] }, { row -> abs(row[SG_CURRENT]) })
                        .split(SET_SG, "G (SG: %s V)")

                    plot.setYAxisType(Plot.AxisType.LINEAR)
                    plot.xLabel = "SD Voltage [V]"

                }

                is TransferMeasurement -> {

                    drain.watch(results, { row -> row[SG_VOLTAGE] }, { row -> abs(row[SD_CURRENT]) })
                          .split(SET_SD, "D (SD: %s V)")

                    gate.watch(results, { row -> row[SG_VOLTAGE] }, { row -> abs(row[SG_CURRENT]) })
                        .split(SET_SD, "G (SD: %s V)")

                    plot.setYAxisType(Plot.AxisType.LOGARITHMIC)
                    plot.xLabel = "SG Voltage [V]"

                }

            }

            // Make sure to add points in the order they are plotted (rather than sorting by x-value)
            plot.yLabel = "Current [A]"
            plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
            plot.useMouseCommands(true)
            plots.addAll(plot)

            generatedPlots["$fileName-$name.svg"] = plot

            try {
                measurement.start()
            } finally {
                measurement.results.finalise()
            }


        }

    }

    private fun disable(flag: Boolean) {

        bSection.isExpanded     = !flag
        start.isDisabled        = flag
        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled  = !flag

        basic.setFieldsDisabled(flag)
        mainWindow.temperature.disable(flag)
        mainWindow.transfer.disable(flag)
        mainWindow.output.disable(flag)

    }

    private fun stopMeasurement() {

        // Stop all and any measurements that might be running
        for ((_, measurement) in measurements) {
            measurement.stop()
        }

    }

    private fun writePlots() {

        val directory = GUI.directorySelect()

        if (directory != null) {

            val errors = LinkedList<String>()

            for ((name, plot) in generatedPlots) {

                try {
                    plot.saveSVG(Util.joinPath(directory, name))
                } catch (e: Exception) {
                    errors.add(e.message ?: "Unknown Error")
                }

            }

            if (errors.isEmpty()) {
                GUI.infoAlert("Plots saved.")
            } else {
                GUI.errorAlert("Some plot(s) failed to save:\n\n" + errors.joinToString("\n\n"))
            }

        }

    }

}