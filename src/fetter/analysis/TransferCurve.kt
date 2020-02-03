package fetter.analysis

import fetter.gui.SD_CURRENT
import fetter.gui.SET_SD
import fetter.gui.SET_SG
import fetter.gui.SG_VOLTAGE
import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.interpolation.Interpolation
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TransferCurve(val length: Double, val width: Double, val capacitance: Double, val data: ResultTable) {

    val results: ResultTable

    init {

        val satFwd = ResultList(Col("VG", "V"), Col("ISD", "A"), Col("sqrt(ISD)", "A^1/2") { sqrt(it[1]) })
        val linFwd = ResultList(Col("VG", "V"), Col("ISD", "A"), Col("sqrt(ISD)", "A^1/2") { sqrt(it[1]) })

        val satBwd = ResultList(Col("VG", "V"), Col("ISD", "A"), Col("sqrt(ISD)", "A^1/2") { sqrt(it[1]) })
        val linBwd = ResultList(Col("VG", "V"), Col("ISD", "A"), Col("sqrt(ISD)", "A^1/2") { sqrt(it[1]) })

        val drainLin = data.getMin { abs(it[SET_SD]) }
        val drainSat = data.getMax { abs(it[SET_SD]) }

        var lastSG: Double? = null
        for (row in data.filteredCopy { abs(it[SET_SD]) == drainLin }) {

            if (lastSG == null || row[SET_SG] < lastSG) {
                linFwd.addData(row[SET_SG], abs(row[SD_CURRENT]))
            } else {
                linBwd.addData(row[SET_SG], abs(row[SD_CURRENT]))
            }

            lastSG = row[SET_SG]

        }

        lastSG = null
        for (row in data.filteredCopy { abs(it[SET_SD]) == drainSat }) {

            if (lastSG == null || row[SET_SG] < lastSG) {
                satFwd.addData(row[SG_VOLTAGE], abs(row[SD_CURRENT]))
            } else {
                satBwd.addData(row[SG_VOLTAGE], abs(row[SD_CURRENT]))
            }

            lastSG = row[SET_SG]

        }


        val linDerivFwd = if (linFwd.numRows > 0) Interpolation.interpolate1D(linFwd.getColumns(0), linFwd.getColumns(1)).derivative() else null
        val satDerivFwd = if (satFwd.numRows > 0) Interpolation.interpolate1D(satFwd.getColumns(0), satFwd.getColumns(2)).derivative() else null
        val linDerivBwd = if (linBwd.numRows > 0) Interpolation.interpolate1D(linBwd.getColumns(0), linBwd.getColumns(1)).derivative() else null
        val satDerivBwd = if (satBwd.numRows > 0) Interpolation.interpolate1D(satBwd.getColumns(0), satBwd.getColumns(2)).derivative() else null

        results = ResultList(
            Col("SG Voltage", "V"),
            Col("Linear Mobility (Forward)", "cm^2/Vs"),
            Col("Linear Mobility (Backward)", "cm^2/Vs"),
            Col("Saturation Mobility (Forward)", "cm^2/Vs"),
            Col("Saturation Mobility (Backward)", "cm^2/Vs")
        )

        for (gate in data.getUniqueValues(SET_SG).sorted()) {

            val linFwdMob: Double = if (linDerivFwd != null) {
                1e4 * abs((length / (capacitance * width)) * (linDerivFwd.value(gate) / drainLin))
            } else {
                0.0
            }

            val linBwdMob: Double = if (linDerivBwd != null) {
                1e4 * abs((length / (capacitance * width)) * (linDerivBwd.value(gate) / drainLin))
            } else {
                0.0
            }

            val satFwdMob: Double = if (satDerivFwd != null) {
                1e4 * 2.0 * satDerivFwd.value(gate).pow(2) * length / (width * capacitance)
            } else {
                0.0
            }

            val satBwdMob: Double = if (satDerivBwd != null) {
                1e4 * 2.0 * satDerivBwd.value(gate).pow(2) * length / (width * capacitance)
            } else {
                0.0
            }

            results.addData(gate, linFwdMob, linBwdMob, satFwdMob, satBwdMob)

        }

    }

}

fun main() {

    val results = ResultList.loadFile("/home/william/Downloads/TestingGosia-Transfer.csv")
    val curve   = TransferCurve(20e-6, 1000e-6, 2.05 * 8.854187817e-12 / 480e-9, results)
    curve.results.outputTable()

}