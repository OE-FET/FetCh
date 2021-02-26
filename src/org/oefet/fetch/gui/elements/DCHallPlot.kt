package org.oefet.fetch.gui.elements

import jisa.experiment.Combination
import jisa.experiment.ResultTable
import jisa.gui.Series
import org.oefet.fetch.measurement.DCHall.Companion.FIELD
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1_ERROR
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2_ERROR
import org.oefet.fetch.measurement.DCHall.Companion.SET_SD_CURRENT
import org.oefet.fetch.measurement.DCHall.Companion.SET_SG_VOLTAGE

class DCHallPlot(data: ResultTable) : FetChPlot("DC Hall", "Field [T]", "Hall Voltage [V]") {

    init {

        isMouseEnabled = true
        pointOrdering = Sort.ORDER_ADDED

        val polyFit = createSeries()
            .setName("Hall Voltage 1")
            .setLineVisible(true)
            .setMarkerVisible(true)
            .watch(data, { it[FIELD] }, { it[HALL_1] }, { it[HALL_1_ERROR] })
            .split({ Combination(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE]) }, {

                if (it[SET_SG_VOLTAGE] != 0.0) {
                    "1: SD = %+.02e A, SG = %+.02e V".format(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE])
                } else {
                    "1: SD = %+.02e A".format(it[SET_SD_CURRENT])
                }

            })
            .setMarkerShape(Series.Shape.CIRCLE)
            .polyFit(1)

        createSeries()
            .setLineVisible(true)
            .setMarkerVisible(true)
            .watch(data, { it[FIELD] }, { it[HALL_2] }, { it[HALL_2_ERROR] })
            .split(
                { Combination(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE]) },
                {

                    if (it[SET_SG_VOLTAGE] != 0.0) {
                        "2: SD = %+.02e A, SG = %+.02e V".format(it[SET_SD_CURRENT], it[SET_SG_VOLTAGE])
                    } else {
                        "2: SD = %+.02e A".format(it[SET_SD_CURRENT])
                    }

                })
            .setMarkerShape(Series.Shape.SQUARE)
            .polyFit(1)

        legendColumns = 2;

    }

}