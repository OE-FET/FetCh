package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.Series.Dash.DOTTED
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.measurement.Conductivity.Companion.FPP1_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.FPP2_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.FPP_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.SD_CURRENT

class FPPPlot(data: ResultTable) : FetChPlot("Conductivity", "Drain Current [A]", "Voltage [V]") {

    val SD_VOLTAGE     = data.findColumn(Conductivity.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Conductivity.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Conductivity.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Conductivity.SG_CURRENT)
    val FPP1_VOLTAGE   = data.findColumn(Conductivity.FPP1_VOLTAGE)
    val FPP2_VOLTAGE   = data.findColumn(Conductivity.FPP2_VOLTAGE)
    val FPP_VOLTAGE    = data.findColumn(Conductivity.FPP_VOLTAGE)
    val TEMPERATURE    = data.findColumn(Conductivity.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Conductivity.GROUND_CURRENT)
    
    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setName("Probe 1")
            .setMarkerVisible(false)
            .setLineDash(DOTTED)
            .watch(data, SD_CURRENT, FPP1_VOLTAGE)

        createSeries()
            .setName("Probe 2")
            .setMarkerVisible(false)
            .setLineDash(DOTTED)
            .watch(data, SD_CURRENT, FPP2_VOLTAGE)

        createSeries()
            .setName("FPP Difference")
            .polyFit(1)
            .watch(data, SD_CURRENT, FPP_VOLTAGE)


    }

}