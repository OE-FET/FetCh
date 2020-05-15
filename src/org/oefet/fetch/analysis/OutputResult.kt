package org.oefet.fetch.analysis

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.gui.Series
import jisa.maths.interpolation.Interpolation
import jisa.maths.functions.Function
import org.oefet.fetch.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class OutputResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = Plot("Output Curve", "SD Voltage [V]", "Drain Current [A]")
    override val name       = "Output Measurement (${data.getAttribute("Name")})"

    private val possibleParameters = listOf(
        Temperature::class,
        Repeat::class,
        Time::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class,
        Gate::class,
        Drain::class
    )

    init {

        if (data.getAttribute("Type") != "Transfer") {
            throw Exception("That is not a transfer curve file")
        }

        val length        = data.getAttribute("Length").removeSuffix("m").toDouble()
        val separation    = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
        val width         = data.getAttribute("Width").removeSuffix("m").toDouble()
        val thickness     = data.getAttribute("Thickness").removeSuffix("m").toDouble()
        val dielectric    = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
        val permittivity  = data.getAttribute("Dielectric Permittivity").toDouble()

        parameters += Length(length, 0.0)
        parameters += FPPSeparation(separation, 0.0)
        parameters += Width(width, 0.0)
        parameters += Thickness(thickness, 0.0)
        parameters += DThickness(dielectric, 0.0)
        parameters += Permittivity(permittivity, 0.0)

        for ((_, value) in data.attributes) {

            parameters += Quantity.parseValue(value) ?: continue

        }

        parameters += extraParams

        val capacitance = permittivity * EPSILON / dielectric

        try {

            val fwd = ResultList("V", "G", "I")
            val bwd = ResultList("V", "G", "I")
            val SDV = 0
            val SGV = 1
            val SDI = 2

            for ((gate, data) in data.split(SET_SG)) {

                val fb = data.splitTwoWaySweep { it[SET_SD] }

                for (row in fb.forward)  fwd.addData(row[SET_SD], gate, row[SD_CURRENT])
                for (row in fb.backward) bwd.addData(row[SET_SD], gate, row[SD_CURRENT])

            }

            for ((drain, data) in fwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val mobility    = max(linMobility, satMobility)
                    val params      = ArrayList(parameters)
                    params         += Gate(gate, 0.0)
                    params         += Drain(drain, 0.0)

                    if (mobility.isFinite()) quantities += FwdLinMobility(mobility, 0.0, params, possibleParameters)

                }

            }

            for ((drain, data) in bwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val mobility    = max(linMobility, satMobility)
                    val params      = ArrayList(parameters)
                    params         += Gate(gate, 0.0)
                    params         += Drain(drain, 0.0)

                    if (mobility.isFinite()) quantities += BwdLinMobility(mobility, 0.0, params, possibleParameters)

                }

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun plotData() {

        plot.useMouseCommands(true)
        plot.setYAxisType(Plot.AxisType.LOGARITHMIC)
        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)

        plot.createSeries()
            .showMarkers(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SG, "D (SG: %s V)")

        plot.createSeries()
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SG, "G (SG: %sV)")


        plot.addSaveButton("Save")
        plot.addToolbarSeparator()
        plot.addToolbarButton("Linear") { plot.setYAxisType(Plot.AxisType.LINEAR) }
        plot.addToolbarButton("Logarithmic") { plot.setYAxisType(Plot.AxisType.LOGARITHMIC) }


    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> = emptyList()

}