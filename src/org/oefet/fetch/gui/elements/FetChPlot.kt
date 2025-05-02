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

            val tabs = Tabs("Edit Series Styles")

            for (s in series) {

                val params = Form(s.name)

                params.addChoice("Colour", colours.values.indexOf(colours.values.find { it.toString() == s.colour.toString() }).takeIf { it != -1 } ?: 0, *colours.keys.toTypedArray()).apply {
                    addChangeListener { _ ->
                        s.colour = colours.values.toList()[value]
                    }
                }

                params.addCheckBox("Marker Visible", s.isMarkerVisible).apply {
                    addChangeListener { _ ->
                        s.isMarkerVisible = value
                    }
                }

                params.addChoice("Marker Shape", s.markerShape.ordinal, *Series.Shape.values().map { it.name }.toTypedArray()).apply {
                    addChangeListener { _ ->
                        s.markerShape = Series.Shape.values()[value]
                    }
                }

                params.addDoubleField("Marker Size", s.markerSize).apply {
                    addChangeListener { _ ->
                        s.markerSize = value
                    }
                }

                params.addCheckBox("Line Visible", s.isLineVisible).apply {
                    addChangeListener { _ ->
                        s.isLineVisible = value
                    }
                }

                params.addDoubleField("Line Width", s.lineWidth).apply {
                    addChangeListener { _ ->
                        s.lineWidth = value
                    }
                }

                params.addChoice("Line Dash", s.lineDash.ordinal , *Series.Dash.values().map { it.name }.toTypedArray()).apply {
                    addChangeListener { _ ->
                        s.lineDash = Series.Dash.values()[value]
                    }
                }

                tabs.add(Grid(s.name, params))

            }

            tabs.showAsAlert()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}