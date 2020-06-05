package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.*
import org.oefet.fetch.SD_CURRENT
import org.oefet.fetch.SD_VOLTAGE
import org.oefet.fetch.SG_CURRENT
import kotlin.math.abs

class SyncPlot(data: ResultTable) : FetChPlot("Synced Voltage Curve", "Voltage [V]", "Current [A]") {

    init {

        setMouseEnabled(true)
        setYAxisType(AxisType.LOGARITHMIC)
        setPointOrdering(Sort.ORDER_ADDED)

        createSeries()
            .setName("Drain")
            .setColour(Colour.CORNFLOWERBLUE)
            .setMarkersVisible(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })

        createSeries()
            .setName("Gate")
            .setColour(Colour.ORANGERED)
            .setMarkersVisible(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })

    }

}