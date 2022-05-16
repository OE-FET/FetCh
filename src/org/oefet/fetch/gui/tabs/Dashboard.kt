package org.oefet.fetch.gui.tabs

import jisa.control.Connection
import jisa.devices.level.ILM200
import jisa.enums.Icon
import jisa.gui.*
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.Log

object Dashboard : Grid("Dashboard", 3) {

    private val plots  = ArrayList<Element>()
    private val shown  = ArrayList<Boolean>()
    private val logged = ArrayList<Boolean>()

    init {

        setIcon(Icon.DASHBOARD)
        setGrowth(true, false)

        addToolbarButton("Start Logging") {

            val path = GUI.saveFileSelect()

            if (path != null) {
                Log.start(path)
            }

        }

        addToolbarButton("Stop Logging") {
            Log.stop()
        }


        addToolbarSeparator()

        addToolbarButton("Configure Visible Plots") {
            editVisible()
        }

        addToolbarButton("Configure Logged Values") {
            editLogged()
        }

        addToolbarButton("Interval") {

            if (!Log.isRunning) {

                val input    = Fields("Change Logging Interval")
                val interval = input.addIntegerField("Interval [ms]", Log.interval)

                if (input.showAsConfirmation()) {
                    Log.interval = interval.value
                }

            }

        }

        addToolbarSeparator()

        addToolbarButton("Open Log File") {

            val path = GUI.openFileSelect()

            if (path != null) {
                openLogFile(ResultList.loadFile(path))
            }

        }

    }

    fun isLogged(index: Int): Boolean {

        return try {
            logged[index]
        } catch (e: IndexOutOfBoundsException) {
            true
        }

    }

    fun watchLog(log: ResultTable) {

        clear()
        plots.clear()
        shown.clear()
        logged.clear()

        val time = log.getColumn(0) as Column<Double>

        for (i in 1 until log.columnCount) {

            val col = log.getColumn(i) as Column<Double>

            val plot = FetChPlot(col.name, "Time [s]", col.title)

            plot.isLegendVisible = false

            plot.createSeries()
                .watch(log, time, col)
                .setMarkerVisible(false)
                .setLineVisible(true)
                .setColour(Series.defaultColours[(i - 1) % Series.defaultColours.size])
                .setAutoReduction(500, 1000)

            if (col.name.contains("ILM200")) {

                plot.addToolbarMenuButton("Sample Rate").apply {
                    addItem("Fast") { (Connection.getConnectionsOf(ILM200::class.java).first()?.instrument as ILM200).setFastRate(0, true)  }
                    addItem("Slow") { (Connection.getConnectionsOf(ILM200::class.java).first()?.instrument as ILM200).setFastRate(0, false) }
                }

            }

            val show = !Settings.dashboard.hasValue(plot.title) || Settings.dashboard.booleanValue(plot.title).get()
            val log  = !Settings.logged.hasValue(plot.title)    || Settings.logged.booleanValue(plot.title).get()

            plots.add(plot)
            shown.add(show)
            logged.add(log)

            if (show && log) add(plot)

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
            Settings.dashboard.clear()

            for ((i, field) in ticks.withIndex()) {

                shown[i] = field.value

                Settings.dashboard.booleanValue(plots[i].title).set(field.value)

                if (field.value && logged[i]) {
                    add(plots[i])
                }

            }

            Settings.dashboard.save()

        }

    }

    fun editLogged() {

        val input = Fields("Logged Values")
        val grid  = Grid("Logged Values", input)
        val ticks = ArrayList<Field<Boolean>>()

        for ((i, plot) in plots.withIndex()) {
            ticks.add(input.addCheckBox(plot.title, logged[i]))
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
            Settings.logged.clear()

            for ((i, field) in ticks.withIndex()) {

                logged[i] = field.value

                Settings.logged.booleanValue(plots[i].title).set(field.value)

                if (field.value && shown[i]) {
                    add(plots[i])
                }

            }

            Settings.logged.save()

        }

    }

    fun openLogFile(log: ResultTable) {

        val prog = Progress("Loading File")

        prog.title = "Loading File"
        prog.status = "Plotting..."
        prog.setProgress(0, log.columnCount - 1)

        prog.show()

        val grid = Grid("Log File", 3)

        val time = log.getColumn(0) as Column<Double>

        for (i in 1 until log.columnCount) {

            val col  = log.getColumn(i) as Column<Double>
            val plot = FetChPlot(col.name, "Time [s]", col.title)

            plot.isLegendVisible = false

            plot.createSeries()
                .watch(log, time, col)
                .setMarkerVisible(false)
                .setLineVisible(true).colour = Series.defaultColours[(i - 1) % Series.defaultColours.size]

            grid.add(plot)

            prog.incrementProgress()

        }

        prog.close()
        grid.isMaximised = true
        grid.show()

    }

}