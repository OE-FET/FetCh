package fetter.gui

import fetter.analysis.FETMeasurement
import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
import jisa.Util
import jisa.devices.TC
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.*
import java.io.File
import java.lang.Exception
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.reflect.KClass

object Measure : Grid("Measurement", 1) {

    val basic        = Fields("Data Output Settings").apply { getPane().style = "-fx-background-color: white;" }
    val bSection     = Section(basic.title, basic)
    val name         = basic.addTextField("Name")
    val dir          = basic.addDirectorySelect("Output Directory")

    init { basic.addSeparator() }

    val length       = basic.addDoubleField("Channel Length [m]")
    val width        = basic.addDoubleField("Channel Width [m]")
    val thick        = basic.addDoubleField("Channel Thickness [m]")

    init { basic.addSeparator() }

    val makeTables   = basic.addCheckBox("Display Tables", true)
    val makePlots    = basic.addCheckBox("Display Plots", true)
    val start        = basic.addButton("Start Measurement", this::runMeasurement)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)

    init { addToolbarSeparator() }

    val loadButton   = addToolbarButton("Load Previous Measurement", this::loadMeasurement)

    val grid         = Grid(1)
    val measurements = HashMap<String, Measurement>()
    var runThread    = Thread.currentThread()

    init {

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(bSection, grid)

        basic.loadFromConfig("measure-basic", Settings)

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

            val temperature  = data.temperature
            val sectionName  = if(temperature > -1) "$temperature K" else "No Temperature Control"
            val cols         = (if (makeTables.get()) 1 else 0) + (if (makePlots.get()) 1 else 0)
            val container    = Grid(cols)

            if (data.output != null) {
                if (makeTables.get()) container.add(Table("Output Data", data.output))
                if (makePlots.get())  container.add(makePlot("Output", OutputMeasurement::class, data.output))
            }

            if (data.transfer != null) {
                if (makeTables.get()) container.add(Table("Transfer Data", data.transfer))
                if (makePlots.get())  container.add(makePlot("Transfer", TransferMeasurement::class, data.transfer))
            }

            grid.add(Section(sectionName, container))

        }

        progress.close()

    }

    private fun runMeasurement() {

        runThread = Thread.currentThread()

        // Reset everything
        grid.clear()
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
                tc.waitForStableTemperature(T, Temperature.stabilityPercentage, Temperature.stabilityTime)

            } finally {

                TempChangePlot.stop()
                grid.remove(section)

            }

            singleMeasurement(T, fileName, path)

        }


    }

    private fun singleMeasurement(fileName: String, path: String) = singleMeasurement(-1.0, fileName, path)

    private fun makePlot(name: String, measurement: KClass<out Measurement>, results: ResultTable) : Plot {

        // Create table, plot and series
        val plot  = Plot("$name Curve")
        val drain = plot.createSeries().showMarkers(false)
        val gate  = plot.createSeries().showMarkers(false).setLineDash(Series.Dash.DOTTED)

        // Which type of measurement are we doing (need to plot different columns depending on which)
        when (measurement) {

            OutputMeasurement::class -> {

                drain.watch(results, { row -> row[SD_VOLTAGE] }, { row -> abs(row[SD_CURRENT]) })
                    .split(SET_SG, "D (SG: %s V)")

                gate.watch(results, { row -> row[SD_VOLTAGE] }, { row -> abs(row[SG_CURRENT]) })
                    .split(SET_SG, "G (SG: %s V)")

                plot.setYAxisType(Plot.AxisType.LINEAR)
                plot.xLabel = "SD Voltage [V]"

            }

            TransferMeasurement::class -> {

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
        plot.setAutoMode()
        plot.useMouseCommands(true)
        plot.addSaveButton("Save")
        plot.addToolbarSeparator()
        plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
        plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }

        return plot

    }

    private fun singleMeasurement(T: Double, fileName: String, path: String) {

        val cols = (if (makeTables.get()) 1 else 0) + (if (makePlots.get()) 1 else 0)
        val container = Grid(cols)

        grid.add(Section(if (T > -1) "$T K" else "No Temperature Control", container))

        for ((name, measurement) in measurements) {

            // Create results file
            val pattern = if (T > -1) "%s-%sK-%s".format(path, T, name) else "%s-%s".format(path, name)
            val results = measurement.newResults("$pattern.csv")

            if (makeTables.get()) container.add(Table("$name Data", results))
            if (makePlots.get())  container.add(makePlot(name, measurement::class, results))

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

}