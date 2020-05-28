package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.analysis.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.streams.toList

object FileLoad : BorderDisplay("Results") {

    private val fileList     = ListDisplay<ResultFile>("Loaded Results")
    private val results      = LinkedList<ResultFile>()
    private val names        = LinkedList<String>()
    private val notDisplayed = listOf(
        FwdSatMobility::class,
        BwdSatMobility::class,
        FwdLinMobility::class,
        BwdLinMobility::class,
        MaxLinMobility::class,
        MaxSatMobility::class
    )

    init {

        setIcon(Icon.DATA)
        leftElement = fileList

        fileList.setOnChange {

            if (fileList.selected == null) {
                centreElement = Grid()
            } else {

                val selected = fileList.selected.getObject()
                val params = Display("Parameters")

                for (quantity in selected.quantities) {

                    if (!notDisplayed.contains(quantity::class)) params.addParameter(
                        quantity.name,
                        "%s %s".format(quantity.value, quantity.unit)
                    )

                }

                for (parameter in selected.parameters) {
                    params.addParameter(parameter.name, "%s %s".format(parameter.value, parameter.unit))
                }

                val row = Grid(2, params, selected.plot)
                val grid = Grid(selected.name, 1, row, Table("Table of Data", selected.data))

                centreElement = grid

            }

        }

        fileList.addDefaultMenuItem("Remove Result") {

            it.remove()
            results -= it.getObject()

        }

        val menu = fileList.addToolbarMenuButton("Add...")

        menu.addItem("Files...") { addFiles() }
        menu.addItem("Folder...") { addFolder() }

        fileList.addToolbarButton("Clear") {

            fileList.clear()
            results.clear()
            System.gc()

        }

    }

    private fun addFiles() {

        val paths = GUI.openFileMultipleSelect() ?: return
        loadFiles(paths)

    }

    private fun addFolder() {

        val folder = GUI.directorySelect() ?: return
        val files  = File(folder).listFiles { f -> !f.isDirectory }?.map { it.absolutePath } ?: return
        loadFiles(files)

    }

    fun getQuantities(): List<Quantity> {

        val list = ArrayList<Quantity>()

        for (result in results) {
            list += result.quantities
        }

        for (result in results) {
            list += result.calculateHybrids(list)
        }

        return list

    }

    private fun loadFiles(paths: List<String>) {

        val progress = Progress("Loading Files")
        progress.title = "Loading Files"
        progress.setStatus("Reading and processing selected files, please wait...")

        progress.setProgress(0.0, paths.size.toDouble())
        var p = 0.0

        centreElement = Grid(progress)

        paths.parallelStream().forEach { path ->

            p++

            try {

                addData(ResultList.loadFile(path))
                progress.setProgress(p)

            } catch (e: UnknownResultException) {

                println("Ignoring $path (${e.message})")

            } catch (e: Exception) {

                e.printStackTrace()
                fileList.add(null, e.message, path, ActionQueue.Status.ERROR.image)

            }

        }

        progress.setProgress(1.0, 1.0)

        fileList.select(0)

    }

    fun getNames(): Map<Double, String> {

        val map  = HashMap<Double, String>()

        for ((index, value) in names.withIndex()) map[(index + 1).toDouble()] = value

        return map

    }

    fun addData(data: ResultTable) {

        val result = processData(data)
        fileList.add(result, result.name, result.getParameterString(), result.image)
        results += result

    }

    private fun processData(data: ResultTable): ResultFile {

        val index = names.indexOf(data.getAttribute("Name"))

        val n: Int

        if (index < 0) {
            names += data.getAttribute("Name")
            n      = names.size
        } else {
            n      = index + 1
        }

        val extra = listOf(Device(n))

        return ResultFile.loadData(data, extra)

    }


}