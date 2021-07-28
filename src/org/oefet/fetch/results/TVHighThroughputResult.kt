package org.oefet.fetch.results

import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVHighThroughput
import org.oefet.fetch.quantities.*
import kotlin.math.abs

class TVHighThroughputResult(data: ResultTable) :
    FetChResult(
        "Thermal Voltage High Throughput",
        "Thermal Voltage High Throughput",
        Images.getImage("fire.png"),
        data
    ) {

    val VOLTAGE  = data.findColumn(TVHighThroughput.VOLTAGE)
    val TEMPERATURE_DIFFERENCE = data.findColumn(TVHighThroughput.TEMPERATURE_DIFFERENCE)

    private val possibleParameters = listOf(
        Device::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class
    )

    init {
        val fit   = Fitting.linearFit(data.toMatrix(TEMPERATURE_DIFFERENCE), data.toMatrix(VOLTAGE))
        val value = abs(fit.gradient)
        val error = abs(fit.gradientError)
        addQuantity(SeebeckCoefficient(value, error, parameters, possibleParameters))
    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {
        return emptyList()
    }



}