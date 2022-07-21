package org.oefet.fetch.results

import jisa.maths.fits.Fitting
import jisa.results.DoubleColumn
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.DCHall
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.math.pow
import kotlin.reflect.KClass

class DCHallResult(data: ResultTable) :
    FetChResult("DC Hall Measurement", "DC Hall", Images.getImage("hall.png"), data) {

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

    val SET_SD_CURRENT = data.findColumn(DCHall.SET_SD_CURRENT)
    val SD_CURRENT = data.findColumn(DCHall.SD_CURRENT)
    val SET_SG_VOLTAGE = data.findColumn(DCHall.SET_SG_VOLTAGE)
    val FIELD = data.findColumn(DCHall.FIELD)
    val HALL_1 = data.findColumn(DCHall.HALL_1)
    val HALL_2 = data.findColumn(DCHall.HALL_2)
    val FPP_1 = data.findColumn(DCHall.HALL_3)
    val FPP_2 = data.findColumn(DCHall.HALL_4)
    val TEMPERATURE = data.findColumn(DCHall.TEMPERATURE)

    init {

        // Split data-up into separate tables based on gate voltage
        for ((gate, data) in data.split(SET_SG_VOLTAGE)) {

            val parameters = ArrayList<Quantity<*>>(parameters)
            parameters += Gate(gate, 0.0)

            // Don't bother with this gate voltage if there's fewer than 2 data-points in it
            if (data.rowCount < 2) {
                continue
            }

            // Check which channels were used (all values recorded from it must be finite (i.e. not Infinity or NaN)
            val usedHVM1 = data.all { it[HALL_1].isFinite() }
            val usedHVM2 = data.all { it[HALL_2].isFinite() }
            val usedFPP1 = data.all { it[FPP_1].isFinite() }
            val usedFPP2 = data.all { it[FPP_2].isFinite() }

            // If both current and field were swept, then we can perform a better analysis of the output
            if (data.getUniqueValues(FIELD).size > 1 && data.getUniqueValues(SET_SD_CURRENT).size > 1) {

                val CURRENT   = DoubleColumn("Current")
                val GRADIENT  = DoubleColumn("Gradient")
                val ERROR     = DoubleColumn("Error")
                val gradients = ResultList(CURRENT, GRADIENT, ERROR)

                // Split the data up based on source-drain current value
                for ((current, currData) in data.split(SET_SD_CURRENT)) {

                    // Determine the Hall voltages (was it channel 1, 2 or the difference between them?)
                    val hallVoltage = if (usedHVM1 && usedHVM2) {
                        currData.toMatrix(HALL_2) - currData.toMatrix(HALL_1)
                    } else if (usedHVM1) {
                        currData.toMatrix(HALL_1)
                    } else {
                        currData.toMatrix(HALL_2)
                    }

                    val gradFit = Fitting.linearFit(currData.toMatrix(FIELD), hallVoltage)

                    if (gradFit != null) {
                        gradients.addData(current, gradFit.gradient, gradFit.gradientError)
                    }

                }

                // Fit the gradients of the V_H vs B fits to their corresponding value of I
                val currFit = Fitting.linearFit(gradients, CURRENT, GRADIENT);

                if (currFit != null) {

                    // If the fit worked, then we can finally calculate RH (and thus charge carrier density)
                    val hall = currFit.gradient * thickness
                    val hallE = currFit.gradientError * thickness
                    val hallQ = HallCoefficient(hall, hallE, parameters, possibleParameters)
                    val density = hallQ.pow(-1) * (100.0).pow(-3) / 1.6e-19

                    // Add both the Hall and carrier density quantities to the list of calculated quantities
                    addQuantities(hallQ, CarrierDensity(density.value, density.error, parameters, possibleParameters))

                }

            } else {

                // If only one parameter was swept, then just fit VH to I*B/t

                val hallVoltage = if (usedHVM1 && usedHVM2) {
                    data.toMatrix(HALL_2) - data.toMatrix(HALL_1)
                } else if (usedHVM1) {
                    data.toMatrix(HALL_1)
                } else {
                    data.toMatrix(HALL_2)
                }

                val xValues = data.toMatrix(SD_CURRENT).elementMultiply(data.toMatrix(FIELD)) / thickness
                val fit = Fitting.linearFit(xValues, hallVoltage)
                val hall = fit.gradient
                val hallE = fit.gradientError
                val hallQ = HallCoefficient(hall, hallE, parameters, possibleParameters)
                val density = hallQ.pow(-1) * (100.0).pow(-3) / 1.6e-19

                // Add both the Hall and carrier density quantities to the list of calculated quantities
                addQuantities(hallQ, CarrierDensity(density.value, density.error, parameters, possibleParameters))

            }

            val possibleParameters = ArrayList<KClass<out Quantity<*>>>(possibleParameters)
            possibleParameters += BField::class

            // If either of the FPP channels were used then try to calculate magneto-conductivity
            if (usedFPP1 || usedFPP2) {

                // Split the data-up based on field strength
                for ((field, condData) in data.split(FIELD)) {

                    val current = condData.toMatrix(SD_CURRENT)

                    // Determine whether to use FPP1, FPP2 or FPP2 - FPP1 based on which channels were used
                    val voltage = if (usedFPP1 && usedFPP2) {
                        condData.toMatrix(FPP_2) - condData.toMatrix(FPP_1)
                    } else if (usedFPP1) {
                        condData.toMatrix(FPP_1)
                    } else {
                        condData.toMatrix(FPP_2)
                    }

                    // Perform a linear fit to find conductivity for this value of magnetic field strength
                    val condFit = Fitting.linearFit(voltage, current) ?: continue
                    val value = condFit.gradient * separation / (width * thickness) / 100.0
                    val error = condFit.gradientError * separation / (width * thickness) / 100.0

                    // Make a copy of the parameters to associate with this quantity, replacing the value of magnetic
                    // field with the one associated with this calculation
                    val mParameters = parameters.filter { it !is BField }.toMutableList()
                    mParameters += BField(field, 0.0)

                    // Add the magneto-conductivity value to the list of calculated quantities
                    addQuantity(MConductivity(value, error, mParameters, possibleParameters))

                }

            }

        }

    }

    /**
     * Calculates any extra quantities that can only be calculated by combining quantities from multiple result files
     */
    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {

        val extras = LinkedList<Quantity<*>>()
        val excluded = listOf(Temperature::class, Frequency::class)

        // Look over all values of the Hall coefficient that were calculated above
        for (hallQuantity in quantities.filter { it is HallCoefficient }.map { it as HallCoefficient }) {

            // Find compatible values of conductivity for this Hall measurement and multiply to get mobility
            extras += otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hallQuantity) }
                .map { it as Conductivity }
                .map { hallQuantity * it * 1e6 }
                .map { HallMobility(it.value, it.error, hallQuantity.parameters) }

        }

        return extras

    }

}