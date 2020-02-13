package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import org.oefet.fetch.analysis.OCurve
import org.oefet.fetch.analysis.TCurve
import jisa.gui.*
import org.oefet.fetch.SD_CURRENT
import org.oefet.fetch.SD_VOLTAGE
import org.oefet.fetch.SET_SG
import org.oefet.fetch.SG_CURRENT
import kotlin.math.abs

class SyncPlot(data: ResultTable) : Plot("Synced Voltage Curve", "Voltage [V]", "Current [A]") {

    init {

        useMouseCommands(true)
        setYAxisType(AxisType.LINEAR)
        setPointOrdering(Sort.ORDER_ADDED)

        createSeries()
            .showMarkers(false)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })

        createSeries()
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data, { it[SD_VOLTAGE] }, { abs(it[SG_CURRENT]) })


        addSaveButton("Save")
        addToolbarSeparator()
        addToolbarButton("Linear") { setYAxisType(AxisType.LINEAR) }
        addToolbarButton("Logarithmic") { setYAxisType(AxisType.LOGARITHMIC) }

    }

}