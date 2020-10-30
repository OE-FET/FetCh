package org.oefet.fetch.gui.tabs

import jisa.Util
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.Measurements
import org.oefet.fetch.analysis.*
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.analysis.results.ResultFile
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KClass

/**
 * Page for loading in and viewing previous results
 */
object FileLoad : BorderDisplay("Results") {

    private val fileList = ListDisplay<ResultFile>("Loaded Results")

    private val progress = Progress("Loading Files").apply {
        status = "Reading and processing selected files, please wait..."
    }

    private val results = LinkedList<ResultFile>()
    private val names   = LinkedList<String>()

    private val notDisplayed = listOf(
        MaxLinMobility::class,
        MaxSatMobility::class
    )

    init {

        // Set icon and put the list of loaded files on the left
        setIcon(Icon.DATA)
        leftElement = fileList

        // Run updateDisplay() every time the selected file is changed in the file list
        fileList.setOnChange(::updateDisplay)

        // Add context menu item to all items in the file list
        fileList.addDefaultMenuItem("Remove Result") { listItem ->
            results -= listItem.getObject()
            listItem.remove()
        }

        // Add the "Add..." menu button to the file list
        fileList.addToolbarMenuButton("Add...").apply {
            addItem("Files...", ::addFiles)
            addItem("Folder...", ::addFolder)
        }

        // Add the "Clear" button to the file list, setting it to clear everything when clicked
        fileList.addToolbarButton("Clear") {
            fileList.clear()
            results.clear()
            names.clear()
            System.gc()
        }

    }

    private fun updateDisplay() {

        if (fileList.selected == null) {

            // If nothing is selected, then show an empty pane
            centreElement = Grid()

        } else {

            val selected = fileList.selected.getObject()
            val params   = Display("Parameters")

            for (type in selected.quantities.map{ it::class }.distinct()) {

                if (notDisplayed.contains(type)) continue

                val filtered = selected.quantities.filter { it::class == type }
                val instance = filtered.first()
                val unit     = instance.unit
                val name     = instance.name
                val values   = filtered.map{ it.value }
                val min      = values.min()
                val max      = values.max()

                params.addParameter(name, if (min == max) "%.03g %s".format(min, unit) else "%.03g to %.03g %s".format(min, max, unit))

            }

            for (parameter in selected.parameters) {
                params.addParameter(parameter.name, "%s %s".format(parameter.value, parameter.unit))
            }

            val row  = Grid(2, params, selected.plot)
            val grid = Grid(selected.name, 1, row, Table("Table of Data", selected.data))

            centreElement = grid

        }

    }

    private fun toDisplay(quantity: Quantity) : Boolean {
        return quantity::class !in notDisplayed
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

    fun addData(data: ResultTable) {

        val name = data.getAttribute("Name") ?: "Unknown"

        // Determine which device number this result is for
        val index = names.indexOf(name)

        val n = if (index < 0) {
            names += name
            names.size
        } else {
            index + 1
        }

        // Load the result as a ResultFile object, specifying device number parameter to use
        val result = Measurements.loadResultFile(
            data,
            listOf(Device(n.toDouble()))
        ) ?: throw UnknownResultException("Unknown result file.")

        // Add the loaded ResultFile to the list display and overall list of loaded results
        fileList.add(result, result.name, result.getParameterString(), result.image)
        results += result

    }

    private fun loadFiles(paths: List<String>) {

        // Display progress bar
        progress.setProgress(0, paths.size)
        centreElement = Grid(progress)

        // Iterate over paths in alphabetical order
        for (path in paths.sorted()) {

            try {

                addData(ResultList.loadFile(path))

            } catch (e: UnknownResultException) {

                // If it doesn't contain a known measurement type, ignore it
                println("Ignoring $path (${e.message})")

            } catch (e: Exception) {

                // Any other errors should be properly reported
                e.printStackTrace()
                fileList.add(null, e.message, path, ActionQueue.Status.ERROR.image)

            }

            // Make the progress bar tick along one unit
            progress.incrementProgress()

        }

        // Make sure we end with a 100% value
        progress.setProgress(1.0, 1.0)

        if (fileList.isEmpty) {
            updateDisplay()
        } else {
            fileList.select(0)
        }

    }

    private fun addFiles() {

        // Add all files that the user selects
        val paths = GUI.openFileMultipleSelect() ?: return
        loadFiles(paths)

    }

    private fun addFolder() {

        // Add all files in the directory the user selects (filtering out "files" that are actually directories).
        val folder = GUI.directorySelect() ?: return
        val files  = File(folder).listFiles { f -> !f.isDirectory }?.map { it.absolutePath } ?: return
        loadFiles(files)

    }

    fun getNames(): Map<Double, String> {

        val map = HashMap<Double, String>()

        Util.runRegardless {
            for ((index, value) in names.withIndex()) map[(index + 1).toDouble()] = value
        }

        return map

    }


}