package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.Series.Dash.DASHED
import jisa.gui.Series.Dash.DOTTED
import jisa.maths.matrices.RealMatrix
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.measurement.ACHall.Companion.HALL_ERROR
import org.oefet.fetch.measurement.ACHall.Companion.HALL_VOLTAGE
import org.oefet.fetch.measurement.ACHall.Companion.SD_CURRENT
import org.oefet.fetch.measurement.ACHall.Companion.X_VOLTAGE
import org.oefet.fetch.measurement.ACHall.Companion.Y_VOLTAGE

class ACHallPlot(data: ResultTable, optimised: RealMatrix?, faraday: RealMatrix?) : FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]") {

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
                .addPoints(data.getColumns(SD_CURRENT), optimised, data.getColumns(HALL_ERROR))

        }

        if (faraday != null) {

            createSeries()
                .setName("Faraday Voltage")
                .setMarkerVisible(false)
                .setLineDash(DASHED)
                .addPoints(data.getColumns(SD_CURRENT), faraday)

        }


    }

}