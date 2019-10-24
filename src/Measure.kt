import jisa.Util
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.gui.*
import java.io.File
import java.io.PrintStream
import java.lang.Exception
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashMap

class Measure(val mainWindow: MainWindow) : Grid("Measurement", 1) {

    val basic = Fields("Data Output Settings")
    val name  = basic.addTextField("Name")
    val dir   = basic.addDirectorySelect("Output Directory")
    val sep   = basic.addSeparator()

    val length = basic.addDoubleField("Channel Length [m]")
    val width  = basic.addDoubleField("Channel Width [m]")
    val thick  = basic.addDoubleField("Channel Thickness [m]")

    val start        = basic.addButton("Start Measurement", this::runMeasurement)
    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop  = addToolbarButton("Stop", this::stopMeasurement)
    val sep1         = addToolbarSeparator()
    val linScale     = addToolbarButton("Linear Scale") { setScale(Plot.AxisType.LINEAR) }
    val logScale     = addToolbarButton("Logarithmic Scale") { setScale(Plot.AxisType.LOGARITHMIC) }
    val sep2         = addToolbarSeparator()
    val savePlots    = addToolbarButton("Save Plots", this::writePlots)

    val grid  = Grid(1)
    val measurements   = HashMap<String, Measurement>()
    val generatedPlots = HashMap<String, Plot>()
    var axisScale      = Plot.AxisType.LOGARITHMIC

    init {
        toolbarStop.isDisabled = true
        setGrowth(true, false)
        setIcon(Icon.FLASK)
        addAll(basic, grid)
        basic.loadFromConfig("measure-basic", mainWindow.config)
    }

    fun runMeasurement() {

        grid.clear()
        generatedPlots.clear()
        measurements.clear()
        disable(true)

        try {

            val fileName = name.get()
            val fileDir  = dir.get()
            val path     = Paths.get(fileDir, fileName).toString()

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
            val doOutput = mainWindow.output.enabled.get()
            val doTransfer = mainWindow.transfer.enabled.get()

            // Get the configured instruments
            val sdSMU = mainWindow.configuration.sourceDrain.get()
            val sgSMU = mainWindow.configuration.sourceGate.get()
            val fpp1 = mainWindow.configuration.fourPP1.get()
            val fpp2 = mainWindow.configuration.fourPP2.get()
            val tc = mainWindow.configuration.tControl.get()
            val tm = mainWindow.configuration.tMeter.get()

            // If we are to use temperature set-points we need a temperature controller
            if (doTemperature && tc == null) {
                throw Exception("Temperature dependent measurements require a temperature controller.")
            }

            if (sdSMU == null || sgSMU == null) {
                throw Exception("Source-Drain and Source-Gate channels must be configured.")
            }

            if (doOutput) {

                val output = OutputMeasurement(sdSMU, sgSMU, fpp1, fpp2, tm)

                output.configureSD(
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

                measurements["Output"] = output

            }

            if (doTransfer) {

                val transfer = TransferMeasurement(sdSMU, sgSMU, fpp1, fpp2, tm)

                transfer.configureSD(
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

                measurements["Transfer"] = transfer

            }

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

                    grid.add(section)

                    tc.targetTemperature = T
                    tc.waitForStableTemperature(T, stabPerc, stabTime)

                    for ((name, measurement) in measurements) {

                        val results = measurement.newResults("%s-%sK-%s.csv".format(path, T, name))

                        if (measurement is TransferMeasurement) {

                            val plot = Plot("Transfer Curve", "Source-Drain Voltage [V]", "Drain Current [A]")

                            plot.createSeries().watch(results, 2, 3).split(1, "S-G: %s V").showMarkers(false)
                            plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
                            plot.setYAxisType(axisScale)
                            plot.useMouseCommands(true)
                            plots.add(plot)

                            generatedPlots["%s-%sK-%s.svg".format(fileName, T, name)] = plot

                        } else if (measurement is OutputMeasurement) {

                            val plot = Plot("Output Curve", "Source-Gate Voltage [V]", "Drain Current [A]")

                            plot.createSeries().watch(results, 4, 3).split(0, "S-D: %s V").showMarkers(false)
                            plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
                            plot.setYAxisType(axisScale)
                            plot.useMouseCommands(true)
                            plots.add(plot)

                            generatedPlots["%s-%sK-%s.svg".format(fileName, T, name)] = plot

                        }

                        measurement.performMeasurement()

                        if (measurement.wasStopped()) {
                            throw InterruptedException("Measurement Stopped");
                        }

                    }

                }

            } else {

                val plots   = Grid(2)
                val section = Section("No Temperature Control", plots)

                grid.add(section)

                for ((name, measurement) in measurements) {

                    val results = measurement.newResults("%s-%s.csv".format(path, name))

                    if (measurement is TransferMeasurement) {

                        val plot = Plot("Transfer Curve", "Source-Drain Voltage [V]", "Drain Current [A]")

                        plot.createSeries().watch(results, 2, 3).split(1, "S-G: %s V").showMarkers(false)
                        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
                        plot.setYAxisType(axisScale)
                        plot.useMouseCommands(true)
                        plots.add(plot)

                        generatedPlots["%s-%s.svg".format(fileName, name)] = plot

                    } else if (measurement is OutputMeasurement) {

                        val plot = Plot("Output Curve", "Source-Gate Voltage [V]", "Drain Current [A]")

                        plot.createSeries().watch(results, 4, 3).split(0, "S-D: %s V").showMarkers(false)
                        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
                        plot.setYAxisType(axisScale)
                        plot.useMouseCommands(true)
                        plots.add(plot)

                        generatedPlots["%s-%s.svg".format(fileName, name)] = plot

                    }

                    measurement.performMeasurement()

                    if (measurement.wasStopped()) {
                        throw InterruptedException("Measurement Stopped");
                    }

                }

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

    fun disable(flag: Boolean) {
        start.isDisabled = flag
        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled = !flag

        basic.setFieldsDisabled(flag)
        mainWindow.temperature.disable(flag)
        mainWindow.output.disable(flag)
        mainWindow.transfer.disable(flag)
    }

    fun stopMeasurement() {

        for ((name, measurement) in measurements) {
            if (measurement.isRunning) measurement.stop()
        }

    }

    fun writePlots() {

        val directory = GUI.directorySelect()

        if (directory != null) {

            val errors = LinkedList<String>()

            for ((name, plot) in generatedPlots) {

                try {
                    plot.saveSVG(Paths.get(directory, name).toString())
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

    fun setScale(scale: Plot.AxisType) {

        axisScale = scale

        for ((_, plot) in generatedPlots) {
            plot.setYAxisType(axisScale)
        }

    }

}