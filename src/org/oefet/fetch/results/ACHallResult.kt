package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.maths.Range
import jisa.maths.fits.Fitting
import jisa.maths.matrices.RealMatrix
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.math.*

class ACHallResult(data: ResultTable) : FetChResult("AC Hall Measurement", "AC Hall", Icon.CIRCLES.blackImage, data) {

    val FREQUENCY = data.findColumn(ACHall.FREQUENCY)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val RMS_FIELD = data.findColumn(ACHall.RMS_FIELD)
    val SD_CURRENT = data.findColumn(ACHall.SD_CURRENT)
    val TEMPERATURE = data.findColumn(ACHall.TEMPERATURE)
    val X_ERROR = data.findColumn(ACHall.X_ERROR)
    val X_VOLTAGE = data.findColumn(ACHall.X_VOLTAGE)
    val Y_ERROR = data.findColumn(ACHall.Y_ERROR)
    val Y_VOLTAGE = data.findColumn(ACHall.Y_VOLTAGE)

    private var rotatedHall: RealMatrix? = null
    private var faradayVoltages: RealMatrix? = null

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

        val rmsField = data.getMean(RMS_FIELD)
        val zero     = data.minByOrNull { it[SD_CURRENT].absoluteValue } ?: data[0]
        val voltages = data.toMatrix(X_VOLTAGE, Y_VOLTAGE).transpose() - RealMatrix.asColumn(zero[X_VOLTAGE], zero[Y_VOLTAGE])
        val currents = data.toMatrix(SD_CURRENT)

        field = rmsField
        replaceParameter(BField(field, 0.0))

        addParameter(Frequency(data.getMean(FREQUENCY), 0.0, emptyList()))

        var minVolts: RealMatrix? = null
        var minParam = Double.POSITIVE_INFINITY
        var minTheta = 0.0

        // Find the rotation that minimises the minimisation parameter |m_y/m_x|
        for (theta in Range.linear(0, PI, 101)) {

            val rotated = voltages.rotate2D(theta)
            val reFit   = Fitting.linearFit(currents, rotated.getRowMatrix(0))
            val imFit   = Fitting.linearFit(currents, rotated.getRowMatrix(1))
            val param   = abs(imFit.gradient / reFit.gradient)

            if (param < minParam) {
                minParam = param
                minVolts = if (reFit.gradient >= 0.0) rotated else rotated * -1.0
                minTheta = theta
            }

        }

        addQuantity(HallPhase(minTheta, 0.0, parameters, possibleParameters))

        // Calculate error weightings
        val hallErrors = data.toMatrix(X_ERROR, Y_ERROR).rowQuadratures
        val weights = hallErrors.map { x -> x.pow(-2) }

        val vectorHall: RealMatrix = data.toMatrix(HALL_VOLTAGE) - zero[HALL_VOLTAGE]

        // Determine whether to use the PO or VS hall fitting
        val hallFit = if (minVolts != null) {
            rotatedHall     = minVolts.getRowMatrix(0).transpose()
            faradayVoltages = minVolts.getRowMatrix(1).transpose()
            Fitting.linearFitWeighted(currents, minVolts.getRowMatrix(0), weights) ?: Fitting.linearFit(currents, minVolts.getRowMatrix(0))
        } else {
            rotatedHall     = null
            faradayVoltages = null
            Fitting.linearFitWeighted(currents, vectorHall, weights) ?: Fitting.linearFit(currents, vectorHall)
        }


        // Calculate parameters from fitting
        val hallValue = hallFit.gradient * thickness / rmsField
        val hallError = hallFit.gradientError * thickness / rmsField
        val hallQuantity = HallCoefficient(hallValue, hallError, parameters, possibleParameters)
        val density = hallQuantity.pow(-1) * (100.0).pow(-3) / 1.6e-19
        val densityQuantity = CarrierDensity(density.value, density.error, parameters, possibleParameters)

        addQuantities(hallQuantity, densityQuantity)

    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {

        val hall = findQuantity(HallCoefficient::class) ?: return emptyList()

        // Find all conductivities that are compatible with this Hall measurement
        val extras = LinkedList<Quantity<*>>()
        val conductivities =
            otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hall) }.map { it as Conductivity }

        for (conductivity in conductivities) {

            val mobility = hall.value * conductivity.value * 100.0 * 10000.0
            val error =
                mobility * sqrt((hall.error / hall.value).pow(2) + (conductivity.error / conductivity.value).pow(2))
            extras += HallMobility(mobility, error, parameters)

        }

        val excluded = listOf(Temperature::class, Frequency::class)

        if (otherQuantities.find { it is UnscreenedHall && it.isCompatibleWith(hall, excluded) } == null) {

            val halls = otherQuantities.filter {
                it is HallCoefficient && it.isCompatibleWith(
                    hall,
                    excluded
                ) && it.hasParameter(Temperature::class)
            }.map { it as HallCoefficient }
            val lnrh = halls.map { ln(it.value) }
            val rh05 = halls.map { it.value.pow(-0.5) }
            val t025 = halls.map { it.getParameter(Temperature::class)?.value?.pow(-0.25) ?: 0.0 }

            // Find peak conductivity value from corresponding conductivity data
            val conds = otherQuantities
                .filter { it is Conductivity && it.isCompatibleWith(hall, excluded) }
                .map { it as Conductivity }

            val maxC = conds.maxByOrNull { it.value }

            val fit1 = Fitting.linearFit(t025, lnrh)
            val fit2 = Fitting.linearFit(t025, rh05)

            if (fit1 != null && fit2 != null && maxC != null) {

                val grad1 = SimpleQuantity(fit1.gradient, fit1.gradientError)
                val grad2 = SimpleQuantity(fit2.gradient, fit2.gradientError)
                val incp2 = SimpleQuantity(fit2.intercept, fit2.interceptError)
                val params = parameters.filter { it !is Temperature }.toMutableList()
                val pParams = possibleParameters.filter { it != Temperature::class }

                params += MaxConductivity(maxC.value, maxC.error, maxC.parameters, maxC.possibleParameters)

                val t0 = (grad1 * 0.5).pow(4)
                val r0 = (incp2 + (grad2 / (grad1 * 0.5))).pow(-2)
                val n0 = (r0 * 1.6e-19).pow(-1) * (100.0).pow(-3)

                val unscreened = UnscreenedHall(r0.value, r0.error, params, pParams)
                extras += MottHoppingT0(t0.value, t0.error, params, pParams)
                extras += unscreened
                extras += BandLikeDensity(n0.value, n0.error, params, pParams)

                extras += conds.map {
                    val mob = unscreened * it * 1e6
                    UnscreenedHallMobility(mob.value, mob.error, it.parameters, it.possibleParameters)
                }

            }

        }

        return extras

    }

    override fun getPlot(): FetChPlot? {

        return if (rotatedHall != null && faradayVoltages != null) {
            ACHallPlot(data, rotatedHall, faradayVoltages)
        } else {
            null
        }

    }

}