package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable


import jisa.gui.*
import org.oefet.fetch.measurement.Output
import kotlin.math.abs

class OutputPlot(data: ResultTable) : FetChPlot("Output Curve", "SD Voltage [V]", "Current [A]") {

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
        yAxisType = AxisType.LINEAR
        pointOrdering = Sort.ORDER_ADDED

        if (data.numRows > 0) {
            legendRows = data.getUniqueValues(SET_SG_VOLTAGE).size
        } else {
            legendColumns = 2
        }

        createSeries()
            .setMarkerVisible(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SG_VOLTAGE, "D (SG: %s V)")

        createSeries()
            .setMarkerVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SG_VOLTAGE, "G (SG: %sV)")


    }

}