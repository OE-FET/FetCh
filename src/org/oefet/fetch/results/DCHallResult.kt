package org.oefet.fetch.results

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.DCHallPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.DCHall.Companion.FIELD
import org.oefet.fetch.measurement.DCHall.Companion.FPP_1
import org.oefet.fetch.measurement.DCHall.Companion.FPP_2
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2
import org.oefet.fetch.measurement.DCHall.Companion.SD_CURRENT
import org.oefet.fetch.measurement.DCHall.Companion.SET_SD_CURRENT
import org.oefet.fetch.measurement.DCHall.Companion.SG_VOLTAGE
import org.oefet.fetch.measurement.DCHall.Companion.TEMPERATURE
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass

class DCHallResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val name = "DC Hall Measurement (${data.getAttribute("Name")})"
    override val image = Images.getImage("hall.png")
    override val label = "DC Hall"

    override var length: Double = 0.0
    override var separation: Double = 0.0
    override var width: Double = 0.0
    override var thickness: Double = 0.0
    override var dielectric: Double = 0.0
    override var permittivity: Double = 0.0
    override var temperature: Double = Double.NaN
    override var repeat: Double = 0.0
    override var stress: Double = 0.0
    override var field: Double = 0.0

    override val plot: Plot = DCHallPlot(data)

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

        for ((gate, data) in data.split(SG_VOLTAGE)) {

            val parameters = ArrayList<Quantity>(parameters)
            parameters += Gate(gate, 0.0)

            if (data.numRows < 2) {
                continue;
            }

            val hvm1 = data.find { !it[HALL_1].isFinite() } == null
            val hvm2 = data.find { !it[HALL_2].isFinite() } == null
            val fpp1 = data.find { !it[FPP_1].isFinite()  } == null
            val fpp2 = data.find { !it[FPP_2].isFinite()  } == null

            if (data.getUniqueValues(FIELD).size > 1 && data.getUniqueValues(SET_SD_CURRENT).size > 1) {

                val gradients = ResultList("Current", "Gradient", "Error")

                for ((current, currData) in data.split(SET_SD_CURRENT)) {

                    val hallVoltage = if (hvm1 && hvm2) {
                        currData.getColumns(HALL_2) - currData.getColumns(HALL_1)
                    } else if (hvm1) {
                        currData.getColumns(HALL_1)
                    } else {
                        currData.getColumns(HALL_2)
                    }

                    val gradFit = Fitting.linearFit(currData.getColumns(FIELD), hallVoltage)

                    if (gradFit != null) {
                        gradients.addData(current, gradFit.gradient, gradFit.gradientError)
                    }

                }

                val currFit = gradients.linearFit(0, 1)

                if (currFit != null) {

                    val hall    = currFit.gradient * thickness
                    val hallE   = currFit.gradientError * thickness
                    val hallQ   = HallCoefficient(hall, hallE, parameters, possibleParameters)
                    val density = hallQ.pow(-1) * (100.0).pow(-3)  / 1.6e-19

                    quantities += hallQ
                    quantities += CarrierDensity(density.value, density.error, parameters, possibleParameters)

                }

            } else {

                val hallVoltage = if (hvm1 && hvm2) {
                    data.getColumns(HALL_2) - data.getColumns(HALL_1)
                } else if (hvm1) {
                    data.getColumns(HALL_1)
                } else {
                    data.getColumns(HALL_2)
                }

                val xValues = data.getColumns(SD_CURRENT).elementMultiply(data.getColumns(FIELD)) / thickness
                val fit     = Fitting.linearFit(xValues, hallVoltage)
                val hall    = fit.gradient
                val hallE   = fit.gradientError
                val hallQ   = HallCoefficient(hall, hallE, parameters, possibleParameters)
                val density = hallQ.pow(-1) * (100.0).pow(-3)  / 1.6e-19

                quantities += hallQ
                quantities += CarrierDensity(density.value, density.error, parameters, possibleParameters)

            }

            val possibleParameters = ArrayList<KClass<out Quantity>>(possibleParameters)
            possibleParameters    += BField::class

            if (fpp1 || fpp2) {

                for ((field, condData) in data.split(FIELD)) {

                    val current = condData.getColumns(SD_CURRENT)

                    val voltage = if (fpp1 && fpp2) {
                        condData.getColumns(FPP_2) - condData.getColumns(FPP_1)
                    } else if (fpp1) {
                        condData.getColumns(FPP_1)
                    } else {
                        condData.getColumns(FPP_2)
                    }

                    val condFit    = Fitting.linearFit(voltage, current) ?: continue
                    val value      = condFit.gradient * separation / (width * thickness) / 100.0
                    val error      = condFit.gradientError * separation / (width * thickness) / 100.0
                    val parameters = ArrayList<Quantity>(parameters)

                    parameters.removeIf { it is BField }

                    parameters    += BField(field, 0.0)
                    quantities    += MConductivity(value, error, parameters, possibleParameters)

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
                    val mobility = quantity.value * condValue * 100.0 * 10000.0
                    val error =
                        mobility * sqrt((quantity.error / quantity.value).pow(2) + (condError / condValue).pow(2))

                    extras += HallMobility(mobility, error, parameters)

                }

            }

        }

        return extras

    }

}