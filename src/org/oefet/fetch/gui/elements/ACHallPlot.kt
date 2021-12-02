package org.oefet.fetch.gui.elements

import jisa.gui.Colour
import jisa.gui.Series
import jisa.gui.Series.Dash.DASHED
import jisa.results.ResultTable
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.results.ACHallResult
import kotlin.math.absoluteValue

class ACHallPlot(data: ResultTable, optimised: ResultTable?) : FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]") {

    val FARADAY       = data.findColumn(ACHall.FARADAY)
    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)
    val FREQUENCY    = data.findColumn(ACHall.FREQUENCY)

    val VS_COLOURS   = arrayOf(Colour.ORANGE, Colour.GREEN, Colour.CORNFLOWERBLUE, Colour.PURPLE)
    val PO_COLOURS   = arrayOf(Colour.RED, Colour.TEAL, Colour.BLUE, Colour.MAROON)

    constructor(data: ResultTable) : this(data, null)

    init {

        val dataF = if (FARADAY != null) {
            data.filter { !it[FARADAY] }
        } else {
            data
        }

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        val zero by lazy { dataF.minByOrNull { it[SD_CURRENT].absoluteValue }?.get(HALL_VOLTAGE) ?: 0.0 }

        createSeries()
            .setName("Vector Subtracted")
            .watch(dataF, { it[SD_CURRENT] }, { it[HALL_VOLTAGE] - zero }, { it[HALL_ERROR] })
            .split(FREQUENCY, "VS (%s Hz)")
            .setColourSequence(*VS_COLOURS)
            .polyFit(1)

        if (optimised != null) {

            createSeries()
                .setName("Phase Optimised")
                .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_HALL, ACHallResult.ROT_ERROR)
                .setMarkerShape(Series.Shape.SQUARE)
                .split(ACHallResult.ROT_FREQUENCY, "PO (%s Hz)")
                .setColourSequence(*PO_COLOURS)
                .polyFit(1)

            createSeries()
                .setName("Faraday Voltage")
                .setMarkerVisible(false)
                .setLineDash(DASHED)
                .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_FARADAY)
                .split(ACHallResult.ROT_FREQUENCY, "FV (%s Hz)")

        }


    }

}