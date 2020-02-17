package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.interpolation.Interpolation
import org.oefet.fetch.*
import org.oefet.fetch.analysis.Curve.Companion.NON_USER_VARIABLES
import kotlin.Exception
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max
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
    override val fppSeparation: Double
    override val channelThickness: Double
    override val dielectricThickness: Double
    override val permittivity: Double

    init {

        if (results.getAttribute("type") != "output") throw Exception("That file does not contain an output measurement")

        val vars = results.attributes

        for (name in NON_USER_VARIABLES) {
            if (!vars.containsKey(name)) {
                throw Exception("That file is missing information.")
            }
        }

        var temp: Double? = null

        for ((name, value) in vars) {

            val endsWithK        = value.endsWith(" K")
            val isNonUser        = name in NON_USER_VARIABLES
            val isDouble         = value.toDoubleOrNull() != null
            val isDoubleWithUnit = value.split(" ")[0].toDoubleOrNull() != null

            when {

                endsWithK        -> temp = value.removeSuffix(" K").toDouble()
                isNonUser        -> {}
                isDouble         -> variables[name] = value.toDouble()
                isDoubleWithUnit -> variables[name] = value.split(" ")[0].toDouble()

            }

        }

        temperature         = temp ?: results.getMean(TEMPERATURE)
        name                = results.getAttribute("name")
        length              = results.getAttribute("length").removeSuffix(" m").toDouble()
        width               = results.getAttribute("width").removeSuffix(" m").toDouble()
        fppSeparation       = results.getAttribute("fppSeparation").removeSuffix(" m").toDouble()
        channelThickness    = results.getAttribute("channelThickness").removeSuffix(" m").toDouble()
        dielectricThickness = results.getAttribute("dielectricThickness").removeSuffix(" m").toDouble()
        permittivity        = results.getAttributeDouble("dielectricPermittivity")

    }

    fun calculate() {

        val capacitance = permittivity * EPSILON / dielectricThickness
        calculated = true

        val dataCopy = results.filteredCopy { true }

        fwdMobTable.clear()
        bwdMobTable.clear()

        try {

            val fwd = ResultList("V", "G", "I")
            val bwd = ResultList("V", "G", "I")
            val SDV = 0
            val SGV = 1
            val SDI = 2

            for ((gate, data) in dataCopy.split(SET_SG)) {

                val fb = data.splitTwoWaySweep { it[SET_SD] }

                for (row in fb.forward) fwd.addData(row[SET_SD], gate, row[SD_CURRENT])
                for (row in fb.backward) bwd.addData(row[SET_SD], gate, row[SD_CURRENT])

            }

            for ((drain, data) in fwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val mobility    = max(linMobility, satMobility)

                    if (mobility.isFinite()) fwdMobTable.addData(gate, drain, mobility)

                }

            }

            for ((drain, data) in bwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val mobility    = max(linMobility, satMobility)

                    if (mobility.isFinite()) bwdMobTable.addData(gate, drain, mobility)

                }

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val SG_VOLTAGE = 0
        const val SD_VOLTAGE = 1
        const val MOBILITY   = 2
    }

}