package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.fits.Fitting
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.DCHallPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.DCHall
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.StringQuantity
import org.oefet.fetch.quant.Type

class DCHallData(data: ResultTable) : FetChData("DC Hall Measurement", "DC Hall", data, Images.getImage("hall.png")) {

    val SET_SD_CURRENT = data.findColumn(DCHall.SET_SD_CURRENT)
    val SD_CURRENT     = data.findColumn(DCHall.SD_CURRENT)
    val SET_SG_VOLTAGE = data.findColumn(DCHall.SET_SG_VOLTAGE)
    val FIELD          = data.findColumn(DCHall.FIELD)
    val HALL_1         = data.findColumn(DCHall.HALL_1)
    val HALL_2         = data.findColumn(DCHall.HALL_2)
    val FPP_1          = data.findColumn(DCHall.HALL_3)
    val FPP_2          = data.findColumn(DCHall.HALL_4)
    val TEMPERATURE    = data.findColumn(DCHall.TEMPERATURE)

    override fun processData(data: ResultTable): List<Result> {

        val list = mutableListOf<Result>()

        val thickness  = findParameter("Thickness", DoubleQuantity::class) ?: DoubleQuantity("Thickness", Type.DISTANCE, 30e-9, 0.0)
        val separation = findParameter("FPP Separation", DoubleQuantity::class) ?: DoubleQuantity("FPP Separation", Type.DISTANCE, 100e-6, 0.0)
        val width      = findParameter("Width", DoubleQuantity::class) ?: DoubleQuantity("Width", Type.DISTANCE, 50.0, 0.0)

        // Split data-up into separate tables based on gate voltage
        for ((gate, data) in data.split(SET_SG_VOLTAGE)) {

            val parameters = parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate, 0.0)

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

                val CURRENT   = Column.ofDoubles("Current")
                val GRADIENT  = Column.ofDoubles("Gradient")
                val ERROR     = Column.ofDoubles("Error")
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

                        gradients.mapRow(
                            CURRENT  to current,
                            GRADIENT to gradFit.gradient,
                            ERROR    to gradFit.gradientError
                        )

                    }

                }

                // Fit the gradients of the V_H vs B fits to their corresponding value of I
                val currFit  = Fitting.linearFit(gradients, CURRENT, GRADIENT)
                val gradient = DoubleQuantity("Gradient", Type.UNKNOWN, currFit.gradient, currFit.gradientError)

                if (currFit != null) {

                    // If the fit worked, then we can finally calculate RH (and thus charge carrier density)
                    val hall    = (gradient * thickness).toResult("DC Hall Coefficient", "R_H", Type.HALL_COEFFICIENT, parameters)
                    val density = hall.pow(-1.0).toResult("Charge Carrier Density", "n_H", Type.CARRIER_DENSITY, parameters)

                    // Add both the Hall and carrier density quantities to the list of calculated quantities
                    list += hall
                    list += density

                }

            } else { // If only one parameter was swept, then just fit VH to I*B/t

                val hallVoltage = if (usedHVM1 && usedHVM2) {
                    data.toMatrix(HALL_2) - data.toMatrix(HALL_1)
                } else if (usedHVM1) {
                    data.toMatrix(HALL_1)
                } else {
                    data.toMatrix(HALL_2)
                }

                val xValues = data.toMatrix(SD_CURRENT).elementMultiply(data.toMatrix(FIELD)) / thickness.value
                val fit     = Fitting.linearFit(xValues, hallVoltage)
                val hall    = DoubleQuantity("Gradient", Type.UNKNOWN, fit.gradient, fit.gradientError).toResult("DC Hall Coefficient", "R_H", Type.HALL_COEFFICIENT, parameters)
                val density = hall.pow(-1.0).toResult("Charge Carrier Density", "n_H", Type.CARRIER_DENSITY, parameters)

                // Add both the Hall and carrier density quantities to the list of calculated quantities
                list += hall
                list += density

            }

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
                    val fit     = DoubleQuantity("Gradient", Type.UNKNOWN, condFit.gradient, condFit.gradientError)
                    val mCond   = (fit * separation / (width * thickness) / 100.0).toResult("DC Magnetoconductivity", "σ(B)", Type.CONDUCTIVITY, parameters + DoubleQuantity("B Field", Type.B_FIELD, field, 0.0))

                    // Add the magneto-conductivity value to the list of calculated quantities
                    list += mCond

                }

            }

        }

        return list

    }

    override fun getDisplay(): Element {
        return DCHallPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> {

        val halls  = results.filter { it.type == Type.HALL_COEFFICIENT && it.name == "DC Hall Coefficient" }
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