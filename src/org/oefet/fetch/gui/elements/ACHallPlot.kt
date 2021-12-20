package org.oefet.fetch.gui.elements

import jisa.gui.Colour
import jisa.gui.Series
import jisa.gui.Series.Dash.DASHED
import jisa.results.ResultTable
import jisa.results.Row
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.results.ACHallResult
import java.util.function.Predicate
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

        println("1")

        val filter = if (FARADAY != null) {
            Predicate<Row> { !it[FARADAY] }
        } else {
            Predicate<Row> { true }
        }

        println("2")

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        val zero by lazy { data.filter(filter).minByOrNull { it[SD_CURRENT].absoluteValue }?.get(HALL_VOLTAGE) ?: 0.0 }

        println("3")

        createSeries()
            .setName("Vector Subtracted")
            .watch(data, { it[SD_CURRENT] }, { it[HALL_VOLTAGE] - zero }, { it[HALL_ERROR] })
            .filter(filter)
            .split(FREQUENCY, "VS (%s Hz)")
            .setColourSequence(*VS_COLOURS)
            .polyFit(1)


        println("4")

        try {

            if (optimised != null) {

                createSeries()
                    .setName("Phase Optimised")
                    .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_HALL, ACHallResult.ROT_ERROR)
                    .setMarkerShape(Series.Shape.SQUARE)
                    .split(ACHallResult.ROT_FREQUENCY, "PO (%s Hz)")
                    .setColourSequence(*PO_COLOURS)
                    .polyFit(1)

                println("5")

                createSeries()
                    .setName("Faraday Voltage")
                    .setMarkerVisible(false)
                    .setLineDash(DASHED)
                    .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_FARADAY)
                    .split(ACHallResult.ROT_FREQUENCY, "FV (%s Hz)")

                println("6")

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        println("7")


    }

}