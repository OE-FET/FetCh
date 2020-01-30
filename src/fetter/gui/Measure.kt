package fetter.gui

import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
import jisa.Util
import jisa.devices.TC
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.gui.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

object Measure : Grid("Measurement", 1) {

    val basic    = Fields("Data Output Settings").apply { getPane().style = "-fx-background-color: white;" }
    val bSection = Section(basic.title, basic)
    val name     = basic.addTextField("Name")
    val dir      = basic.addDirectorySelect("Output Directory")
    val sep1     = basic.addSeparator()

    val length     = basic.addDoubleField("Channel Length [m]")
    val width      = basic.addDoubleField("Channel Width [m]")
    val thick      = basic.addDoubleField("Channel Thickness [m]")
    val sep2       = basic.addSeparator()
    val makeTables = basic.addCheckBox("Display Tables", true)
    val makePlots  = basic.addCheckBox("Display Plots", true)
    val start      = basic.addButton("Start Measurement", this::runMeasurement)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)

    val grid = Grid(1)
    val measurements   = HashMap<String, Measurement>()
    val generatedPlots = HashMap<String, Plot>()
    var runThread      = Thread.currentThread()

    init {

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(bSection, grid)

        basic.loadFromConfig("measure-basic", Settings)

    }

    private fun runMeasurement() {

        runThread = Thread.currentThread()

        // Reset everything
        grid.clear()
        generatedPlots.clear()
        measurements.clear()
        disable(true)

        // Get the configured instruments
        val instruments = Configuration.getInstruments()

        try {

            // Get file path and name to use, combine into single path String
            val fileName = name.get()
            val fileDir = dir.get()
            val path = Util.joinPath(fileDir, fileName)

            // Make sure something was actually written in those fields
            if (fileName == "" || fileDir == "") {
                throw Exception("You must specify an output name and directory.")
            }

            // Output size information to file
            File("$path-info.txt").printWriter().run {
                printf("Name: %s%n", name.get())
                printf("Length: %e%n", length.get())
                printf("Width: %e%n", width.get())
                printf("Thickness: %e%n", thick.get())
                close()
            }

            // Check which measurements we are doing, pre-configure them
            if (Transfer.isEnabled) measurements["Transfer"] = Transfer.getMeasurement(instruments)
            if (Output.isEnabled)   measurements["Output"]   = Output.getMeasurement(instruments)

            // If we're controlling temperature, then we will need to loop over all temperature set-points
            if (Temperature.isEnabled) {

                if (!instruments.hasTC) {
                    throw Exception("Temperature dependent measurements require a temperature controller.")
                }

                temperatureSweep(instruments.tc!!, fileName, path)

            } else {

                singleMeasurement(fileName, path)

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

    private fun temperatureSweep(tc: TC, fileName: String, path: String) {

        val stabPerc = Temperature.stabPerc.get()
        val stabTime = (Temperature.stabTime.get() * 1000).toLong()

        for (T in Temperature.values) {

            val plots = Grid(2)
            val section = Section("%s K".format(T), plots)

            plots.addAll(TempChangeNotice, TempChangePlot)

            TempChangePlot.start(T, tc)

            grid.add(section)

            try {

                // Change temperature and wait for stability
                tc.targetTemperature = T
                tc.useAutoHeater()
                tc.waitForStableTemperature(T, stabPerc, stabTime)

            } finally {

                TempChangePlot.stop()
                grid.remove(section)

            }

            singleMeasurement(T, fileName, path)

        }


    }

    private fun singleMeasurement(fileName: String, path: String) = singleMeasurement(-1.0, fileName, path)

    private fun singleMeasurement(T: Double, fileName: String, path: String) {

        val cols      = (if (makeTables.get()) 1 else 0) + (if(makePlots.get()) 1 else 0)
        val container = Grid(cols)

        grid.add(Section(if (T > -1) "$T K" else "No Temperature Control", container))

        for ((name, measurement) in measurements) {

            // Create results file
            val pattern = if (T > -1) "%s-%sK-%s".format(path, T, name) else "%s-%s".format(path, name)
            val results = measurement.newResults("$pattern.csv")

            // Create table, plot and series
            val plot  = Plot("$name Curve")
            val drain = plot.createSeries().showMarkers(false)
            val gate  = plot.createSeries().showMarkers(false).setLineDash(Series.Dash.DOTTED)

            // Which type of measurement are we doing (need to plot different columns depending on which)
            when (measurement) {

                is OutputMeasurement   -> {

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
            plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
            plot.yLabel = "Current [A]"
            plot.useMouseCommands(true)
            plot.addSaveButton("Save")
            plot.addToolbarSeparator()
            plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
            plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }

            if (makeTables.get()) {
                container.add(Table("$name Data", results))
            }

            if (makePlots.get()) {
                container.add(plot)
            }

            generatedPlots["$fileName-$name.svg"] = plot

            try {
                measurement.start()
            } finally {
                measurement.results.finalise()
            }


        }

    }

    private fun disable(flag: Boolean) {

        bSection.isExpanded = !flag
        start.isDisabled = flag
        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled = !flag

        basic.setFieldsDisabled(flag)
        Temperature.disable(flag)
        Transfer.disable(flag)
        Output.disable(flag)

    }

    private fun stopMeasurement() {

        // Stop all and any measurements that might be running
        for ((_, measurement) in measurements) {
            measurement.stop()
        }

        runThread.interrupt()

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