package fetter.analysis

import fetter.gui.SD_CURRENT
import fetter.gui.SET_SD
import fetter.gui.SET_SG
import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.functions.Function
import jisa.maths.interpolation.Interpolation
import java.lang.Exception
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OCurve(val length: Double, val width: Double, val capacitance: Double, val data: ResultTable) {

    val fwdMob = ResultList(
        Col("SG Voltage", "V"),
        Col("SD Voltage", "V"),
        Col("Mobility", "cm^2/Vs")
    )

    val bwdMob = ResultList(
        Col("SG Voltage", "V"),
        Col("SD Voltage", "V"),
        Col("Mobility", "cm^2/Vs")
    )

    var calculated = false

    fun calculate() {

        calculated = true

        val dataCopy = data.filteredCopy { true }

        fwdMob.clear()
        bwdMob.clear()

        try {

            val fwd = ResultList("V", "G", "I")
            val bwd = ResultList("V", "G", "I")

            for ((gate, data) in dataCopy.split(SET_SG)) {

                val fb = data.splitTwoWaySweep { it[SET_SD] }

                for (row in fb.forward)  fwd.addData(row[SET_SD], gate, row[SD_CURRENT])
                for (row in fb.backward) bwd.addData(row[SET_SD], gate, row[SD_CURRENT])

            }

            for ((drain, data) in fwd.split(0)) {

                if (data.numRows < 2) continue

                val linFunction = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }
                val linGrad     = Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> abs(x) }).derivative()
                val satGrad     = Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> sqrt(abs(x)) }).derivative()
                val satFunction = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    val linMobility = linFunction.value(linGrad.value(gate))
                    val satMobility = satFunction.value(satGrad.value(gate))

                    val mobility = if (linMobility > satMobility) linMobility else satMobility

                    if (mobility.isFinite()) fwdMob.addData(
                        gate,
                        drain,
                        mobility
                    )

                }

            }

            for ((drain, data) in bwd.split(0)) {

                if (data.numRows < 2) continue

                val linFunction = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }
                val linGrad     = Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> abs(x) }).derivative()
                val satGrad     = Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> sqrt(abs(x)) }).derivative()
                val satFunction = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    val linMobility = linFunction.value(linGrad.value(gate))
                    val satMobility = satFunction.value(satGrad.value(gate))

                    val mobility = if (linMobility > satMobility) linMobility else satMobility

                    if (mobility.isFinite()) bwdMob.addData(
                        gate,
                        drain,
                        mobility
                    )

                }

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val SG_VOLTAGE = 0
        const val SD_VOLTAGE = 1
        const val MOBILITY = 2
    }

}