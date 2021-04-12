package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.quantities.*
import kotlin.math.abs

class CondResult(data: ResultTable, extraParams: List<Quantity> = emptyList()) :
    FetChResult("Conductivity Measurement", "FPP Conductivity", Icon.ELECTRICITY.blackImage, data, extraParams) {

    val SD_VOLTAGE     = data.findColumn(Conductivity.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Conductivity.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Conductivity.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Conductivity.SG_CURRENT)
    val FPP1_VOLTAGE   = data.findColumn(Conductivity.FPP1_VOLTAGE)
    val FPP2_VOLTAGE   = data.findColumn(Conductivity.FPP2_VOLTAGE)
    val FPP_VOLTAGE    = data.findColumn(Conductivity.FPP_VOLTAGE)
    val TEMPERATURE    = data.findColumn(Conductivity.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Conductivity.GROUND_CURRENT)

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

        val fit   = Fitting.linearFit(data.getColumns(FPP_VOLTAGE), data.getColumns(SD_CURRENT))
        val value = abs(fit.gradient * separation / (width * thickness) / 100.0)
        val error = abs(fit.gradientError * separation / (width * thickness) / 100.0)

        addQuantity(Conductivity(value, error, parameters, possibleParameters))

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}