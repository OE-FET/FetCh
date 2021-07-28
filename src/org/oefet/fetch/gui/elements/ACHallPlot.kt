package org.oefet.fetch.gui.elements

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Series.Dash.DASHED
import jisa.maths.matrices.RealMatrix
import org.oefet.fetch.measurement.ACHall

class ACHallPlot(data: ResultTable, optimised: RealMatrix?, faraday: RealMatrix?) : FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]") {

    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)

    constructor(data: ResultTable) : this(data, null, null)

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setName("Vector Subtracted")
            .polyFit(1)
            .watch(data, SD_CURRENT, HALL_VOLTAGE, HALL_ERROR)

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