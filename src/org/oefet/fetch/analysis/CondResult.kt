package org.oefet.fetch.analysis

import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.gui.Series
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.FPPMeasurement

class CondResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = FPPPlot(data)
    override val name       = "Conductivity Measurement (${data.getAttribute("Name")})"
    override val image      = Icon.ELECTRICITY.blackImage

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

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}