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
    private val notDisplayed = listOf(
        FwdSatMobility::class,
        BwdSatMobility::class,
        FwdLinMobility::class,
        BwdLinMobility::class
    )

    init {

        setIcon(Icon.DATA)
        setLeftElement(fileList)

        fileList.setOnChange {

            val selected = fileList.selected.getObject()
            val params   = Display("Parameters")

            for (quantity in selected.quantities) {

                if (!notDisplayed.contains(quantity::class)) params.addParameter(
                    quantity.name,
                    "%s %s".format(quantity.value, quantity.unit)
                )

            }

            for (parameter in selected.parameters) {
                params.addParameter(parameter.name, "%s %s".format(parameter.value, parameter.unit))
            }

            val row  = Grid(2, params, selected.plot)
            val grid = Grid(selected.name, 1, row, Table("Table of Data", selected.data))

            setCentreElement(grid)

        }

        fileList.addDefaultMenuItem("Remove Result") {

            it.remove()
            results -= it.getObject()

        }

        fileList.addToolbarButton("Add Results...") { addFiles() }

        fileList.addToolbarButton("Clear Results") {

            fileList.clear()
            results.clear()

        }

    }

    private fun addFiles() {

        val paths = GUI.openFileMultipleSelect() ?: return
        loadFiles(paths)

    }

    private fun addFolder() {

        val folder = GUI.directorySelect() ?: return
        val files  = (File(folder).list() ?: return).asList()
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

        for (path in paths) {

            try {
                addData(ResultList.loadFile(path))
            } catch (e: Exception) {

                e.printStackTrace()
                fileList.add(null, e.message, path, ActionQueue.Status.ERROR.image)

            }

        }

        fileList.select(0)

    }

    fun getNames(): Map<Double, String> {

        val list = results.stream().map { it.data.getAttribute("Name") }.distinct().toList()
        val map = HashMap<Double, String>()

        for ((index, value) in list.withIndex()) map[(index + 1).toDouble()] = value

        return map

    }

    fun addData(data: ResultTable) {

        val result = loadData(data)
        fileList.add(result, result.name, result.getParameterString(), ActionQueue.Status.COMPLETED.image)
        results += result

    }

    private fun loadData(data: ResultTable): ResultFile {


        val names = results.stream().map { it.data.getAttribute("Name") }.distinct().toList()
        val index = names.indexOf(data.getAttribute("Name"))
        val n     = (if (index >= 0) index else names.size) + 1
        val extra = listOf(Device(n))

        return when (data.getAttribute("Type")) {

            "Transfer"         -> TransferResult(data, extra)
            "Output"           -> OutputResult(data, extra)
            "FPP Conductivity" -> CondResult(data, extra)
            else               -> null

        } ?: throw Exception("Unknown measurement type")

    }


}