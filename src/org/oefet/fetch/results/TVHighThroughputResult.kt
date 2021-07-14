package org.oefet.fetch.results

import jisa.experiment.ResultTable
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

    val VOLTAGE1  = data.findColumn(TVHighThroughput.VOLTAGE1)
    val VOLTAGE2  = data.findColumn(TVHighThroughput.VOLTAGE1)
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
        //TODO
        //addQuantity(SeebeckCoefficient((VOLTAGE1 - VOLTAGE2)/(TEMPERATURE1 - TEMPERATURE2), 0, parameters, possibleParameters))
    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

    override fun getPlot(): FetChPlot {
        return TVCResultPlot(data)
    }

}