package org.oefet.fetch.gui.tabs

import jisa.control.Connection
import jisa.devices.meter.ILM200
import jisa.enums.Icon
import jisa.gui.*
import jisa.gui.form.Field
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import jisa.results.RowEvaluable
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.Log

object Dashboard : Grid("Dashboard", 3) {

    private val plots   = ArrayList<Element>()
    private val shown   = ArrayList<Boolean>()
    private val logged  = ArrayList<Boolean>()

    init {

        numColumns = Settings.dashboard.intValue("columns").getOrDefault(if (Settings.wide) 3 else 1)

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

        addToolbarButton("Visible") {
            editVisible()
        }

        addToolbarButton("Logged") {
            editLogged()
        }

        addToolbarButton("Interval") {

            if (!Log.isRunning) {

                val input    = Form("Change Logging Interval")
                val interval = input.addTimeField("Interval", Log.interval)

                if (input.showAsConfirmation()) {
                    Log.interval = interval.value
                }

            }

        }

        addToolbarButton("Columns") {

            val input = Form("Change Column Count")
            val cols  = input.addIntegerField("Columns", numColumns)

            if (input.showAsConfirmation()) {
                numColumns = cols.value
                Settings.dashboard.intValue("columns").set(cols.value)
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

        val time = log.getColumn(0) as Column<Number>

        val tEval = if (time.units == "UTC ms") {
            RowEvaluable { it[time].toDouble() / 1e3 }
        } else {
            RowEvaluable { it[time].toDouble() }
        }

        for (i in 1 until log.columnCount) {

            val col = log.getColumn(i) as Column<Double>

            val plot = FetChPlot(col.name, "Time", col.title)

            plot.isLegendVisible = false
            plot.xAxisType       = Plot.AxisType.TIME

            plot.createSeries()
                .watch(log, tEval, { it[col] })
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

        val input = Form("Visible Plots")
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

        val input = Form("Logged Values")
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

        val time  = log.getColumn(0) as Column<Number>
        val isUTC = time.units == "UTC ms"
        val tEval = if (isUTC) {
            RowEvaluable { it[time].toDouble() / 1e3 }
        } else {
            RowEvaluable { it[time].toDouble() }
        }

        for (i in 1 until log.columnCount) {

            val col  = log.getColumn(i) as Column<Double>
            val plot = FetChPlot(col.name, "Time", col.title)

            plot.isLegendVisible = false

            if (isUTC) {
                plot.xAxisType = Plot.AxisType.TIME
                plot.xUnit     = null
            } else {
                plot.xAxisType = Plot.AxisType.LINEAR
                plot.xUnit     = "s"
            }

            plot.createSeries()
                .watch(log, tEval, { it[col] })
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