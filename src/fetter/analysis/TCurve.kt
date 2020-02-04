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

class TCurve(val length: Double, val width: Double, val capacitance: Double, val data: ResultTable) {

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

        fwdMob.clear()
        bwdMob.clear()

        try {

            for ((drain, data) in data.split(SET_SD)) {

                val fb = data.splitTwoWaySweep { it[SET_SG] }

                val function: Function
                val gradFwd:  Function?
                val gradBwd:  Function?

                if (abs(drain) < data.getMax { abs(it[SET_SG]) }) {

                    gradFwd = if (fb.forward.numRows > 1) {
                        Interpolation.interpolate1D(
                            fb.forward.getColumns(SET_SG),
                            fb.forward.getColumns(SD_CURRENT).map { x -> abs(x) }
                        ).derivative()
                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {

                        Interpolation.interpolate1D(
                            fb.backward.getColumns(SET_SG),
                            fb.backward.getColumns(SD_CURRENT).map { x -> abs(x) }
                        ).derivative()

                    } else null

                    function = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }

                } else {

                    gradFwd = if (fb.forward.numRows > 1) Interpolation.interpolate1D(
                        fb.forward.getColumns(SET_SG),
                        fb.forward.getColumns(SD_CURRENT).map { x -> sqrt(abs(x)) }
                    ).derivative() else null

                    gradBwd = if (fb.backward.numRows > 1) Interpolation.interpolate1D(
                        fb.backward.getColumns(SET_SG),
                        fb.backward.getColumns(SD_CURRENT).map { x -> sqrt(abs(x)) }
                    ).derivative() else null

                    function = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    if (gradFwd != null) fwdMob.addData(gate, drain, function.value(gradFwd.value(gate)))
                    if (gradBwd != null) bwdMob.addData(gate, drain, function.value(gradBwd.value(gate)))

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