package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.Combination
import jisa.experiment.ResultList
import jisa.gui.Plot
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.math.ln
import kotlin.math.pow
import kotlin.reflect.KClass

object HallAnalysis : Analysis {
    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Analysis.Output {

        val processed = tabulate(quantities)
        val plots     = LinkedList<Plot>()

        for (table in processed) {

            // The columns indices for the value and its error (final two columns)
            val valueIndex = table.parameters.size
            val errorIndex = valueIndex + 1

            // Loop over all the parameter columns in the table
            for ((paramIndex, parameter) in table.parameters.withIndex()) {

                // If the quantity isn't varied or is not meant to be displayed as a number, then skip it
                if (table.table.getUniqueValues(paramIndex).size < 2) continue

                val splits     = LinkedList<Int>()
                val names      = LinkedHashMap<Int, Quantity>()
                var splitCount = 1

                // Loop over all other varied parameters in the table
                for ((splitIndex, splitParam) in table.parameters.withIndex()) {

                    if (splitIndex != paramIndex && table.table.getUniqueValues(splitIndex).size > 1) {
                        splits            += splitIndex
                        names[splitIndex]  = splitParam
                        splitCount        *= table.table.getUniqueValues(splitIndex).size
                    }

                }

                // Don't plot if more values in legend than x-axis
                if ((table.table.getUniqueValues(paramIndex).size) < splitCount) continue

                // Create the plot and the data series
                val line   = table.table.getUniqueValues(paramIndex).size > 20
                val plot   = FetChPlot("${table.quantity.name} vs ${parameter.name}")
                val series = plot.createSeries()
                    .watch(table.table, paramIndex, valueIndex, errorIndex)
                    .setColour(AutoAnalysis.colours[plots.size % AutoAnalysis.colours.size])
                    .setMarkerVisible(!line)
                    .setLineVisible(line)

                if (splits.isNotEmpty()) {

                    series.split(

                        // Split by the unique combination of all varied parameters not on the x-axis
                        { row -> Combination(*splits.map { if (row[it].isFinite()) row[it] else Double.NEGATIVE_INFINITY }.toTypedArray()) },

                        // Label each legend item with any pre-defined labels, or default to x = n.nnn U
                        { row -> names.entries.joinToString(" \t ") { labels[it.value::class]?.get(row[it.key]) ?: "%s = %.4g %s".format(it.value.symbol, row[it.key], it.value.unit) } }

                    )

                    plot.isLegendVisible = true

                } else {

                    plot.isLegendVisible = false

                }

                // Make sure the plot is user-interactive via the mouse
                plot.isMouseEnabled = true
                plot.autoLimits()

                plots += plot

            }

        }

        return Analysis.Output(processed, plots)

    }

    private fun tabulate(quantities: List<Quantity>): List<Analysis.Tabulated> {

        val tables      = LinkedList<Analysis.Tabulated>()
        val quantitySet = quantities.map { it::class }.toSet()

        for (quantityClass in quantitySet) {

            val filtered     = quantities.filter { it::class == quantityClass }
            val instance     = filtered.first()

            val table = ResultList(Col("Temperature", "K"), Col("Gate", "V"), Col("Device"), Col(instance.name, instance.unit), Col("${instance.name} Error", instance.unit))

            for (value in filtered) {

                val temperature = value.parameters.find { it is Temperature } ?: continue
                val gate        = value.parameters.find { it is Gate }
                val device      = value.parameters.find { it is Device }

                table.addData(temperature.value, gate?.value ?: 0.0, device?.value ?: 0.0, value.value, value.error)

            }

            tables += Analysis.Tabulated(listOf(Temperature(0.0, 0.0), Gate(0.0, 0.0), Device(0.0)), instance, table)

        }

        val halls = tables.find { it.quantity::class == HallCoefficient::class }

        if (halls != null) {

            val t0 = ResultList(Col("Gate", "V"), Col("Device"), Col("T0", "K"), Col("Error", "K"))
            val r0 = ResultList(Col("Gate", "V"), Col("Device"), Col("RH0", "m^3/C"), Col("Error", "m^3/C"))
            val n0 = ResultList(Col("Gate", "V"), Col("Device"), Col("Band-Like Carrier Density", "cm^-3"), Col("Error", "cm^-3"))

            for ((device, devData) in halls.table.split(2)) {

                for ((gate, data) in devData.split(1)) {

                    val t025 = data.getColumns(0).map { v -> v.pow(-0.25) }
                    val lnrh = data.getColumns(3).map { v -> ln(v) }
                    val rh05 = data.getColumns(3).map { v -> v.pow(-0.5) }

                    val fit1 = Fitting.linearFit(t025, lnrh)
                    val fit2 = Fitting.linearFit(t025, rh05)
                    val T0   = (0.5 * fit1.gradient).pow(4)
                    val R0   = (fit2.intercept + fit2.gradient/(0.5 * fit1.gradient)).pow(-2)
                    val N0   = ((100.0).pow(-3)) / (1.6e-19 * R0)

                    t0.addData(gate, device, T0, 0.0)
                    r0.addData(gate, device, R0, 0.0)
                    n0.addData(gate, device, N0, 0.0)

                }

            }

            tables += Analysis.Tabulated(listOf(Gate(0.0, 0.0), Device(0.0)), T0(0.0, 0.0), t0)
            tables += Analysis.Tabulated(listOf(Gate(0.0, 0.0), Device(0.0)), UnscreenedHall(0.0, 0.0), r0)
            tables += Analysis.Tabulated(listOf(Gate(0.0, 0.0), Device(0.0)), BandLikeDensity(0.0, 0.0), n0)

        }

        return tables.sortedBy { it.quantity.name }

    }

}