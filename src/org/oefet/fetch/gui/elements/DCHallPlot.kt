package org.oefet.fetch.gui.elements

import jisa.experiment.Combination
import jisa.experiment.Result
import jisa.experiment.ResultTable
import org.oefet.fetch.measurement.DCHall
import kotlin.math.pow
import kotlin.math.sqrt

class DCHallPlot(data: ResultTable) : FetChPlot("DC Hall", "Field [T]", "Hall Voltage [V]") {

    val FIELD          = data.findColumn(DCHall.FIELD)
    val HALL_1         = data.findColumn(DCHall.HALL_1)
    val HALL_1_ERROR   = data.findColumn(DCHall.HALL_1_ERROR)
    val HALL_2         = data.findColumn(DCHall.HALL_2)
    val HALL_2_ERROR   = data.findColumn(DCHall.HALL_2_ERROR)
    val SET_SD_CURRENT = data.findColumn(DCHall.SET_SD_CURRENT)
    val SET_SG_VOLTAGE = data.findColumn(DCHall.SET_SG_VOLTAGE)

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        val noField = (data.getUniqueValues(FIELD).size == 0 && data.getUniqueValues(SET_SD_CURRENT).size > 0) || data.getAttribute("Field Sweep") == "false"

        val xValue: (Result) -> Double = if (noField) {
            xLabel = "Source-Drain Current [A]"
            { it[SET_SD_CURRENT] }
        } else {
            xLabel = "Field [T]"
            { it[FIELD] }
        }

        val yValue: (Result) -> Double = {

            if (it[HALL_1].isFinite() && it[HALL_2].isFinite()) {
                it[HALL_2] - it[HALL_1]
            } else if (it[HALL_1].isFinite()) {
                it[HALL_1]
            } else {
                it[HALL_2]
            }

        }

        val eValue: (Result) -> Double = {

            if (it[HALL_1].isFinite() && it[HALL_2].isFinite()) {
                sqrt(it[HALL_1_ERROR].pow(2) + it[HALL_2_ERROR].pow(2))
            } else if (it[HALL_1].isFinite()) {
                it[HALL_1_ERROR]
            } else {
                it[HALL_2_ERROR]
            }

        }

        val splitter: (Result) -> Combination = if(noField) {
            { Combination(it[SET_SG_VOLTAGE]) }
        } else {
            { Combination(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE]) }
        }

        val splitNamer: (Result) -> String = if (noField) {

            { "SG = %+.02e V".format(it[SET_SG_VOLTAGE]) }

        } else {

            {

                if (it[SET_SG_VOLTAGE] != 0.0) {
                    "SD = %+.02e A, SG = %+.02e V".format(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE])
                } else {
                    "SD = %+.02e A".format(it[SET_SD_CURRENT])
                }

            }

        }

        createSeries()
            .setName("Hall Voltage")
            .setLineVisible(true)
            .setMarkerVisible(true)
            .watch(data, xValue, yValue, eValue)
            .split(splitter, splitNamer).polyFit(1)

    }

}