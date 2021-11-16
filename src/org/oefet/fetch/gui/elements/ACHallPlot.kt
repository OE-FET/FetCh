package org.oefet.fetch.gui.elements

import jisa.gui.Series.Dash.DASHED
import jisa.maths.matrices.RealMatrix
import jisa.results.ResultTable
import org.oefet.fetch.measurement.ACHall
import kotlin.math.absoluteValue

class ACHallPlot(data: ResultTable, optimised: RealMatrix?, faraday: RealMatrix?) : FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]") {

    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)

    constructor(data: ResultTable) : this(data, null, null)

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        val zero by lazy { data.minByOrNull { it[SD_CURRENT].absoluteValue }?.get(HALL_VOLTAGE) ?: 0.0 }

        createSeries()
            .setName("Vector Subtracted")
            .polyFit(1)
            .watch(data, { it[SD_CURRENT] }, { it[HALL_VOLTAGE] - zero }, { it[HALL_ERROR] })

        if (optimised != null) {

            createSeries()
                .setName("Phase Optimised")
                .polyFit(1)
                .addPoints(data.toMatrix(SD_CURRENT), optimised, data.toMatrix(HALL_ERROR))

        }

        if (faraday != null) {

            createSeries()
                .setName("Faraday Voltage")
                .setMarkerVisible(false)
                .setLineDash(DASHED)
                .addPoints(data.toMatrix(SD_CURRENT), faraday)

        }


    }

}