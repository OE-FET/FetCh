package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.quantities.*
import kotlin.math.abs

class CondResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    val SD_VOLTAGE     = data.findColumn(Conductivity.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Conductivity.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Conductivity.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Conductivity.SG_CURRENT)
    val FPP1_VOLTAGE   = data.findColumn(Conductivity.FPP1_VOLTAGE)
    val FPP2_VOLTAGE   = data.findColumn(Conductivity.FPP2_VOLTAGE)
    val FPP_VOLTAGE    = data.findColumn(Conductivity.FPP_VOLTAGE)
    val TEMPERATURE    = data.findColumn(Conductivity.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Conductivity.GROUND_CURRENT)

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
    override var field:        Double = 0.0

    private val possibleParameters = listOf(
        Device::class,
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
            data.getColumns(FPP_VOLTAGE),
            data.getColumns(SD_CURRENT)
        )

        val value = abs(fit.gradient * separation / (width * thickness) / 100.0)
        val error = abs(fit.gradientError * separation / (width * thickness) / 100.0)

        quantities += Conductivity(
            value,
            error,
            parameters,
            possibleParameters
        )

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}