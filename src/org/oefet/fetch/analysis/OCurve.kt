package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.functions.Function
import jisa.maths.interpolation.Interpolation
import org.oefet.fetch.*
import java.util.*
import kotlin.Exception
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OCurve(private val results: ResultTable) : Curve {

    val fwdMobTable = ResultList(
        Col("SG Voltage", "V"),
        Col("SD Voltage", "V"),
        Col("Mobility", "cm^2/Vs")
    )

    val bwdMobTable = ResultList(
        Col("SG Voltage", "V"),
        Col("SD Voltage", "V"),
        Col("Mobility", "cm^2/Vs")
    )

    override val fwdMob: ResultTable
        get() {
            if (!calculated) calculate()
            return fwdMobTable
        }

    override val bwdMob: ResultTable
        get() {
            if (!calculated) calculate()
            return bwdMobTable
        }

    override val data: ResultTable get() = results

    private var calculated = false

    override val temperature: Double
    override val variables = HashMap<String, Double>()

    override val name: String
    override val length: Double
    override val width: Double
    override val thick: Double
    override val permittivity: Double

    init {

        if (results.getAttribute("type") != "output") throw Exception("That file does not contain an output measurement")

        var temp: Double? = null

        for ((name, value) in results.attributes) {

            val endsWithK         = value.endsWith(" K")
            val isNonUser         = name in Curve.NON_USER_VARIABLES
            val isDouble          = value.toDoubleOrNull() != null
            val isDoubleWithUnit  = value.split(" ")[0].toDoubleOrNull() != null

            when {

                endsWithK        -> temp = value.removeSuffix(" K").toDouble()
                isNonUser        -> {}
                isDouble         -> variables[name] = value.toDouble()
                isDoubleWithUnit -> variables[name] = value.split(" ")[0].toDouble()

            }

        }

        if (temp == null) {
            temp = results.getMean(TEMPERATURE)
        }

        temperature = temp

        name         = results.getAttribute("name")
        length       = results.getAttribute("length").removeSuffix(" m").toDouble()
        width        = results.getAttribute("width").removeSuffix(" m").toDouble()
        thick        = results.getAttribute("dielectricThickness").removeSuffix(" m").toDouble()
        permittivity = results.getAttributeDouble("dielectricPermittivity")

    }

    fun calculate() {

        val capacitance = permittivity * EPSILON / thick
        calculated = true

        val dataCopy = results.filteredCopy { true }

        fwdMobTable.clear()
        bwdMobTable.clear()

        try {

            val fwd = ResultList("V", "G", "I")
            val bwd = ResultList("V", "G", "I")

            for ((gate, data) in dataCopy.split(SET_SG)) {

                val fb = data.splitTwoWaySweep { it[SET_SD] }

                for (row in fb.forward) fwd.addData(row[SET_SD], gate, row[SD_CURRENT])
                for (row in fb.backward) bwd.addData(row[SET_SD], gate, row[SD_CURRENT])

            }

            for ((drain, data) in fwd.split(0)) {

                if (data.numRows < 2) continue

                val linGrad = Interpolation
                    .interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> abs(x) })
                    .derivative()

                val satGrad = Interpolation
                    .interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> sqrt(abs(x)) })
                    .derivative()

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)

                    val mobility = if (linMobility > satMobility) linMobility else satMobility

                    if (mobility.isFinite()) fwdMobTable.addData(
                        gate,
                        drain,
                        mobility
                    )

                }

            }

            for ((drain, data) in bwd.split(0)) {

                if (data.numRows < 2) continue

                val linFunction = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }
                val linGrad =
                    Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> abs(x) }).derivative()
                val satGrad =
                    Interpolation.interpolate1D(data.getColumns(1), data.getColumns(2).map { x -> sqrt(abs(x)) })
                        .derivative()
                val satFunction = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    val linMobility = linFunction.value(linGrad.value(gate))
                    val satMobility = satFunction.value(satGrad.value(gate))

                    val mobility = if (linMobility > satMobility) linMobility else satMobility

                    if (mobility.isFinite()) bwdMobTable.addData(
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