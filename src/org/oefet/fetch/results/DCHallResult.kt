package org.oefet.fetch.results

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.DCHallPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.DCHall
import org.oefet.fetch.measurement.DCHall.Companion.SG_VOLTAGE
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ln
import kotlin.math.pow
import kotlin.reflect.KClass

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
    override var field:        Double = 0.0

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

    val SET_SD_CURRENT = data.findColumn(DCHall.SET_SD_CURRENT)
    val SD_CURRENT     = data.findColumn(DCHall.SD_CURRENT)
    val SD_VOLTAGE     = data.findColumn(SG_VOLTAGE)
    val FIELD          = data.findColumn(DCHall.FIELD)
    val HALL_1         = data.findColumn(DCHall.HALL_1)
    val HALL_2         = data.findColumn(DCHall.HALL_2)
    val FPP_1          = data.findColumn(DCHall.FPP_1)
    val FPP_2          = data.findColumn(DCHall.FPP_2)
    val TEMPERATURE    = data.findColumn(DCHall.TEMPERATURE)
    /**
     * This is run on construction.
     */
    init {

        parseParameters(data, extraParams, data.getMean(TEMPERATURE))

        // Split data-up into separate tables based on gate voltage
        for ((gate, data) in data.split(SD_VOLTAGE)) {

            val parameters = ArrayList<Quantity>(parameters)
            parameters    += Gate(gate, 0.0)

            // Don't bother with this gate voltage if there's fewer than 2 data-points in it
            if (data.numRows < 2) {
                continue
            }

            // Check which channels were used (if it contains a non-finite value, assume it weren't used)
            val hvm1 = data.find { !it[HALL_1].isFinite() } == null
            val hvm2 = data.find { !it[HALL_2].isFinite() } == null
            val fpp1 = data.find { !it[FPP_1].isFinite()  } == null
            val fpp2 = data.find { !it[FPP_2].isFinite()  } == null

            // If both current and field were swept, then we can perform a better analysis of the output
            if (data.getUniqueValues(FIELD).size > 1 && data.getUniqueValues(SET_SD_CURRENT).size > 1) {

                val gradients = ResultList("Current", "Gradient", "Error")

                // Split the data up based on source-drain current value
                for ((current, currData) in data.split(SET_SD_CURRENT)) {

                    // Determine the Hall voltages (was it channel 1, 2 or the difference between them?)
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

                // Fit the gradients of the V_H vs B fits to their corresponding value of I
                val currFit = gradients.linearFit(0, 1)

                if (currFit != null) {

                    // If the fit worked, then we can finally calculate RH (and thus charge carrier density)
                    val hall    = currFit.gradient * thickness
                    val hallE   = currFit.gradientError * thickness
                    val hallQ   = HallCoefficient(hall, hallE, parameters, possibleParameters)
                    val density = hallQ.pow(-1) * (100.0).pow(-3)  / 1.6e-19

                    // Add both the Hall and carrier density quantities to the list of calculated quantities
                    quantities += hallQ
                    quantities += CarrierDensity(density.value, density.error, parameters, possibleParameters)

                }

            } else {

                // If only one parameter was swept, then just fit VH to I*B/t

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

                // Add both the Hall and carrier density quantities to the list of calculated quantities
                quantities += hallQ
                quantities += CarrierDensity(density.value, density.error, parameters, possibleParameters)

            }

            val possibleParameters = ArrayList<KClass<out Quantity>>(possibleParameters)
            possibleParameters    += BField::class

            // If either of the FPP channels were used then try to calculate magneto-conductivity
            if (fpp1 || fpp2) {

                // Split the data-up based on field strength
                for ((field, condData) in data.split(FIELD)) {

                    val current = condData.getColumns(SD_CURRENT)

                    // Determine whether to use FPP1, FPP2 or FPP2 - FPP1 based on which channels were used
                    val voltage = if (fpp1 && fpp2) {
                        condData.getColumns(FPP_2) - condData.getColumns(FPP_1)
                    } else if (fpp1) {
                        condData.getColumns(FPP_1)
                    } else {
                        condData.getColumns(FPP_2)
                    }

                    // Perform a linear fit to find conductivity for this value of magnetic field strength
                    val condFit     = Fitting.linearFit(voltage, current) ?: continue
                    val value       = condFit.gradient * separation / (width * thickness) / 100.0
                    val error       = condFit.gradientError * separation / (width * thickness) / 100.0

                    // Make a copy of the parameters to associate with this quantity, replacing the value of magnetic
                    // field with the one associated with this calculation
                    val mParameters = parameters.filter { it !is BField }.toMutableList()
                    mParameters    += BField(field, 0.0)

                    // Add the magneto-conductivity value to the list of calculated quantities
                    quantities     += MConductivity(value, error, mParameters, possibleParameters)

                }

            }

        }

    }

    /**
     * Calculates any extra quantities that can only be calculated by combining quantities from multiple result files
     */
    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {

        val extras   = LinkedList<Quantity>()
        val excluded = listOf(Temperature::class, Frequency::class)

        // Look over all values of the Hall coefficient that were calculated above
        for (hallQuantity in quantities.filter { it is HallCoefficient }) {

            // Find compatible values of conductivity for this Hall measurement and multiply to get mobility
            extras += otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hallQuantity) }
                                     .map    { hallQuantity * it * 1e6 }
                                     .map    { HallMobility(it.value, it.error, hallQuantity.parameters) }

            // Check to see if fitting has already been done for extracting un-screened Hall coefficient
            if (otherQuantities.find { it is UnscreenedHall && it.isCompatibleWith(hallQuantity, excluded) } == null) {

                // Find all Hall coefficients in the same temperature sweep
                val halls = otherQuantities.filter { it is HallCoefficient && it.isCompatibleWith(hallQuantity, excluded) && it.hasParameter(Temperature::class) }

                // Calculate values of ln(RH), 1/sqrt(RH) and T^(-1/4)
                val lnrh  = halls.map { ln(it.value) }
                val rh05  = halls.map { it.value.pow(-0.5) }
                val t025  = halls.map { it.getParameter(Temperature::class)?.value?.pow(-0.25) ?: 0.0 }

                // Find peak conductivity value from corresponding conductivity data
                val maxC  = otherQuantities
                            .filter { it is Conductivity && it.isCompatibleWith(hallQuantity, excluded) }
                            .maxBy  { it.value }

                // Do the two fits for extracting T0 and RH0
                val fit1  = Fitting.linearFit(t025, lnrh)
                val fit2  = Fitting.linearFit(t025, rh05)

                // Only bother if the fitting worked and there's a peak value of conductivity to associate with it
                if (fit1 != null && fit2 != null && maxC != null) {

                    // Turn numbers in "Quantity" objects - allows for easy error propagation
                    val grad1   = SimpleQuantity(fit1.gradient, fit1.gradientError)
                    val grad2   = SimpleQuantity(fit2.gradient, fit2.gradientError)
                    val incp2   = SimpleQuantity(fit2.intercept, fit2.interceptError)
                    val params  = parameters.filter { it !is Temperature }.toMutableList()
                    val pParams = possibleParameters.filter { it != Temperature::class }

                    params += maxC

                    val t0 = (grad1 * 0.5).pow(4)
                    val r0 = (incp2 + (grad2 / (grad1 * 0.5))).pow(-2)
                    val n0 = (r0 * 1.6e-19).pow(-1) * (100.0).pow(-3)

                    extras += MottHoppingT0(t0.value, t0.error, params, pParams)
                    extras += UnscreenedHall(r0.value, r0.error, params, pParams)
                    extras += BandLikeDensity(n0.value, n0.error, params, pParams)

                }

            }

        }

        return extras

    }

}