package org.oefet.fetch.data

import jisa.enums.Icon
import jisa.gui.Element
import jisa.maths.Range
import jisa.maths.fits.Fitting
import jisa.maths.matrices.RealMatrix
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.ACHallPlot
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.StringQuantity
import org.oefet.fetch.quant.Type
import kotlin.collections.iterator
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class ACHallData(data: ResultTable) : FetChData("AC Hall Measurement", "AC Hall", data, Icon.CIRCLES.blackImage) {

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


    override fun processData(data: ResultTable): List<Result> {

        val results = mutableListOf<Result>()

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

            val parameters = parameters + DoubleQuantity("B-Field Frequency", Type.FREQUENCY, frequency, 0.0)
            val rmsField   = data.mean(RMS_FIELD)
            val zero       = data.minByOrNull { it[SD_CURRENT].absoluteValue } ?: data[0]
            val voltages   = data.toMatrix(X_VOLTAGE, Y_VOLTAGE).transpose()
            val currents   = data.toList(SD_CURRENT)

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
                minVolts -= minVolts.getColMatrix(0)
            }

            results += Result("Hall Phase", "ϕ_H", Type.PHASE, minTheta, 0.0, parameters)

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

            val thickness = findDoubleParameter("Thickness", 30e-9)
            val gradient  = DoubleQuantity("Gradient", hallFit?.gradient ?: 0.0, hallFit?.gradientError ?: 0.0)

            // Calculate parameters from fitting
            val hallValue = (gradient * sign * thickness / rmsField).toResult("AC Hall Coefficient", "R_H", Type.HALL_COEFFICIENT, parameters)
            val density   = (hallValue.pow(-1.0) * (100.0).pow(-3) / 1.6e-19).toResult("AC Hall Carrier Density", "n_H", Type.CARRIER_DENSITY, parameters)

            results += hallValue
            results += density

        }

        return results

    }

    override fun getDisplay(): Element {

        return if (rotated.rowCount > 0) {
            ACHallPlot(data, rotated, faraday)
        } else {
            ACHallPlot(data)
        }

    }

    override fun generateHybrids(results: List<Result>): List<Result> {

        val halls  = results.filter { it.type == Type.HALL_COEFFICIENT && it.name == "AC Hall Coefficient" }
        val extras = mutableListOf<Result>()

        for (hall in halls) {

            val device         = hall.findParameter("Name", StringQuantity::class) ?: StringQuantity("Name", "dev", Type.UNKNOWN, "")
            val conductivities = results.filter { it.type == Type.CONDUCTIVITY && hall.overlappingParametersMatch(it) }

            for (conductivity in conductivities) {

                val params   = parameters + conductivity.parameters.filter { a -> a.name !in parameters.map{ b -> b.name } }
                val mobility = (hall * conductivity * 100.0 * 10000.0).toResult("Hall Mobility", "μ_H", Type.MOBILITY, params)

                extras += mobility

            }

        }

        return extras

    }


}