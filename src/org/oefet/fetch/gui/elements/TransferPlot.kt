package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.Series
import org.oefet.fetch.measurement.Transfer
import kotlin.math.abs

class TransferPlot(data: ResultTable) : FetChPlot("Transfer Curve", "SG Voltage [V]", "Current [A]") {

    val SET_SD_VOLTAGE = data.findColumn(Transfer.SET_SD_VOLTAGE)
    val SET_SG_VOLTAGE = data.findColumn(Transfer.SET_SG_VOLTAGE)
    val SD_VOLTAGE     = data.findColumn(Transfer.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Transfer.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Transfer.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Transfer.SG_CURRENT)
    val FPP_1          = data.findColumn(Transfer.FPP_1)
    val FPP_2          = data.findColumn(Transfer.FPP_2)
    val TEMPERATURE    = data.findColumn(Transfer.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Transfer.GROUND_CURRENT)

    init {

        isMouseEnabled = true
        yAxisType = AxisType.LOGARITHMIC
        pointOrdering = Sort.ORDER_ADDED

        if (data.numRows > 0) {
            legendRows = data.getUniqueValues(SET_SG_VOLTAGE).size
        } else {
            legendColumns = 2
        }

        createSeries()
            .setMarkerVisible(false)
            .watch(data, { it[SG_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SD_VOLTAGE, "D (SD: %s V)")

        createSeries()
            .setMarkerVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SG_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SD_VOLTAGE, "G (SD: %sV)")

    }

}