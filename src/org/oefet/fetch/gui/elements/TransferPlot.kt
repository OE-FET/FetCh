package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable

import jisa.gui.*
import org.oefet.fetch.SD_CURRENT
import org.oefet.fetch.SET_SD
import org.oefet.fetch.SG_CURRENT
import org.oefet.fetch.SG_VOLTAGE
import kotlin.math.abs

class TransferPlot(data: ResultTable) : FetChPlot("Transfer Curve", "SG Voltage [V]", "Current [A]") {

    init {

        setMouseEnabled(true)
        setYAxisType(AxisType.LOGARITHMIC)
        setPointOrdering(Sort.ORDER_ADDED)

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