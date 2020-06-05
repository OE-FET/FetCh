package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable


import jisa.gui.*
import org.oefet.fetch.SD_CURRENT
import org.oefet.fetch.SD_VOLTAGE
import org.oefet.fetch.SET_SG
import org.oefet.fetch.SG_CURRENT
import kotlin.math.abs

class OutputPlot(data: ResultTable) : FetChPlot("Output Curve", "SD Voltage [V]", "Current [A]") {

    init {

        setMouseEnabled(true)
        setYAxisType(AxisType.LINEAR)
        setPointOrdering(Sort.ORDER_ADDED)

        createSeries()
            .setMarkersVisible(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SG, "D (SG: %s V)")

        createSeries()
            .setMarkersVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SG, "G (SG: %sV)")


    }

}