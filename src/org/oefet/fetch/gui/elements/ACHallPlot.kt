package org.oefet.fetch.gui.elements

import jisa.gui.*
import jisa.gui.Series.Dash.DOTTED
import jisa.results.ResultTable
import jisa.results.Row
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.results.ACHallResult
import java.util.function.Predicate
import kotlin.math.absoluteValue

class ACHallPlot(data: ResultTable, optimised: ResultTable?, faraday: ResultTable?) : Tabs("AC Hall") {

    val FARADAY      = data.findColumn(ACHall.FARADAY)
    val HALL_ERROR   = data.findColumn(ACHall.HALL_ERROR)
    val HALL_VOLTAGE = data.findColumn(ACHall.HALL_VOLTAGE)
    val SD_CURRENT   = data.findColumn(ACHall.SD_CURRENT)
    val FREQUENCY    = data.findColumn(ACHall.FREQUENCY)

    val VS_COLOURS   = arrayOf(Colour.ORANGE, Colour.GREEN, Colour.CORNFLOWERBLUE, Colour.PURPLE)
    val PO_COLOURS   = arrayOf(Colour.RED, Colour.TEAL, Colour.BLUE, Colour.MAROON)

    constructor(data: ResultTable) : this(data, null, null)

    init {

        val plot1 = FetChPlot("AC Hall", "Drain Current [A]", "Hall Voltage [V]")

        val filter = if (FARADAY != null) {
            Predicate<Row> { !it[FARADAY] }
        } else {
            Predicate<Row> { true }
        }

        plot1.isMouseEnabled = true
        plot1.pointOrdering  = Plot.Sort.ORDER_ADDED

        val zero by lazy { data.filter(filter).minByOrNull { it[SD_CURRENT].absoluteValue }?.get(HALL_VOLTAGE) ?: 0.0 }

        plot1.createSeries()
            .setName("Vector Subtracted")
            .watch(data, { it[SD_CURRENT] }, { it[HALL_VOLTAGE] - zero }, { it[HALL_ERROR] })
            .filter(filter)
            .split(FREQUENCY, "VS (%s Hz)")
            .setColourSequence(*VS_COLOURS)
            .polyFit(1)


        try {

            if (optimised != null) {

                plot1.createSeries()
                    .setName("Phase Optimised")
                    .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_HALL, ACHallResult.ROT_ERROR)
                    .setMarkerShape(Series.Shape.SQUARE)
                    .split(ACHallResult.ROT_FREQUENCY, "PO (%s Hz)")
                    .setColourSequence(*PO_COLOURS)
                    .polyFit(1)


                plot1.createSeries()
                    .setName("Faraday Voltage")
                    .setMarkerVisible(false)
                    .setLineDash(DOTTED)
                    .setLineWidth(1.0)
                    .watch(optimised, ACHallResult.ROT_CURRENT, ACHallResult.ROT_FARADAY)
                    .split(ACHallResult.ROT_FREQUENCY, "FV (%s Hz)")


            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        add(Grid("AC Hall", plot1))

        if (faraday != null && faraday.rowCount > 0) {

            val plot2 = FetChPlot("Faraday Sweep", "Frequency [Hz]", "Faraday Voltage [V]")

            plot2.createSeries()
                 .watch(faraday, ACHallResult.FAR_FREQUENCY, ACHallResult.FAR_VOLTAGE)
                 .polyFit(1)

            plot2.isLegendVisible = false

            add(Grid("Faraday Sweep", plot2))

        }

    }

}