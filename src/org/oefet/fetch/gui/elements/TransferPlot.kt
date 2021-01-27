package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable

import jisa.gui.*
import org.oefet.fetch.*
import kotlin.math.abs

class TransferPlot(data: ResultTable) : FetChPlot("Transfer Curve", "SG Voltage [V]", "Current [A]") {

    init {

        setMouseEnabled(true)
        setYAxisType(AxisType.LOGARITHMIC)
        setPointOrdering(Sort.ORDER_ADDED)

        if (data.numRows > 0) {
            legendRows = data.getUniqueValues(SET_SG).size
        } else {
            legendColumns = 2
        }

        createSeries()
            .setMarkerVisible(false)
            .watch(data, { it[SG_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SD, "D (SD: %s V)")

        createSeries()
            .setMarkerVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SG_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SD, "G (SD: %sV)")

    }

}