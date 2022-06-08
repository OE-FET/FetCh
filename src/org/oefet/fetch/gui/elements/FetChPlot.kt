package org.oefet.fetch.gui.elements

import jisa.gui.Plot

open class FetChPlot(title: String, xLabel: String = "", yLabel: String = "") : Plot(title, xLabel, yLabel) {

    init {

        isMouseEnabled = true

        addSaveButton("Save")
        addToolbarSeparator()

        addToolbarMenuButton("Scaling").apply {

            addItem("Linear") {
                yAxisType = AxisType.LINEAR
                isMouseEnabled     = true
            }

            addItem("Logarithmic") {
                yAxisType = AxisType.LOGARITHMIC
                autoRangeX()
                autoRangeY()
            }

        }

        addToolbarMenuButton("Display").apply {
            addItem("Markers Only") { series.forEach { it.setLineVisible(false).isMarkerVisible = true } }
            addItem("Lines Only") { series.forEach { it.setLineVisible(true).isMarkerVisible = false } }
            addItem("Markers and Lines") { series.forEach { it.setLineVisible(true).isMarkerVisible = true } }
        }

        addToolbarSeparator()

        addToolbarButton("⛶") {
            copy().show()
        }

    }

}