package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.interpolation.Interpolation
import jisa.results.Column
import jisa.results.ResultList
import jisa.results.ResultTable
import org.oefet.fetch.EPSILON
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Output
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class OutputData(data: ResultTable) : FetChData("Output Measurement", "Output", data, Images.getImage("output.png")) {

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

            val SDV = Column.ofDoubles("V")
            val SGV = Column.ofDoubles("G")
            val SDI = Column.ofDoubles("I")

            val fwd = ResultList(SDV, SGV, SDI)
            val bwd = ResultList(SDV, SGV, SDI)

            for ((gate, data) in data.split(SET_SG_VOLTAGE)) {


                for ((i, run) in data.directionalSplit(SET_SD_VOLTAGE).withIndex()) {

                    if ((i % 2) == 0) {
                        for (row in run) fwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])
                    } else {
                        for (row in run) bwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])
                    }

                }

            }

            val maxLinMobility = LinkedHashMap<Double, Double>()
            val maxSatMobility = LinkedHashMap<Double, Double>()

            for ((drain, data) in fwd.split(SDV)) {

                if (data.rowCount < 2) continue

                val iD = data[SDI]
                val vG = data[SGV]

                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = ((length / (capacitance * width)) * (linGrad.value(gate) / drain)).abs() * 1e4
                    val satMobility = (length / (width * capacitance)) * 1e4 * 2.0 * satGrad.value(gate).pow(2)
                    val params      = parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate, 0.0) + DoubleQuantity("Drain Voltage", Type.VOLTAGE, drain, 0.0)

                    if (linMobility.value.isFinite()) {
                        list += linMobility.toResult("Forward Linear Mobility", "μ_linf", Type.MOBILITY, params)
                        maxLinMobility[gate] = max(maxLinMobility[gate] ?: 0.0, linMobility.value)
                    }

                    if (satMobility.value.isFinite()) {
                        list += satMobility.toResult("Forward Saturation Mobility", "μ_satf", Type.MOBILITY, params)
                        maxSatMobility[gate] = max(maxSatMobility[gate] ?: 0.0, satMobility.value)
                    }

                }

            }

            for ((drain, data) in bwd.split(SDV)) {

                if (data.rowCount < 2) continue

                val vG = data.toList(SGV)
                val iD = data.toList(SDI)

                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = ((length / (capacitance * width)) * (linGrad.value(gate) / drain)).abs() * 1e4
                    val satMobility = (length / (width * capacitance)) * 1e4 * 2.0 * satGrad.value(gate).pow(2)
                    val params      = parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate, 0.0) + DoubleQuantity("Drain Voltage", Type.VOLTAGE, drain, 0.0)

                    if (linMobility.value.isFinite()) {
                        list += linMobility.toResult("Backwards Linear Mobility", "μ_linb", Type.MOBILITY, params)
                        maxLinMobility[gate] = max(maxLinMobility[gate] ?: 0.0, linMobility.value)
                    }

                    if (satMobility.value.isFinite()) {
                        list += satMobility.toResult("Backwards Saturation Mobility", "μ_satb", Type.MOBILITY, params)
                        maxSatMobility[gate] = max(maxSatMobility[gate] ?: 0.0, satMobility.value)
                    }

                }

            }

            for ((gate, max) in maxLinMobility) {
                list += Result("Max Linear Mobility", "max(μ_lin)", Type.MOBILITY, max, 0.0, parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate, 0.0))
            }

            for ((gate, max) in maxSatMobility) {
                list += Result("Max Saturation Mobility", "max(μ_sat)", Type.MOBILITY, max, 0.0, parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate, 0.0))
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list

    }

    override fun getDisplay(): Element {
        return OutputPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> = emptyList()

}