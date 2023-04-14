package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.maths.Range
import jisa.maths.fits.Fitting
import jisa.maths.matrices.RealMatrix
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.math.*

class ACHallResult(data: ResultTable) : FetChResult("AC Hall Measurement", "AC Hall", Icon.CIRCLES.blackImage, data) {

    // Find data columns in provided results table
    val FARADAY      = data.findColumn(ACHall.FARADAY)
    val FREQUENCY    = data.findColumn(ACHall.FREQUENCY)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val RMS_FIELD    = data.findColumn(ACHall.RMS_FIELD)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)
    val TEMPERATURE  = data.findColumn(ACHall.TEMPERATURE)
    val X_ERROR      = data.findColumn(ACHall.X_ERROR)
    val X_VOLTAGE    = data.findColumn(ACHall.X_VOLTAGE)
    val Y_ERROR      = data.findColumn(ACHall.Y_ERROR)
    val Y_VOLTAGE    = data.findColumn(ACHall.Y_VOLTAGE)

    // Define columns for rotated data
    companion object {

        val ROT_FREQUENCY = Column.ofDecimals("Frequency", "Hz")
        val ROT_CURRENT   = Column.ofDecimals("SD Current", "A")
        val ROT_HALL      = Column.ofDecimals("Hall Voltage", "V")
        val ROT_ERROR     = Column.ofDecimals("Hall Error", "V")
        val ROT_FARADAY   = Column.ofDecimals("Faraday Voltage", "V")
        val FAR_FREQUENCY = Column.ofDecimals("Frequency", "Hz")
        val FAR_VOLTAGE   = Column.ofDecimals("Faraday Voltage", "V")

    }

    val rotated = ResultList(ROT_FREQUENCY, ROT_CURRENT, ROT_HALL, ROT_ERROR, ROT_FARADAY)
    val faraday = ResultList(FAR_FREQUENCY, FAR_VOLTAGE)

    private val possibleParameters = listOf(
        Device::class,
        Temperature::class,
        Frequency::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class,
        RMSField::class,
        Voltage::class
    )

    init {

        val noOptim = if (this.data.attributes.containsKey("No Optimisation")) this.data.getAttribute("No Optimisation").toBoolean() else false

        val faraday: ResultTable?
        val data:    ResultTable

        if (FARADAY != null) {
            faraday = this.data.filter { it[FARADAY] }.takeIf { it.rowCount > 1 }
            data    = this.data.filter { !it[FARADAY] }
        } else {
            faraday = null
            data    = this.data
        }

        for ((frequency, data) in data.split(FREQUENCY)) {

            val parameters = parameters + Frequency(frequency, 0.0)
            val rmsField   = data.mean(RMS_FIELD)
            val zero       = data.minByOrNull { it[SD_CURRENT].absoluteValue } ?: data[0]
            val voltages   = data.toMatrix(X_VOLTAGE, Y_VOLTAGE).transpose()
            val currents   = data.toList(SD_CURRENT)

            field = rmsField
            replaceParameter(BField(field, 0.0))

            var minVolts: RealMatrix? = null
            var minParam = Double.POSITIVE_INFINITY
            var minTheta = 0.0

            // Find the rotation that minimises the minimisation parameter |m_y/m_x|
            for (theta in Range.linear(0, PI, 101)) {

                val rotated = voltages.rotate2D(theta)
                val reFit   = Fitting.linearFit(currents, rotated.getRowMatrix(0))
                val imFit   = Fitting.linearFit(currents, rotated.getRowMatrix(1))
                val param   = try {
                    imFit.gradient.absoluteValue / reFit.gradient.absoluteValue
                } catch (e: Exception) { continue }

                if (param < minParam) {
                    minParam = param
                    minVolts = if (reFit.gradient >= 0.0) rotated else rotated.rotate2D(PI)
                    minTheta = if (reFit.gradient >= 0.0) theta else (theta + PI)
                }

            }

            if (minVolts != null) {
                minVolts -= minVolts.getColMatrix(0);
            }

            addQuantity(HallPhase(minTheta, 0.0, parameters, possibleParameters))

            val vectorHall: RealMatrix = data.toMatrix(HALL_VOLTAGE) - zero[HALL_VOLTAGE]

            val sign = if (!noOptim && faraday != null) {

                val freq    = faraday.toList(FREQUENCY)
                val signage = faraday.toMatrix(X_VOLTAGE, Y_VOLTAGE).transpose().rotate2D(minTheta)
                val fVolt   = signage.getRow(1).toList()
                val fit     = Fitting.linearFit(freq, fVolt)

                this.faraday.mapRows(
                    FAR_FREQUENCY to freq,
                    FAR_VOLTAGE   to fVolt
                )

                fit?.gradient?.sign?.times(-1) ?: +1.0

            } else {
                Fitting.linearFit(currents, vectorHall)?.gradient?.sign ?: +1.0
            }

            // Calculate error weightings
            val hallErrors = data.toList(HALL_ERROR)
            val weights    = hallErrors.map { x -> x.pow(-2) }

            // Determine whether to use the PO or VS hall fitting
            val hallFit = if (!noOptim && minVolts != null) {

                val rotatedHall     = minVolts.getRow(0).toList()
                val faradayVoltages = minVolts.getRow(1).toList()

                for ((index, current) in currents.withIndex()) {

                    rotated.mapRow(
                        ROT_FREQUENCY to frequency,
                        ROT_CURRENT   to current,
                        ROT_HALL      to rotatedHall[index],
                        ROT_ERROR     to hallErrors[index],
                        ROT_FARADAY   to faradayVoltages[index]
                    )

                }

                if (weights.all { it.isFinite() && it > 0 }) {
                    Fitting.linearFitWeighted(currents, rotatedHall, weights) ?: Fitting.linearFit(currents, rotatedHall)
                } else {
                    Fitting.linearFit(currents, rotatedHall)
                }

            } else {
                if (weights.all { it.isFinite() && it > 0 }) {
                    Fitting.linearFitWeighted(currents, vectorHall, weights) ?: Fitting.linearFit(currents, vectorHall)
                } else {
                    Fitting.linearFit(currents, vectorHall)
                }
            }


            // Calculate parameters from fitting
            val hallValue       = sign * abs(hallFit?.gradient ?: 0.0) * thickness / rmsField
            val hallError       = (hallFit?.gradientError ?: 0.0) * thickness / rmsField
            val hallQuantity    = HallCoefficient(hallValue, hallError, parameters, possibleParameters)
            val density         = hallQuantity.pow(-1) * (100.0).pow(-3) / 1.6e-19
            val densityQuantity = CarrierDensity(abs(density.value), density.error, parameters, possibleParameters)

            addQuantities(hallQuantity, densityQuantity)

        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {

        val halls  = findQuantities(HallCoefficient::class)
        val extras = LinkedList<Quantity<*>>()

        for (hall in halls) {

            val freq           = hall.getParameter(Frequency::class) ?: Frequency(0.0, 0.0)
            val conductivities = otherQuantities.filter { it is Conductivity && it.isCompatibleWith(hall, listOf(Frequency::class)) }.map { it as Conductivity }

            for (conductivity in conductivities) {

                val params   = parameters + conductivity.parameters.filter { c -> c::class !in parameters.map{ p -> p::class } } + freq
                val mobility = hall.value.absoluteValue * conductivity.value * 100.0 * 10000.0
                val error    = mobility * sqrt((hall.error / hall.value).pow(2) + (conductivity.error / conductivity.value).pow(2))
                extras      += HallMobility(mobility, error, params)

            }

        }

        return extras

    }

    override fun getPlot(): ACHallPlot? {

        return if (rotated.rowCount > 0) {
            ACHallPlot(data, rotated, faraday)
        } else {
            null
        }

    }

}