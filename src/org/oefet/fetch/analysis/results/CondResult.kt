package org.oefet.fetch.analysis.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.maths.fits.Fitting
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.measurement.Conductivity.Companion.TEMPERATURE

class CondResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = FPPPlot(data)
    override val name       = "Conductivity Measurement (${data.getAttribute("Name")})"
    override val image      = Icon.ELECTRICITY.blackImage
    override val label      = "FPP Conductivity"

    override var length:       Double = 0.0
    override var separation:   Double = 0.0
    override var width:        Double = 0.0
    override var thickness:    Double = 0.0
    override var dielectric:   Double = 0.0
    override var permittivity: Double = 0.0
    override var temperature:  Double = Double.NaN
    override var repeat:       Double = 0.0
    override var stress:       Double = 0.0

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

        parseParameters(data, extraParams, data.getMean(TEMPERATURE))

        if (parameters.count { it is Temperature } == 0) parameters += Temperature(
            data.getMean(TEMPERATURE),
            0.0,
            emptyList()
        )

        val fit = Fitting.linearFit(
            data.getColumns(Conductivity.FPP_VOLTAGE),
            data.getColumns(Conductivity.SD_CURRENT)
        )

        val value = fit.gradient * separation / (width * thickness) / 100.0
        val error = fit.gradientError * separation / (width * thickness) / 100.0

        quantities += Conductivity(
            value,
            error,
            parameters,
            possibleParameters
        )

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}