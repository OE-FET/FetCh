package org.oefet.fetch.gui.elements

import jisa.gui.Plot

open class FetChPlot(title: String, xLabel: String = "", yLabel: String = "") : Plot(title, xLabel, yLabel) {

    init {

        useMouseCommands(true)

        addSaveButton("Save")
        addToolbarSeparator()

        addToolbarMenuButton("Scaling").apply {
            addItem("Linear") { setYAxisType(Plot.AxisType.LINEAR) }
            addItem("Logarithmic") { setYAxisType(Plot.AxisType.LOGARITHMIC) }
        }

        addToolbarMenuButton("Display").apply {
            addItem("Markers Only") { series.forEach { it.showLine(false).showMarkers(true) } }
            addItem("Lines Only") { series.forEach { it.showLine(true).showMarkers(false) } }
            addItem("Markers and Lines") { series.forEach { it.showLine(true).showMarkers(true) } }
        }

    }

}