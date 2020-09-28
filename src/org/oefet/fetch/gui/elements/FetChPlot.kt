package org.oefet.fetch.gui.elements

import jisa.Util
import jisa.gui.Plot

open class FetChPlot(title: String, xLabel: String = "", yLabel: String = "") : Plot(title, xLabel, yLabel) {

    init {

        setMouseEnabled(true)

        addSaveButton("Save")
        addToolbarSeparator()

        addToolbarMenuButton("Scaling").apply {
            addItem("Linear") { setYAxisType(AxisType.LINEAR) }
            addItem("Logarithmic") { setYAxisType(AxisType.LOGARITHMIC) }
        }

        addToolbarMenuButton("Display").apply {
            addItem("Markers Only") { series.forEach { it.setLineVisible(false).setMarkerVisible(true) } }
            addItem("Lines Only") { series.forEach { it.setLineVisible(true).setMarkerVisible(false) } }
            addItem("Markers and Lines") { series.forEach { it.setLineVisible(true).setMarkerVisible(true) } }
        }

        addToolbarSeparator()

        addToolbarButton("⛶") {

            copy().apply {

                show()
                Util.sleep(250)
                isMaximised = true

            }
        }

    }

}