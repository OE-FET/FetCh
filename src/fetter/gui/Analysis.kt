package fetter.gui

import fetter.analysis.FETMeasurement
import fetter.analysis.MeasurementCluster
import jisa.enums.Icon
import jisa.gui.*
import java.io.File

object Analysis : Grid("Analysis", 1) {

    private val cluster = MeasurementCluster()
    private val plotter = Fields("Create Series")
    private val name    = plotter.addTextField("Name")
    private val xCol    = plotter.addChoice("X-Axis", 2, *cluster.getCombined(false, false).names)
    private val yCol    = plotter.addChoice("Y-Axis", 7, *cluster.getCombined(false, false).names)

    init {
        plotter.addSeparator()
    }

    private val sDo = plotter.addCheckBox("Split", false)
    private val sCol = plotter.addChoice("Split By", 1, *cluster.getCombined(false, false).names)

    init {
        plotter.addSeparator()
    }

    private val output = plotter.addCheckBox("Use Output Data", true)
    private val transfer = plotter.addCheckBox("Use Transfer Data", true)

    init {
        plotter.addSeparator()
    }

    private val lines = plotter.addCheckBox("Draw Lines", true)

    private val grid = Grid(2)

    init {

        setIcon(Icon.PLOT)
        setGrowth(true, false)
        grid.setGrowth(true, false)

        sDo.setOnChange { sCol.isDisabled = !sDo.get() }
        sCol.isDisabled = !sDo.get()

        addToolbarButton("New Plot") {

            val response = GUI.inputWindow("New Plot", "Plot Title", "Please enter a title for the plot", "Title")
            val plot = Plot(response[0])

            plot.setPointOrdering(Plot.Sort.ORDER_ADDED)
            plot.useMouseCommands(true)
            plot.addToolbarButton("Remove") { grid.remove(plot) }
            plot.addToolbarSeparator()
            plot.addSaveButton("Save")
            plot.addToolbarSeparator()
            plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
            plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }
            plot.addToolbarSeparator()

            plot.addToolbarButton("Add Series") {

                if (plotter.showAndWait()) {

                    val progress = Progress("Plotting")
                    progress.setTitle("Plotting Data")
                    progress.setStatus("Please Wait...")
                    progress.setProgress(-1.0)

                    progress.show()
                    plot.isVisible = false

                    val results = cluster.getCombined(output.get(), transfer.get())

                    val series = plot.createSeries().watch(results, xCol.get(), yCol.get())

                    if (lines.get()) {
                        series.showMarkers(false)
                    } else {
                        series.setMarkerShape(Series.Shape.DOT).setMarkerSize(2.0).showLine(false)
                    }

                    if (sDo.get()) series.split(sCol.get(), "${name.get()} %s ${results.getUnits(sCol.get())}") else series.setName(name.get())

                    plot.isVisible = true
                    progress.close()

                }

            }

            grid.add(plot)
            scrollToEnd()

        }

        addAll(grid)

        addToolbarButton("Add Measurement") {

            val file = GUI.openFileSelect()

            if (file != null && file.endsWith("-info.txt")) {
                val fileObject = File(file)
                cluster.add(FETMeasurement(fileObject.name.removeSuffix("-info.txt"), fileObject.parent))
            }

        }

    }

}