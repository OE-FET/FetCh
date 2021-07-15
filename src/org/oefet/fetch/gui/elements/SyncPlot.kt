package org.oefet.fetch.gui.elements

import jisa.gui.Colour
import jisa.gui.Series
import jisa.results.ResultTable
import org.oefet.fetch.measurement.Output
import kotlin.math.abs

class SyncPlot(data: ResultTable) : FetChPlot("Synced Voltage Curve", "Voltage [V]", "Current [A]") {

    val SET_SD_VOLTAGE = data.findColumn(Output.SET_SD_VOLTAGE)
    val SET_SG_VOLTAGE = data.findColumn(Output.SET_SG_VOLTAGE)
    val SD_VOLTAGE     = data.findColumn(Output.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Output.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Output.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Output.SG_CURRENT)
    val FPP_1          = data.findColumn(Output.FPP_1)
    val FPP_2          = data.findColumn(Output.FPP_2)
    val TEMPERATURE    = data.findColumn(Output.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Output.GROUND_CURRENT)

    init {

        isMouseEnabled = true
        yAxisType      = AxisType.LOGARITHMIC
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setName("Drain")
            .setColour(Colour.CORNFLOWERBLUE)
            .setMarkerVisible(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })

        createSeries()
            .setName("Gate")
            .setColour(Colour.ORANGERED)
            .setMarkerVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })

    }

}