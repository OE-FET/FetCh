package org.oefet.fetch.logging

import jisa.control.ConfigBlock
import java.util.*

class DashPlot(val title: String, val xLabel: String, val yLabel: String) {

    val plotted = LinkedList<Plotted>()

    class Plotted(val name: String, val x: DashValue, val y: DashValue)

    fun writeToConfig(block: ConfigBlock) {

        block.stringValue("title").set(title)
        block.stringValue("xLabel").set(xLabel)
        block.stringValue("yLabel").set(yLabel)

        val plots = block.subBlock("plotted")

        for ((i, plot) in plotted.withIndex()) {

            val sub = plots.subBlock("%d".format(i))

            sub.stringValue("name").set(plot.name)
            sub.stringValue("x").set(plot.x.name)
            sub.stringValue("y").set(plot.y.name)

        }

    }

    companion object {

        fun loadFromConfig(dash: Dash, config: ConfigBlock): DashPlot {

            val title   = config.stringValue("title").getOrDefault("Unknown")
            val xLabel = config.stringValue("xLabel").getOrDefault("Unknown")
            val yLabel = config.stringValue("yLabel").getOrDefault("Unknown")
            val plot   = DashPlot(title, xLabel, yLabel)

            if (config.hasBlock("plotted")) {

                for ((name, block) in config.subBlock("plotted").subBlocks) {

                    val name = block.stringValue("name").getOrDefault("Unknown")
                    val x    = dash.values.find { it.name == block.stringValue("x").getOrDefault("N/A") } ?: continue
                    val y    = dash.values.find { it.name == block.stringValue("y").getOrDefault("N/A") } ?: continue

                    plot.plotted.add(Plotted(name, x, y))

                }

            }

            return plot

        }

    }

}