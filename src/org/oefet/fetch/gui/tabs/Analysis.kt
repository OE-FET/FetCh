package org.oefet.fetch.gui.tabs

import jisa.Util
import jisa.enums.Icon
import jisa.gui.*
import jisa.results.ResultTable
import org.oefet.fetch.Settings
import org.oefet.fetch.analysis.Analysis
import org.oefet.fetch.analysis.SpecificAnalysis
import org.oefet.fetch.quantities.*

object Analysis : BorderDisplay("Analysis") {

    val sidebar       = ListDisplay<Analysis>("Available Analyses")
    val analyseButton = sidebar.addToolbarButton("Analyse") { analyse() }
    val saveButton    = sidebar.addToolbarMenuButton("Save...").apply {
        addItem("Plots...") { savePlots() }
        addItem("Tables...") { saveTables() }
        addItem("Plots and Tables...") { save() }
    }

    var output: Analysis.Output? = null

    init {

        setIcon(Icon.PLOT)

        sidebar.add(
            SpecificAnalysis(),
            "Automatic Analysis",
            "Automatically determines what to plot",
            Icon.LIGHTBULB.blackImage
        )

        sidebar.add(
            SpecificAnalysis(Frequency::class, Temperature::class, Device::class, Gate::class, MaxConductivity::class, Voltage::class),
            "Hall Analysis",
            "Plot quantities in ways useful for Hall analysis",
            Icon.HALL.blackImage
        )

        sidebar.add(
            SpecificAnalysis(Temperature::class, Device::class),
            "Temperature Only",
            "Plot everything against temperature",
            Icon.THERMOMETER.blackImage
        )

        sidebar.add(
            SpecificAnalysis(Time::class, Device::class),
            "Time Only",
            "Plot everything against time",
            Icon.CLOCK.blackImage
        )

        sidebar.add(
            SpecificAnalysis(Repeat::class, Device::class),
            "Repeat Only",
            "Plot everything against repeat number",
            Icon.REPEAT.blackImage
        )

        sidebar.add(
            SpecificAnalysis(BField::class, Device::class),
            "Magnetic Field Only",
            "Plot everything against magnetic field",
            Icon.MAGNET.blackImage
        )

        sidebar.select(0)

        if (Settings.wide) {
            leftElement = sidebar
        } else {
            topElement  = sidebar
        }

    }

    private fun analyse() {

        try {

            val quantities = FileLoad.getQuantities()
            val names      = FileLoad.getNames()
            val analysis   = sidebar.selected.getObject()
            val plots      = Grid("Plots", if (Settings.wide) 2 else 1)
            val tables     = Grid("Tables", 1)

            plots.setGrowth(true, false)
            tables.setGrowth(true, true)

            val window = Tabs("Analysis", plots, tables)

            val progress      = Progress("Analysing")
            progress.title    = "Analysing"
            progress.status   = "Analysing and plotting loaded results..."
            progress.progress = -1.0

            centreElement = Grid(progress)

            val output  = analysis.analyse(quantities)
            this.output = output

            plots.addAll(output.plots)
            tables.addAll(output.tables.map {

                Table(it.quantity.name, it.table).apply {

                    addToolbarButton("Save") { saveTable(it.table) }
                    addToolbarSeparator()
                    addToolbarButton("⛶") {

                        Table(it.quantity.name, it.table).apply {
                            show()
                            Util.sleep(250)
                            isMaximised = true
                        }

                    }

                }

            })

            centreElement = window

            // Need to do this to get the scene to update for some reason
            GUI.runNow { centreElement.node.requestFocus() }

        } catch (e: Exception) {
            e.printStackTrace()
            GUI.errorAlert(e.message)
        }

        System.gc()

    }

    private fun saveTable(table: ResultTable) {

        val file = GUI.saveFileSelect() ?: return
        table.output(file)

    }

    private fun save() {

        if (output == null) return

        val saveInput  = Fields("Save Parameters")
        val plotFormat = saveInput.addChoice("Format", "svg", "png", "tex")
        val plotWidth  = saveInput.addIntegerField("Plot Width", 600)
        val plotHeight = saveInput.addIntegerField("Plot Height", 500)
        val directory  = saveInput.addDirectorySelect("Directory")

        if (!Grid("Save Data", 1, saveInput).showAsConfirmation()) return

        val dir    = directory.get()
        val width  = plotWidth.get().toDouble()
        val height = plotHeight.get().toDouble()
        val format = plotFormat.get()

        saveTables(dir)
        savePlots(dir, width, height, format)

    }

    private fun savePlots(path: String? = null, width: Double = 800.0, height: Double = 600.0, format: Int = 0) {

        val output = this.output ?: return
        val dir: String
        val w: Double
        val h: Double
        val f: Int

        if (path == null) {

            val saveInput  = Fields("Save Parameters")
            val plotFormat = saveInput.addChoice("Format", "svg", "png", "tex")
            val plotWidth  = saveInput.addIntegerField("Plot Width", 600)
            val plotHeight = saveInput.addIntegerField("Plot Height", 500)
            val directory  = saveInput.addDirectorySelect("Directory")

            if (!Grid("Save Data", 1, saveInput).showAsConfirmation()) return

            dir    = directory.value
            w      = plotWidth.value.toDouble()
            h      = plotHeight.value.toDouble()
            f      = plotFormat.value

        } else {
            dir = path
            w   = width
            h   = height
            f   = format
        }

        for (plot in output.plots) {

            when (f) {
                0 -> plot.saveSVG(Util.joinPath(dir, "${plot.title.toLowerCase().replace(" ", "-")}.svg"), w, h)
                1 -> plot.savePNG(Util.joinPath(dir, "${plot.title.toLowerCase().replace(" ", "-")}.png"), w, h)
                2 -> plot.saveTex(Util.joinPath(dir, "${plot.title.toLowerCase().replace(" ", "-")}.tex"))
            }

        }

    }

    private fun saveTables(path: String? = null) {

        val output = this.output ?: return
        val dir = path ?: GUI.directorySelect() ?: return

        for (table in output.tables) {
            table.table.output(Util.joinPath(dir, "${table.quantity.name.toLowerCase().replace(" ", "-")}.csv"))
        }

    }


}