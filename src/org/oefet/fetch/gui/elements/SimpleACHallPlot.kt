package org.oefet.fetch.gui.elements

import jisa.gui.Colour
import jisa.results.ResultTable
import jisa.results.Row
import org.oefet.fetch.measurement.ACHall
import java.util.function.Predicate

class SimpleACHallPlot(data: ResultTable) : FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]") {

    val FARADAY      = data.findColumn(ACHall.FARADAY)
    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)
    val FREQUENCY    = data.findColumn(ACHall.FREQUENCY)

    val VS_COLOURS   = arrayOf(Colour.ORANGE, Colour.GREEN, Colour.CORNFLOWERBLUE, Colour.PURPLE)
    val PO_COLOURS   = arrayOf(Colour.RED, Colour.TEAL, Colour.BLUE, Colour.MAROON)

    init {

        val filter = if (FARADAY != null) {
            Predicate<Row> { !it[FARADAY] }
        } else {
            Predicate<Row> { true }
        }

        isMouseEnabled = true

        val vsZero by lazy { data.filter(filter).getMin { it[HALL_VOLTAGE] } ?: 0.0 }
        val vsMax  by lazy { (data.filter(filter).getMax { it[HALL_VOLTAGE] } ?: 0.0) - vsZero }

        createSeries()
            .setName("VS")
            .watch(data, { it[SD_CURRENT] }, { it[HALL_VOLTAGE] - vsZero }, { it[HALL_ERROR] })
            .filter(filter)
            .split(FREQUENCY, "VS (%s Hz)")
            .polyFit(1)

    }

}