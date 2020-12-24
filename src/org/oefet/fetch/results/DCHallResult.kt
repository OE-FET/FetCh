package org.oefet.fetch.results

import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.fits.Fitting
import org.oefet.fetch.quantities.*
import org.oefet.fetch.gui.elements.DCHallPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.DCHall.Companion.FIELD
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2
import org.oefet.fetch.measurement.DCHall.Companion.SD_CURRENT
import org.oefet.fetch.measurement.DCHall.Companion.SG_VOLTAGE
import org.oefet.fetch.measurement.DCHall.Companion.TEMPERATURE
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

class DCHallResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val name       = "DC Hall Measurement (${data.getAttribute("Name")})"
    override val image      = Images.getImage("hall.png")
    override val label      = "DC Hall"

    override var length:       Double = 0.0
    override var separation:   Double = 0.0
    override var width:        Double = 0.0
    override var thickness:    Double = 0.0
    override var dielectric:   Double = 0.0
    override var permittivity: Double = 0.0
    override var temperature:  Double = Double.NaN
    override var repeat:       Double = 0.0
    override var stress:       Double = 0.0

    override val plot : Plot =  DCHallPlot(data)

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

        for ((gate, data) in data.split(SG_VOLTAGE)) {

            if (data.numRows < 2) {
                continue;
            }

            val xValues = data.getColumns(SD_CURRENT).elementMultiply(data.getColumns(FIELD)) / thickness

            var use1 = true;
            var use2 = true;

            for (row in data) {

                if (!row[HALL_1].isFinite()) use1 = false
                if (!row[HALL_2].isFinite()) use2 = false

            }

            if (use1) {

                val fit = Fitting.linearFit(xValues, data.getColumns(HALL_1))

                if (fit != null) {

                    val hall = fit.gradient
                    val hallE = fit.gradientError
                    val density = 1e-6 / (1.6e-19 * hall)
                    val densityE = sqrt((-1e-6 / (1.6e-19 * hall.pow(2))).pow(2) * hallE.pow(2))
                    val parameters = ArrayList<Quantity>(this.parameters)
                    parameters += Gate(gate, 0.0)
                    quantities += HallCoefficient(hall, hallE, parameters, possibleParameters)
                    quantities += CarrierDensity(density, densityE, parameters, possibleParameters)

                }

            }

            if (use2) {

                val fit = Fitting.linearFit(xValues, data.getColumns(HALL_2))

                if (fit != null) {

                    val hall  = fit.gradient
                    val hallE = fit.gradientError
                    val density = 1e-6 / (1.6e-19 * hall)
                    val densityE = sqrt((-1e-6 / (1.6e-19 * hall.pow(2))).pow(2) * hallE.pow(2))
                    val parameters = ArrayList<Quantity>(this.parameters)
                    parameters += Gate(gate, 0.0)
                    quantities += HallCoefficient(hall, hallE, parameters, possibleParameters)
                    quantities += CarrierDensity(density, densityE, parameters, possibleParameters)

                }

            }

        }

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {

        // Find all conductivities that are compatible with this Hall measurement
        val extras = LinkedList<Quantity>()

        for (quantity in quantities) {

            if (quantity is HallCoefficient) {

                val conductivities = quantities.filter { it is Conductivity && it.isCompatibleWith(quantity) }

                for (conductivity in conductivities) {

                    val condValue = conductivity.value
                    val condError = conductivity.error
                    val mobility  = quantity.value * condValue * 100.0 * 10000.0
                    val error     = mobility * sqrt((quantity.error / quantity.value).pow(2) + (condError / condValue).pow(2))
                    extras       += HallMobility(mobility, error, parameters)

                }

            }

        }

        return extras

    }

}