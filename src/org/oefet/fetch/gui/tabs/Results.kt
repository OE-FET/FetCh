package org.oefet.fetch.gui.tabs

import javafx.scene.layout.ColumnConstraints
import jisa.enums.Icon
import jisa.gui.BorderDisplay
import jisa.gui.GUI
import jisa.gui.Grid
import jisa.gui.Tabs
import jisa.results.ResultGroup
import jisa.results.ResultList
import org.oefet.fetch.Measurements
import org.oefet.fetch.data.FetChData
import org.oefet.fetch.gui.elements.ResultDisplay
import java.io.File
import java.nio.file.Path

object Results : Tabs("Results") {

    private val grid = Grid("Loaded Files", 3).apply {
        setGrowth(true, false)
    }

    private val list = mutableListOf<FetChData>()

    init {

        setIcon(Icon.DATA)

        addToolbarMenuButton("Load Data...").apply {

            addItem("File(s)...")  {

                val files = GUI.openFileMultipleSelect()

                if (files != null) {

                    for (file in files) {
                        val data = Measurements.loadResultFile(ResultList.loadCSVFile(file)) ?: continue
                        load(data)
                    }

                }

            }

            addItem("Archive(s)...") {

                val files = GUI.openFileMultipleSelect()

                if (files != null) {

                    for (file in files) {

                        val group = ResultGroup.loadFile(file)

                        for (data in group.allTables) {
                            val data = Measurements.loadResultFile(ResultList.loadCSVFile(file)) ?: continue
                            load(data)
                        }

                    }

                }

            }

            addItem("Directory...")  {

                val dir = GUI.directorySelect()

                if (dir != null) {

                    val files  = File(dir).listFiles { f -> !f.isDirectory }?.map { it.absolutePath } ?: emptyList()

                    for (file in files) {
                        val data = Measurements.loadResultFile(ResultList.loadCSVFile(file)) ?: continue
                        load(data)
                    }

                }

            }

        }

        addToolbarButton("Clear") {
            grid.clear()
            list.clear()
        }

        addToolbarButton("Close All") {
            removeAll(elements - grid)
        }

        add(grid)

        grid.node.widthProperty().addListener { _, _, newValue ->

            val nCols = (newValue.toDouble() / 500.0).toInt()

            grid.numColumns = nCols

            grid.pane.columnConstraints.clear()
            grid.pane.columnConstraints.addAll((1..nCols).map {

                ColumnConstraints().apply {
                    percentWidth = 100.0 / nCols
                }

            })

        }

    }

    fun load(data: FetChData) {

        val display = ResultDisplay(data.name, data)
        grid.add(display)
        list.add(data)

    }

    fun remove(data: FetChData) {

        val index = list.indexOf(data)
        list.removeAt(index)
        grid.remove(grid.elements[index])

    }

}