package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.FMeasurement
import java.util.*
import kotlin.collections.ArrayList

object Dashboard : Grid("Dashboard", 3) {

    private val plots = ArrayList<Plot>()
    private val shown = ArrayList<Boolean>()

    init {

        setIcon(Icon.DASHBOARD)
        setGrowth(true, false)

        addToolbarButton("Open Log File") {

            val path = GUI.openFileSelect()

            if (path != null) {
                openLogFile(ResultList.loadFile(path))
            }

        }

        addToolbarButton("Configure Visible Plots") {
            editVisible()
        }

    }

    fun watchLog(log: ResultTable) {

        clear()
        plots.clear()
        shown.clear()

        for (i in 1 until log.numCols) {

            val plot = FetChPlot(log.getName(i), "Time [s]", log.getTitle(i))

            plot.isLegendVisible = false

            plot.createSeries()
                .watch(log, 0, i)
                .setMarkerVisible(false)
                .setLineVisible(true)
                .setColour(Series.defaultColours[(i-1) % Series.defaultColours.size])

            plots.add(plot)
            shown.add(true)

            add(plot)

        }

    }

    fun editVisible() {

        val input = Fields("Visible Plots")
        val grid  = Grid("Visible Plots", input)
        val ticks = ArrayList<Field<Boolean>>()

        for ((i, plot) in plots.withIndex()) {
            ticks.add(input.addCheckBox(plot.title, shown[i]))
        }

        grid.windowHeight = 500.0
        grid.windowWidth  = 350.0

        grid.addToolbarButton("Select All") {
            ticks.forEach { it.value = true }
        }

        grid.addToolbarButton("Deselect All") {
            ticks.forEach { it.value = false }
        }

        grid.addToolbarButton("Toggle All") {
            ticks.forEach { it.value = !it.value }
        }

        if (grid.showAsConfirmation()) {

            clear()

            for ((i, field) in ticks.withIndex()) {
                shown[i] = field.value

                if (field.value) {
                    add(plots[i])
                }

            }

        }

    }

    fun openLogFile(log: ResultTable) {

        val prog = Progress("Loading File")

        prog.title  = "Loading File"
        prog.status = "Plotting..."
        prog.setProgress(0, log.numCols-1)

        prog.show()

        val grid = Grid("Log File", 3)

        for (i in 1 until log.numCols) {

            val plot = FetChPlot(log.getName(i), "Time [s]", log.getTitle(i))

            plot.isLegendVisible = false

            plot.createSeries()
                .watch(log, 0, i)
                .setMarkerVisible(false)
                .setLineVisible(true)
                .setColour(Series.defaultColours[(i-1) % Series.defaultColours.size])

            grid.add(plot)

            prog.incrementProgress()

        }

        prog.close()
        grid.isMaximised = true
        grid.show()

    }

}