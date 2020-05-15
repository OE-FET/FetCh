package org.oefet.fetch.analysis

import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.gui.Series
import jisa.maths.fits.Fitting
import org.oefet.fetch.measurement.FPPMeasurement

class CondResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = Plot("Conductivity Sweep", "SD Current [A]", "Voltage [V]")
    override val name       = "Conductivity Measurement (${data.getAttribute("Name")})"

    private val possibleParameters = listOf(
        Temperature::class,
        Repeat::class,
        Time::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class
    )

    init {

        if (data.getAttribute("Type") != "FPP Conductivity") {
            throw Exception("That is not a FPP Conductivity measurement file")
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

        if (parameters.count { it is Temperature } == 0) parameters += Temperature(data.getMean(FPPMeasurement.TEMPERATURE), 0.0, emptyList())

        parameters += extraParams

        val fit = Fitting.linearFit(
            data.getColumns(FPPMeasurement.FPP_VOLTAGE),
            data.getColumns(FPPMeasurement.SD_CURRENT)
        )

        val value = fit.gradient * separation / (width * thickness) / 100.0
        val error = fit.gradientError * separation / (width * thickness) / 100.0

        quantities += Conductivity(value, error, parameters, possibleParameters)

        plotData()

    }

    private fun plotData() {

        plot.useMouseCommands(true)
        plot.setPointOrdering(Plot.Sort.ORDER_ADDED)

        plot.createSeries()
            .setName("SD Voltage")
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, FPPMeasurement.SD_CURRENT, FPPMeasurement.SD_VOLTAGE)

        plot.createSeries()
            .setName("Probe 1")
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, FPPMeasurement.SD_CURRENT, FPPMeasurement.FPP1_VOLTAGE)

        plot.createSeries()
            .setName("Probe 2")
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, FPPMeasurement.SD_CURRENT, FPPMeasurement.FPP2_VOLTAGE)

        plot.createSeries()
            .setName("FPP Difference")
            .showMarkers(true)
            .polyFit(1)
            .watch(data, FPPMeasurement.SD_CURRENT, FPPMeasurement.FPP_VOLTAGE)

        plot.addSaveButton("Save")

    }
    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}