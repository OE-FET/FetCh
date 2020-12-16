package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.Series.Dash.DOTTED
import org.oefet.fetch.measurement.Conductivity.Companion.FPP1_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.FPP2_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.FPP_VOLTAGE
import org.oefet.fetch.measurement.Conductivity.Companion.SD_CURRENT

class FPPPlot(data: ResultTable) : FetChPlot("Conductivity", "Drain Current [A]", "Voltage [V]") {

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