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
    val saveButton    = sidebar.addToolbarButton("Save Data") { save() }

    var output: Analysis.Output? = null;

    init {

        setIcon(Icon.PLOT)

        sidebar.add(AutoAnalysis, "Automatic Analysis", "Automatically determines what to plot", Util.invertImage(Image(Icon.LIGHTBULB.image.openStream())))
        sidebar.add(TemperatureAnalysis, "Temperature Only", "Plot everything against temperature with no splitting", Util.invertImage(Image(Icon.THERMOMETER.image.openStream())))
        sidebar.select(0)

        setLeft(sidebar)

    }

    private fun analyse() {

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

            val output  = analysis.analyse(quantities, labels)
            this.output = output

            plots.addAll(output.plots)
            tables.addAll(output.tables.stream().map{ Table(it.quantity.name, it.table) }.toList())

            setCentre(window)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun save() {

        if (output == null) return

        val output = this.output!!

        val saveInput  = Fields("Save Data")
        val plotWidth  = saveInput.addIntegerField("Plot Width", 600)
        val plotHeight = saveInput.addIntegerField("Plot Height", 500)
        val directory  = saveInput.addDirectorySelect("Directory")

        if (!saveInput.showAndWait()) return

        val dir   = directory.get()
        val width = plotWidth.get()
        val height = plotHeight.get()

        output.tables.forEach { it.table.output(Util.joinPath(dir, "${it.quantity.name.toLowerCase().replace(" ", "-")}.csv")) }
        output.plots.forEach { it.saveSVG(Util.joinPath(dir, "${it.title.toLowerCase().replace(" ", "-")}.svg"), width.toDouble(), height.toDouble()) }

        GUI.infoAlert("Data Saved.")


    }


}