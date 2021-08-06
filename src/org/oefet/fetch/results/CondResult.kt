package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.quantities.*
import kotlin.math.abs

class CondResult(data: ResultTable) : FetChResult("Conductivity Measurement", "FPP Conductivity", Icon.ELECTRICITY.blackImage, data) {

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

        val usedFPP = data.getAttribute("Used FPP")?.toBoolean() ?: true
        val size    = if (usedFPP) separation else length
        val fit     = Fitting.linearFit(data.toMatrix(FPP_VOLTAGE), data.toMatrix(SD_CURRENT))
        val value   = abs(fit.gradient * size / (width * thickness) / 100.0)
        val error   = abs(fit.gradientError * size / (width * thickness) / 100.0)

        addParameter(FourPointProbe(usedFPP))
        addQuantity(Conductivity(value, error, parameters, possibleParameters))

    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {
        return emptyList()
    }

}