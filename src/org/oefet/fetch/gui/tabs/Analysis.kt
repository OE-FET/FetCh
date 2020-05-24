package org.oefet.fetch.gui.tabs

import javafx.scene.image.Image
import jisa.Util
import jisa.enums.Icon
import jisa.gui.*
import org.oefet.fetch.analysis.*
import org.oefet.fetch.analysis.Analysis
import kotlin.reflect.KClass
import kotlin.streams.toList

object Analysis : BorderDisplay("Analysis") {

    val sidebar       = ListDisplay<Analysis>("Available Analyses")
    val analyseButton = sidebar.addToolbarButton("Analyse") { analyse() }
    val saveButton    = sidebar.addToolbarMenuButton("Save...").apply {
        addItem("Plots...") { savePlots() }
        addItem("Tables...") { saveTables() }
        addItem("Plots and Tables...") { save() }
    }

    var output: Analysis.Output? = null;

    init {

        setIcon(Icon.PLOT)

        sidebar.add(
            AutoAnalysis,
            "Automatic Analysis",
            "Automatically determines what to plot",
            Icon.LIGHTBULB.blackImage
        )

        sidebar.add(
            TemperatureAnalysis,
            "Temperature Only",
            "Plot everything against temperature with no splitting",
            Icon.THERMOMETER.blackImage
        )

        sidebar.select(0)

        leftElement = sidebar

    }

    private fun analyse() {

        System.gc()

        try {

            val quantities = FileLoad.getQuantities()
            val names      = FileLoad.getNames()
            val labels     = mapOf<KClass<out Quantity>, Map<Double, String>>(Device::class to names)
            val analysis   = sidebar.selected.getObject()

            val plots  = Grid("Plots", 2)
            val tables = Grid("Tables", 2)

            plots.setGrowth(true, false)
            tables.setGrowth(true, true)

            val window = Tabs("Analysis", plots, tables)

            val progress = Progress("Analysing")
            progress.title = "Analysing"
            progress.setStatus("Analysing and plotting loaded results...")
            progress.setProgress(-1.0)

            centreElement = Grid(progress)

            val output  = analysis.analyse(quantities, labels)
            this.output = output

            plots.addAll(output.plots)
            tables.addAll(output.tables.stream().map{ Table(it.quantity.name, it.table) }.toList())

            centreElement = window

            progress.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        System.gc()

    }

    private fun save() {

        if (output == null) return

        val saveInput  = Fields("Save Parameters")
        val plotWidth  = saveInput.addIntegerField("Plot Width", 600)
        val plotHeight = saveInput.addIntegerField("Plot Height", 500)
        val directory  = saveInput.addDirectorySelect("Directory")

        if (!Grid("Save Data", 1, saveInput).showAsConfirmation()) return

        val dir    = directory.get()
        val width  = plotWidth.get().toDouble()
        val height = plotHeight.get().toDouble()

        saveTables(dir)
        savePlots(dir, width, height)

    }

    private fun savePlots(path: String? = null, width: Double = 800.0, height: Double = 600.0) {

        val output = this.output ?: return
        val dir: String
        val w: Double
        val h: Double

        if (path == null) {

            val saveInput  = Fields("Save Parameters")
            val plotWidth  = saveInput.addIntegerField("Plot Width", 600)
            val plotHeight = saveInput.addIntegerField("Plot Height", 500)
            val directory  = saveInput.addDirectorySelect("Directory")

            if (!Grid("Save Data", 1, saveInput).showAsConfirmation()) return

            dir    = directory.get()
            w      = plotWidth.get().toDouble()
            h      = plotHeight.get().toDouble()

        } else {
            dir = path
            w   = width
            h   = height
        }

        output.plots.forEach { it.saveSVG(Util.joinPath(dir, "${it.title.toLowerCase().replace(" ", "-")}.svg"), w, h) }

    }

    private fun saveTables(path: String? = null) {

        val output = this.output ?: return
        val dir    = path ?: GUI.directorySelect() ?: return

        output.tables.forEach { it.table.output(Util.joinPath(dir, "${it.quantity.name.toLowerCase().replace(" ", "-")}.csv")) }

    }


}