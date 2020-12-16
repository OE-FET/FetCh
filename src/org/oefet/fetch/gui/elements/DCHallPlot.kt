package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.Series
import org.oefet.fetch.measurement.DCHall.Companion.FIELD
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1
import org.oefet.fetch.measurement.DCHall.Companion.HALL_1_ERROR
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2
import org.oefet.fetch.measurement.DCHall.Companion.HALL_2_ERROR
import org.oefet.fetch.measurement.DCHall.Companion.SD_CURRENT
import org.oefet.fetch.measurement.DCHall.Companion.SG_VOLTAGE

class DCHallPlot(data: ResultTable) : FetChPlot("DC Hall", "Drain Current [A]", "Hall Voltage [V]") {

    init {

        isMouseEnabled  = true
        pointOrdering   = Sort.ORDER_ADDED

        val polyFit = createSeries()
            .setName("Hall Voltage 1")
            .setLineVisible(true)
            .setMarkerVisible(true)
            .watch(data, { it[SD_CURRENT] * it[FIELD] }, { it[HALL_1] }, { it[HALL_1_ERROR] })
            .split(SG_VOLTAGE, "Hall 1 (SG = %s V)")
            .setMarkerShape(Series.Shape.CIRCLE)
            .polyFit(1)

        createSeries()
            .setLineVisible(true)
            .setMarkerVisible(true)
            .watch(data, { it[SD_CURRENT] * it[FIELD] } , { it[HALL_2] }, { it[HALL_2_ERROR] })
            .split(SG_VOLTAGE, "Hall 2 (SG = %s V)")
            .setMarkerShape(Series.Shape.SQUARE)
            .polyFit(1)

        legendColumns = 2;

        xLabel = "Current × Field [AT]"
        yLabel = "Hall Voltage [V]"

    }

}