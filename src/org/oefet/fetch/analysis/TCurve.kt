package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.functions.Function
import jisa.maths.interpolation.Interpolation
import org.oefet.fetch.*
import org.oefet.fetch.analysis.Curve.Companion.NON_USER_VARIABLES
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TCurve(private val results: ResultTable) : Curve {

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

        if (results.getAttribute("type") != "transfer") throw Exception("That file does not contain a transfer measurement")

        val vars = results.attributes

        for (name in NON_USER_VARIABLES) {
            if (!vars.containsKey(name)) {
                throw Exception("That file is missing information.")
            }
        }

        var temp: Double? = null

        for ((name, value) in vars) {

            val endsWithK         = value.endsWith(" K")
            val isNonUser         = name in NON_USER_VARIABLES
            val isDouble          = value.toDoubleOrNull() != null
            val isDoubleWithUnit  = value.split(" ")[0].toDoubleOrNull() != null

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

        fwdMobTable.clear()
        bwdMobTable.clear()

        val dataCopy = results.filteredCopy { true }

        try {

            for ((drain, data) in dataCopy.split(SET_SD)) {

                val fb = data.splitTwoWaySweep { it[SET_SG] }

                val function: Function
                val gradFwd: Function?
                val gradBwd: Function?

                val vGFwd = fb.forward.getColumns(SET_SG)
                val iDFwd = fb.forward.getColumns(SD_CURRENT)
                val vGBwd = fb.backward.getColumns(SET_SG)
                val iDBwd = fb.backward.getColumns(SD_CURRENT)

                if (abs(drain) < data.getMax { abs(it[SET_SG]) }) {

                    gradFwd = if (fb.forward.numRows > 1) {

                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> abs(x) }).derivative()

                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {

                        Interpolation.interpolate1D(vGBwd, iDFwd.map { x -> abs(x) }).derivative()

                    } else null

                    function = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }

                } else {

                    gradFwd = if (fb.forward.numRows > 1) {
                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {
                        Interpolation.interpolate1D(vGBwd, iDBwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    function = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    if (gradFwd != null) fwdMobTable.addData(gate, drain, function.value(gradFwd.value(gate)))
                    if (gradBwd != null) bwdMobTable.addData(gate, drain, function.value(gradBwd.value(gate)))

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