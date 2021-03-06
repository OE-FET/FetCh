package org.oefet.fetch.results

import jisa.experiment.ResultTable
import jisa.maths.functions.Function
import jisa.maths.interpolation.Interpolation
import org.oefet.fetch.EPSILON
import org.oefet.fetch.gui.elements.TransferPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Transfer
import org.oefet.fetch.quantities.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class TransferResult(data: ResultTable, extraParams: List<Quantity> = emptyList()) :
    FetChResult("Transfer Measurement", "Transfer", Images.getImage("transfer.png"), data, extraParams) {

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

        val capacitance = permittivity * EPSILON / dielectric

        try {

            var maxLinMobility = 0.0
            var maxSatMobility = 0.0

            for ((drain, data) in data.split(SET_SD_VOLTAGE)) {

                val fb = data.splitTwoWaySweep { it[SET_SG_VOLTAGE] }

                val function: (Double) -> Double
                val gradFwd:  Function?
                val gradBwd:  Function?

                val vGFwd  = fb.forward.getColumns(SET_SG_VOLTAGE)
                val iDFwd  = fb.forward.getColumns(SD_CURRENT)
                val vGBwd  = fb.backward.getColumns(SET_SG_VOLTAGE)
                val iDBwd  = fb.backward.getColumns(SD_CURRENT)
                val linear = abs(drain) < data.getMax { abs(it[SET_SG_VOLTAGE]) }

                if (linear) {

                    gradFwd = if (fb.forward.numRows > 1) {

                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> abs(x) }).derivative()

                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {

                        Interpolation.interpolate1D(vGBwd, iDBwd.map { x -> abs(x) }).derivative()

                    } else null

                    function = { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }

                } else {

                    gradFwd = if (fb.forward.numRows > 1) {
                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {
                        Interpolation.interpolate1D(vGBwd, iDBwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    function = { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                }

                for (gate in data.getUniqueValues(SET_SG_VOLTAGE).sorted()) {

                    val params = ArrayList(parameters)
                    params    += Gate(gate, 0.0)
                    params    += Drain(drain, 0.0)

                    if (gradFwd != null) quantities += if (linear) {
                        maxLinMobility = max(maxLinMobility, function(gradFwd.value(gate)))
                        FwdLinMobility(function(gradFwd.value(gate)), 0.0, params, possibleParameters)
                    } else {
                        maxSatMobility = max(maxSatMobility, function(gradFwd.value(gate)))
                        FwdSatMobility(function(gradFwd.value(gate)), 0.0, params, possibleParameters)
                    }

                    if (gradBwd != null) quantities += if (linear) {
                        maxLinMobility = max(maxLinMobility, function(gradBwd.value(gate)))
                        BwdLinMobility(function(gradBwd.value(gate)), 0.0, params, possibleParameters)
                    } else {
                        maxSatMobility = max(maxSatMobility, function(gradBwd.value(gate)))
                        BwdSatMobility(function(gradBwd.value(gate)), 0.0, params, possibleParameters)
                    }

                }

            }

            if (maxLinMobility > 0) addQuantity(MaxLinMobility(maxLinMobility, 0.0, parameters, possibleParameters))
            if (maxSatMobility > 0) addQuantity(MaxSatMobility(maxSatMobility, 0.0, parameters, possibleParameters))

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> = emptyList()

}