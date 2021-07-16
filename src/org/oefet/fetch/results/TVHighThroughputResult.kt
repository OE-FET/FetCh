package org.oefet.fetch.results

import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.elements.TVCResultPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVHighThroughput
import org.oefet.fetch.quantities.*

class TVHighThroughputResult(data: ResultTable, extraParams: List<Quantity> = emptyList()) :
    FetChResult(
        "Thermal Voltage High Throughput",
        "Thermal Voltage High Throughput",
        Images.getImage("fire.png"),
        data,
        extraParams
    ) {

    val VOLTAGE  = data.findColumn(TVHighThroughput.VOLTAGE)
    val TEMPERATURE1  = data.findColumn(TVHighThroughput.TEMPERATURE1)
    val TEMPERATURE2  = data.findColumn(TVHighThroughput.TEMPERATURE2)

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
        val voltage = data.getMean(VOLTAGE)
        val temperature1 = data.getMean(TEMPERATURE1)
        val temperature2 = data.getMean(TEMPERATURE2)

        addQuantity(SeebeckCoefficient((voltage)/(temperature1 - temperature2), 0.0, parameters, possibleParameters))
    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }



}