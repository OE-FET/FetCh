package org.oefet.fetch.gui.tabs

import jisa.Util
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.gui.*
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.Measurements
import org.oefet.fetch.analysis.UnknownResultException
import org.oefet.fetch.quantities.DoubleQuantity
import org.oefet.fetch.quantities.MaxLinMobility
import org.oefet.fetch.quantities.MaxSatMobility
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.FetChResult
import java.io.File
import java.util.*
import kotlin.reflect.KClass

/**
 * Page for loading in and viewing previous results
 */
object FileLoad : BorderDisplay("Results") {

    private val fileList = ListDisplay<FetChResult>("Loaded Results")
    private val cached   = HashMap<FetChResult, Grid>()

    private val progress = Progress("Loading Files").apply {
        status = "Reading and processing selected files, please wait..."
    }

    private val results = LinkedList<FetChResult>()
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
            cached.remove(listItem.getObject())
            updateDisplay()
        }

        // Add the "Add..." menu button to the file list
        fileList.addToolbarMenuButton("Add...").apply {
            addItem("Files...", ::addFiles)
            addItem("Folder...", ::addFolder)
        }

        fileList.addToolbarMenuButton("Clear").apply {
            addItem("Errors") { clearErrors() }
            addItem("All") {
                fileList.clear()
                results.clear()
                names.clear()
                cached.clear()
                System.gc()
            }
        }

        fileList.addToolbarMenuButton("Filter").apply {
            addItem("Remove") { filter(false) }
            addItem("Retain") { filter(true) }
        }
    }

    private fun updateDisplay() {

        if (fileList.selected == null) {

            // If nothing is selected, then show an empty pane
            centreElement = Grid()

        } else if (!cached.containsKey(fileList.selected.getObject())) {

            val selected = fileList.selected.getObject()
            val params   = DataDisplay("Parameters")

            for (type in selected.quantities.map{ it::class }.distinct()) {

                if (notDisplayed.contains(type)) continue

                val filtered = selected.quantities.filter { it::class == type }
                val instance = filtered.first()
                val unit     = instance.unit
                val name     = instance.name
                val values   = filtered.map { it.value as Double }.filter { it.isFinite() }

                if (values.isEmpty()) {
                    continue
                }

                val min = values.minOrNull()
                val max = values.maxOrNull()

                var value = if (min == max) "%.03g".format(min) else "%.03g to %.03g".format(min, max)

                value += if (filtered.filter{ it is DoubleQuantity }.any { (it as DoubleQuantity).error > 0 }) {
                    " ± %.03g %s".format(filtered.filter{ it is DoubleQuantity }.map { (it as DoubleQuantity).error }.average(), unit)
                } else {
                    " $unit"
                }

                params.addParameter(name, value)

            }

            for (parameter in selected.parameters) {

                val value = parameter.value

                if (value !is Double || value.isFinite()) {
                    params.addParameter(parameter.name, "%s %s".format(value, parameter.unit))
                }

            }

            val row  = Grid(2, params, selected?.getPlot() ?: Measurements.createElement(selected.data))
            val grid = Grid(selected.name, 1, row, Table("Table of Data", selected.data))

            centreElement    = grid
            cached[selected] = grid

        } else {
            centreElement = cached[fileList.selected.getObject()]
        }

    }

    private fun toDisplay(quantity: Quantity<*>) : Boolean {
        return quantity::class !in notDisplayed
    }

    private fun filter(retain: Boolean) {

        val types  = results.flatMap { r -> r.parameters.map { p -> p::class } }.distinct()
        val values = LinkedHashMap<Quantity<*>, List<*>>()

        for (type in types) {

            val typeExample     = results.flatMap { r -> r.parameters }.find { it::class == type } as Quantity<*>
            val typeValues      = results.flatMap { r -> r.parameters }.filter { it::class == type }.map { it.value }.distinct().sortedBy { it as Comparable<Any> } + null
            values[typeExample] = typeValues

        }

        val grid         = Grid(if (retain) "Retain by Filter" else "Remove by Filter", 3);
        grid.setWindowSize(1000.0, 700.0);
        grid.setGrowth(true, false)

        val responses = LinkedHashMap<Quantity<*>, MutableList<Field<Boolean>>>()

        for ((type, options) in values) {

            responses[type] = ArrayList<Field<Boolean>>()

            val fields = Fields(type.name)

            for (option in options) {
                responses[type]!!.add(fields.addCheckBox(if (option == null) "None" else "$option ${type.unit}", retain))
            }

            grid.add(fields)

        }

        if (grid.showAsConfirmation()) {

            val toRemove = LinkedHashSet<FetChResult>()

            if (retain) {

                for ((quantity, fields) in responses) {

                    val selected = values[quantity]!!.withIndex().filterIndexed { i, _ -> fields[i].value }.map { it.value }

                        toRemove += results.filter {

                            val found = it.parameters.find { it::class == quantity::class }

                            if (found == null) {
                                null !in selected
                            } else {
                                found.value !in selected
                            }

                        }

                }

            } else {

                val selected = LinkedHashMap<KClass<out Quantity<*>>, List<*>>()

                for ((quantity, fields) in responses) {
                    selected[quantity::class] = values[quantity]!!.withIndex().filterIndexed { i, _ -> fields[i].value }.map { it.value }
                }

                toRemove += results.filter {

                    r -> r.parameters.all {
                        p -> (p.value in selected[p::class]!!) || selected[p::class]!!.isEmpty()
                    } && selected.filter {
                        null in it.value && selected[it.key]!!.isNotEmpty()
                    }.all {
                        it.key !in r.parameters.map { it::class }
                    }

                }

            }

            for (result in toRemove) {

                fileList.filter { it.getObject() in toRemove }.forEach { it.remove() }
                cached.clear()
                results.removeAll(toRemove)

            }
            
            updateDisplay()

        }

    }

    fun getQuantities(): List<Quantity<*>> {

        val list = ArrayList<Quantity<*>>()

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
        val result = Measurements.loadResultFile(data) ?: throw UnknownResultException("Unknown result file.")

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
                fileList.add(null, e.message, path, Action.Status.ERROR.image)

            }

            // Make the progress bar tick along one unit
            progress.incrementProgress()

        }

        // Make sure we end with a 100% value
        progress.setProgress(1.0, 1.0)

        centreElement = Grid()

        if (fileList.isEmpty) {
            updateDisplay()
        } else {
            fileList.select(0)
        }

    }

    private fun clearErrors() {

        fileList.filter { it.getObject() == null }.forEach { it.remove() }
        updateDisplay()

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