package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.FMeasurement

object Dashboard : Grid("Dashboard", 3) {

    init {

        setIcon(Icon.DASHBOARD)

        addToolbarButton("Open Log File") {

            val path = GUI.openFileSelect()

            if (path != null) {
                openLogFile(ResultList.loadFile(path))
            }

        }

    }

    fun watchLog(log: ResultTable) {

        clear()

        for (i in 1 until log.numCols) {

            val plot = FetChPlot(log.getName(i), "Time [s]", log.getTitle(i))

            plot.isLegendVisible = false

            plot.createSeries()
                .watch(log, 0, i)
                .setMarkerVisible(false)
                .setLineVisible(true)
                .setColour(Series.defaultColours[(i-1) % Series.defaultColours.size])

            add(plot)

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