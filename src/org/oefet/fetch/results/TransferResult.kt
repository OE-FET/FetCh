package org.oefet.fetch.results

import jisa.maths.interpolation.Interpolation
import jisa.results.ResultTable
import org.oefet.fetch.EPSILON
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Transfer
import org.oefet.fetch.quantities.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class for handling and processing transfer curve data, extracting linear and saturation mobilities as a function
 * of gate voltage.
 */
class TransferResult(data: ResultTable) : FetChResult("Transfer Measurement", "Transfer", Images.getImage("transfer.png"), data) {

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
        Permittivity::class,
        Gate::class,
        Drain::class
    )

    init {

        // Calculate the capacitance (per unit area) to use for calculations
        val capacitance = permittivity * EPSILON / dielectric

        try {

            // Split by drain voltage
            for ((drain, drainTable) in data.split(SET_SD_VOLTAGE)) {

                // Separate forwards and backwards sweeps
                for ((index, subTable) in drainTable.directionalSplit(SET_SG_VOLTAGE).withIndex()) {

                    val vG        = subTable.toList(SET_SG_VOLTAGE)                  // Extract source-gate voltages as a list
                    val iD        = subTable.toList(SD_CURRENT)                      // Extract drain currents as a list
                    val isLinear  = abs(drain) < (vG.maxOfOrNull { abs(it) } ?: 0.0) // Assume linearity if |V_SD| < max(|V_SG|)
                    val isForward = (index % 2) == 0                                 // Treat all even-indexed sweeps as forwards
                    val params    = parameters + Drain(drain, 0.0)                   // Build parameters

                    // Perform different calculation depending on whether we are in linear or saturation regime
                    when {

                        isLinear -> { // Linear regime

                            val gradient = Interpolation.interpolateSmooth(vG, iD.map { abs(it) }).derivative()
                            val mobility = vG.map { v -> 1e4 * abs((length / (capacitance * width)) * (gradient.value(v) / drain)) }
                            val points   = vG.zip(mobility)

                            // Add the calculated mobilities to the overall list of determined quantities
                            quantities += when {
                                isForward -> points.map { (g, m) -> FwdLinMobility(m, 0.0 , params + Gate(g, 0.0), possibleParameters) }
                                else      -> points.map { (g, m) -> BwdLinMobility(m, 0.0 , params + Gate(g, 0.0), possibleParameters) }
                            }

                        }

                        else     -> { // Saturation regime

                            val gradient = Interpolation.interpolateSmooth(vG, iD.map{ sqrt(abs(it)) }).derivative()
                            val mobility = vG.map { v -> 1e4 * 2.0 * gradient.value(v).pow(2) * length / (width * capacitance) }
                            val points = vG.zip(mobility)

                            // Add the calculated mobilities to the overall list of determined quantities
                            quantities += when {
                                isForward -> points.map { (g, m) -> FwdSatMobility(m, 0.0 , params + Gate(g, 0.0), possibleParameters) }
                                else      -> points.map { (g, m) -> BwdSatMobility(m, 0.0 , params + Gate(g, 0.0), possibleParameters) }
                            }

                        }

                    }

                }

            }

            // Find the maximum values for linear and saturation mobilities
            val maxLinMobility = quantities.filterIsInstance<LinMobility>().maxByOrNull { abs(it.value) }
            val maxSatMobility = quantities.filterIsInstance<SatMobility>().maxByOrNull { abs(it.value) }

            // If they were found, add them as quantities
            if (maxLinMobility != null) {
                quantities += MaxLinMobility(maxLinMobility.value, maxLinMobility.error, parameters, possibleParameters);
            }

            if (maxSatMobility != null) {
                quantities += MaxSatMobility(maxSatMobility.value, maxSatMobility.error, parameters, possibleParameters);
            }

        } catch (e: Exception) {
            // If something goes wrong, make sure the exception is output to the standard error stream
            e.printStackTrace()
        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> = emptyList()

}