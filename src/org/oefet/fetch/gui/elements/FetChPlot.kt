package org.oefet.fetch.gui.elements

import javafx.scene.paint.Color
import jisa.gui.*
import java.lang.reflect.Modifier

open class FetChPlot(title: String, xLabel: String = "", yLabel: String = "") : Plot(title, xLabel, yLabel) {

    val colours = mapOf(
        "PLOT DEFAULT 1" to Color.web("#f3622d"),
        "PLOT DEFAULT 2" to Color.web("#fba71b"),
        "PLOT DEFAULT 3" to Color.web("#57b757"),
        "PLOT DEFAULT 4" to Color.web("#41a9c9"),
        "PLOT DEFAULT 5" to Color.web("#4258c9"),
        "PLOT DEFAULT 6" to Color.web("#9a42c8"),
        "PLOT DEFAULT 7" to Color.web("#c84164"),
        "PLOT DEFAULT 8" to Color.web("#888888")
    ) + Colour::class.java.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.type == Color::class.java }.associate { it.name to it.get(null) as Color }

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

//        addToolbarMenuButton("Display").apply {
//            addItem("Markers Only") { series.forEach { it.setLineVisible(false).isMarkerVisible = true } }
//            addItem("Lines Only") { series.forEach { it.setLineVisible(true).isMarkerVisible = false } }
//            addItem("Markers and Lines") { series.forEach { it.setLineVisible(true).isMarkerVisible = true } }
//        }

        addToolbarButton("Styling") {
            editProperties()
        }

        addToolbarSeparator()

        addToolbarButton("⛶") {
            copy().show()
        }

    }

    fun editProperties() {

        try {

            val tabs = Grid("Edit Series Styles", 2).apply {
                setGrowth(true, false)
            }

            for (s in series) {

                val params = Fields(s.name)

                val colour = params.addChoice("Colour", colours.values.indexOf(colours.values.find { it.toString() == s.colour.toString() }).takeIf { it != -1 } ?: 0, *colours.keys.toTypedArray()).apply {
                    setOnChange {
                        s.colour = colours.values.toList()[value]
                    }
                }

                val mVis   = params.addCheckBox("Marker Visible", s.isMarkerVisible).apply {
                    setOnChange {
                        s.isMarkerVisible = value
                    }
                }

                val marker = params.addChoice("Marker Shape", s.markerShape.ordinal, *Series.Shape.values().map { it.name }.toTypedArray()).apply {
                    setOnChange {
                        s.markerShape = Series.Shape.values()[value]
                    }
                }

                val mSize  = params.addDecimalField("Marker Size", s.markerSize).apply {
                    setOnChange {
                        s.markerSize = value
                    }
                }

                val lVis   = params.addCheckBox("Line Visible", s.isLineVisible).apply {
                    setOnChange {
                        s.isLineVisible = value
                    }
                }

                val lThick = params.addDecimalField("Line Width", s.lineWidth).apply {
                    setOnChange {
                        s.lineWidth = value
                    }
                }

                val lDash  = params.addChoice("Line Dash", s.lineDash.ordinal , *Series.Dash.values().map { it.name }.toTypedArray()).apply {
                    setOnChange {
                        s.lineDash = Series.Dash.values()[value]
                    }
                }

                tabs.add(params)

            }

            tabs.showAsAlert()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}