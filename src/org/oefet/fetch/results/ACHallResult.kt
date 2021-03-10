package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.Range
import jisa.maths.fits.Fitting
import jisa.maths.matrices.RealMatrix
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.measurement.ACHall.Companion.FREQUENCY
import org.oefet.fetch.measurement.ACHall.Companion.HALL_VOLTAGE
import org.oefet.fetch.measurement.ACHall.Companion.RMS_FIELD
import org.oefet.fetch.measurement.ACHall.Companion.SD_CURRENT
import org.oefet.fetch.measurement.ACHall.Companion.TEMPERATURE
import org.oefet.fetch.measurement.ACHall.Companion.X_ERROR
import org.oefet.fetch.measurement.ACHall.Companion.X_VOLTAGE
import org.oefet.fetch.measurement.ACHall.Companion.Y_ERROR
import org.oefet.fetch.measurement.ACHall.Companion.Y_VOLTAGE
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

class ACHallResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val name       = "AC Hall Measurement (${data.getAttribute("Name")})"
    override val image      = Icon.CIRCLES.blackImage
    override val label      = "AC Hall"

    override var length: Double       = 0.0
    override var separation: Double   = 0.0
    override var width: Double        = 0.0
    override var thickness: Double    = 0.0
    override var dielectric: Double   = 0.0
    override var permittivity: Double = 0.0
    override var temperature: Double  = Double.NaN
    override var repeat: Double       = 0.0
    override var stress: Double       = 0.0
    override var field: Double        = 0.0

    override val plot: Plot

    private val hallValue: Double
    private val hallError: Double
    private val densityValue: Double
    private val densityError: Double
    private val hallQuantity: Quantity
    private val densityQuantity: Quantity
    private val possibleParameters = listOf(
        Device::class,
        Temperature::class,
        Repeat::class,
        Time::class,
        Frequency::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class,
        RMSField::class
    )

    init {

        parseParameters(data, extraParams, data.getMean(TEMPERATURE))

        val rmsField = data.getMean(RMS_FIELD)
        val voltages = data.getColumns(X_VOLTAGE, Y_VOLTAGE).transpose()
        val currents = data.getColumns(SD_CURRENT)

        field = rmsField
        parameters.removeIf { it is BField }
        parameters += BField(field, 0.0)

        parameters += Frequency(data.getMean(FREQUENCY), 0.0, emptyList())

        var minVolts: RealMatrix? = null
        var minParam = Double.POSITIVE_INFINITY
        var minTheta = 0.0

        // Find the rotation that minimises the minimisation parameter |m_y/m_x|
        for (theta in Range.linear(0, PI, 101)) {

            val rotated = voltages.rotate2D(theta)
            val reFit = Fitting.linearFit(currents, rotated.getRowMatrix(0))
            val imFit = Fitting.linearFit(currents, rotated.getRowMatrix(1))
            val param = abs(imFit.gradient / reFit.gradient)

            if (param < minParam) {
                minParam = param
                minVolts = if (reFit.gradient > 0) rotated else rotated * -1.0
                minTheta = theta
            }

        }

        parameters.add(HallPhase(minTheta, 0.0, parameters, possibleParameters))

        // Calculate error weightings
        val hallErrors = data.getColumns(X_ERROR, Y_ERROR).rowQuadratures
        val weights = hallErrors.map { x -> x.pow(-2) }

        val rotatedHall: RealMatrix?
        val faradayVoltages: RealMatrix?
        val vectorHall: RealMatrix = data.getColumns(HALL_VOLTAGE)

        // Determine whether to use the PO or VS hall fitting
        val hallFit = if (minVolts != null) {
            rotatedHall     = minVolts.getRowMatrix(0).transpose()
            faradayVoltages = minVolts.getRowMatrix(1).transpose()
            Fitting.linearFitWeighted(currents, minVolts.getRowMatrix(0), weights)
        } else {
            rotatedHall     = null
            faradayVoltages = null
            Fitting.linearFitWeighted(currents, vectorHall, weights)
        }


        // Calculate parameters from fitting
        hallValue       = hallFit.gradient * thickness / rmsField
        hallError       = hallFit.gradientError * thickness / rmsField
        hallQuantity    = HallCoefficient(hallValue, hallError, parameters, possibleParameters)
        val density     = hallQuantity.pow(-1) * (100.0).pow(-3)  / 1.6e-19
        densityValue    = density.value
        densityError    = density.error
        densityQuantity = CarrierDensity(density.value, density.error, parameters, possibleParameters)
        quantities     += hallQuantity
        quantities     += densityQuantity

        // AC Hall - Plotting Everything
        plot = ACHallPlot(data, rotatedHall, faradayVoltages)

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {

        // Find all conductivities that are compatible with this Hall measurement
        val extras         = LinkedList<Quantity>()
        val conductivities = otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hallQuantity) }

        for (conductivity in conductivities) {

            val condValue = conductivity.value
            val condError = conductivity.error
            val mobility  = hallValue * condValue * 100.0 * 10000.0
            val error     = mobility * sqrt((hallError / hallValue).pow(2) + (condError / condValue).pow(2))
            extras       += HallMobility(mobility, error, parameters)

        }

        val excluded = listOf(Temperature::class, Frequency::class)

        if (otherQuantities.find { it is UnscreenedHall && it.isCompatibleWith(hallQuantity, excluded) } == null) {

            val halls = otherQuantities.filter { it is HallCoefficient && it.isCompatibleWith(hallQuantity, excluded) && it.hasParameter(Temperature::class) }
            val lnrh  = halls.map { ln(it.value) }
            val rh05  = halls.map { it.value.pow(-0.5) }
            val t025  = halls.map { it.getParameter(Temperature::class)?.value?.pow(-0.25) ?: 0.0 }

            val maxC  = otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hallQuantity, excluded) }.maxBy { it.value }
            val fit1  = Fitting.linearFit(t025, lnrh)
            val fit2  = Fitting.linearFit(t025, rh05)

            if (fit1 != null && fit2 != null && maxC != null) {

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

        return extras

    }

}