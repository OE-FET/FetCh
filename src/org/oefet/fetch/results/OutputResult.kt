package org.oefet.fetch.results

import jisa.maths.interpolation.Interpolation
import jisa.results.DoubleColumn
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.EPSILON
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Output
import org.oefet.fetch.quantities.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class OutputResult(data: ResultTable) : FetChResult("Output Measurement", "Output", Images.getImage("output.png"), data) {

    val SET_SD_VOLTAGE = data.findColumn(Output.SET_SD_VOLTAGE)
    val SET_SG_VOLTAGE = data.findColumn(Output.SET_SG_VOLTAGE)
    val SD_VOLTAGE     = data.findColumn(Output.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Output.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Output.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Output.SG_CURRENT)
    val FPP_1          = data.findColumn(Output.FPP_1)
    val FPP_2          = data.findColumn(Output.FPP_2)
    val TEMPERATURE    = data.findColumn(Output.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Output.GROUND_CURRENT)

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

            val SDV = DoubleColumn("V")
            val SGV = DoubleColumn("G")
            val SDI = DoubleColumn("I")

            val fwd = ResultList(SDV, SGV, SDI)
            val bwd = ResultList(SDV, SGV, SDI)

            for ((gate, data) in data.split(SET_SG_VOLTAGE)) {

                val fb = data.directionalSplit { it[SET_SD_VOLTAGE] }

                for (row in fb[0]) fwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])
                for (row in fb[1]) bwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])

            }

            val maxLinMobility = LinkedHashMap<Double, Double>()
            val maxSatMobility = LinkedHashMap<Double, Double>()

            for ((drain, data) in fwd.split(SDV)) {

                if (data.rowCount < 2) continue

                val vG      = data.toList(SGV)
                val iD      = data.toList(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val params      = parameters + Gate(gate, 0.0) + Drain(drain, 0.0)

                    if (linMobility.isFinite()) {
                        quantities           += FwdLinMobility(linMobility, 0.0, params, possibleParameters)
                        maxLinMobility[gate]  = max(maxLinMobility[gate] ?: 0.0, linMobility)
                    }

                    if (satMobility.isFinite()) {
                        quantities           += FwdSatMobility(satMobility, 0.0, params, possibleParameters)
                        maxSatMobility[gate]  = max(maxSatMobility[gate] ?: 0.0, satMobility)
                    }

                }

            }

            for ((drain, data) in bwd.split(SDV)) {

                if (data.rowCount < 2) continue

                val vG      = data.toMatrix(SGV)
                val iD      = data.toMatrix(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val params      = ArrayList(parameters)
                    params         += Gate(gate, 0.0)
                    params         += Drain(drain, 0.0)

                    if (linMobility.isFinite()) {
                        quantities += BwdLinMobility(
                            linMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxLinMobility[gate] = max(maxLinMobility[gate] ?: 0.0, linMobility)
                    }
                    if (satMobility.isFinite()) {
                        quantities += BwdSatMobility(
                            satMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxSatMobility[gate] = max(maxSatMobility[gate] ?: 0.0, satMobility)
                    }

                }

            }

            for ((gate, max) in maxLinMobility) {
                quantities += MaxLinMobility(max, 0.0, parameters + Gate(gate, 0.0))
            }

            for ((gate, max) in maxSatMobility) {
                quantities += MaxSatMobility(max, 0.0, parameters + Gate(gate, 0.0))
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> = emptyList()

}