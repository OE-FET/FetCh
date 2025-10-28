package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.interpolation.Interpolation
import jisa.results.ResultTable
import org.oefet.fetch.EPSILON
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Transfer
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TransferData(data: ResultTable) : FetChData("Transfer Measurement", "Transfer", data, Images.getImage("transfer.png")) {

    val SET_SD_VOLTAGE = data.findColumn(Transfer.SET_SD_VOLTAGE)
    val SET_SG_VOLTAGE = data.findColumn(Transfer.SET_SG_VOLTAGE)
    val SD_VOLTAGE     = data.findColumn(Transfer.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Transfer.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Transfer.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Transfer.SG_CURRENT)
    val FPP_1          = data.findColumn(Transfer.FPP_1)
    val FPP_2          = data.findColumn(Transfer.FPP_2)
    val TEMPERATURE    = data.findColumn(Transfer.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Transfer.GROUND_CURRENT)

    override fun processData(data: ResultTable): List<Result> {

        val list = mutableListOf<Result>()

        val permittivity = findDoubleParameter("Dielectric Permittivity", 1.0)
        val dielectric   = findDoubleParameter("Dielectric Thickness", 400e-9)
        val length       = findDoubleParameter("Length", 300e-6)
        val width        = findDoubleParameter("Width", 100e-6)
        val separation   = findDoubleParameter("FPP Separation", 100e-6)
        val thickness    = findDoubleParameter("Thickness", 30e-9)

        val capacitance = permittivity * EPSILON / dielectric

        try {

            // Split by drain voltage
            for ((drain, drainTable) in data.split(SET_SD_VOLTAGE)) {

                // Separate forwards and backwards sweeps
                for ((index, subTable) in drainTable.directionalSplit(SET_SG_VOLTAGE).withIndex()) {

                    val vG        = subTable.toList(SET_SG_VOLTAGE)                                                          // Extract source-gate voltages as a list
                    val iD        = subTable.toList(SD_CURRENT)                                                              // Extract drain currents as a list
                    val isForward = (index % 2) == 0                                                                         // Treat all even-indexed sweeps as forwards
                    val params    = (parameters + DoubleQuantity("Drain Voltage", Type.VOLTAGE, drain, 0.0)).toMutableList() // Build parameters
                    val fpp1      = subTable.all { it[FPP_1].isFinite() && it[FPP_2].isNaN() }
                    val fpp2      = subTable.all { it[FPP_2].isFinite() && it[FPP_1].isNaN() }
                    val fppD      = subTable.all { it[FPP_1].isFinite() && it[FPP_2].isFinite() }

                    // If we have an FPP measurement, then we need to extrapolate what the true SD-Voltage value is
                    val vF = when {

                        fpp1 -> subTable.toList { (length.value / separation.value) * it[FPP_1] }
                        fpp2 -> subTable.toList { (length.value / separation.value) * it[FPP_2] }
                        fppD -> subTable.toList { (length.value / separation.value) * (it[FPP_2] - it[FPP_1]) }
                        else -> subTable.toList(SD_VOLTAGE)

                    }

                    // Perform different calculation depending on whether we are in linear or saturation regime
                    list += if ((vF.maxOfOrNull { abs(it) } ?: 0.0) < (vG.maxOfOrNull { abs(it) } ?: 0.0)) { // Linear regime

                        val gradient = Interpolation.interpolateSmooth(vG, iD.map { abs(it) }).derivative()
                        val dvF      = Interpolation.interpolateSmooth(vG, vF).derivative()
                        val mobility = vG.zip(vF).map { (g, d) -> ((length / (capacitance * width)) * (gradient.value(g) / (dvF.value(g) * (g - 2.0 * d) + d))).abs() * 1e4 }
                        val points   = vG.zip(mobility)

                        // Add the calculated mobilities to the overall list of determined quantities
                        when {
                            isForward -> points.map { (g, m) -> m.toResult("Forward Linear Mobility", "μ_linf", Type.MOBILITY, params + DoubleQuantity("Gate Voltage", Type.VOLTAGE, g)) }
                            else      -> points.map { (g, m) -> m.toResult("Backwards Linear Mobility", "μ_linb", Type.MOBILITY, params + DoubleQuantity("Gate Voltage", Type.VOLTAGE, g)) }
                        }

                    } else { // Saturation regime

                        val gradient = Interpolation.interpolateSmooth(vG, iD.map { sqrt(abs(it)) }).derivative()
                        val mobility = vG.map { v -> (length / (width * capacitance)) * gradient.value(v).pow(2) * 1e4 * 2.0  }
                        val points   = vG.zip(mobility)

                        // Add the calculated mobilities to the overall list of determined quantities
                        when {
                            isForward -> points.map { (g, m) -> m.toResult("Forward Saturation Mobility", "μ_satf", Type.MOBILITY, params + DoubleQuantity("Gate Voltage", Type.VOLTAGE, g)) }
                            else      -> points.map { (g, m) -> m.toResult("Backwards Saturation Mobility", "μ_satb", Type.MOBILITY, params + DoubleQuantity("Gate Voltage", Type.VOLTAGE, g)) }
                        }

                    }

                }

            }

            // Find the maximum values for linear and saturation mobilities
            val maxLinMobility = list.filter { it.name.contains("Linear") && it.type == Type.MOBILITY } .maxByOrNull { abs(it.value) }
            val maxSatMobility = list.filter { it.name.contains("Saturation") && it.type == Type.MOBILITY } .maxByOrNull { abs(it.value) }

            // If they were found, add them as quantities
            if (maxLinMobility != null) {
                list += maxLinMobility.toResult("Max Linear Mobility", "max(μ_lin)", Type.MOBILITY, parameters)
            }

            if (maxSatMobility != null) {
                list += maxSatMobility.toResult("Max Saturation Mobility", "max(μ_sat)", Type.MOBILITY, parameters)
            }

        } catch (e: Exception) {
            // If something goes wrong, make sure the exception is output to the standard error stream
            e.printStackTrace()
        }

        return list
    }

    override fun getDisplay(): Element {
        TODO("Not yet implemented")
    }

    override fun generateHybrids(results: List<Result>): List<Result> {
        TODO("Not yet implemented")
    }

}