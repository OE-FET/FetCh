package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.GUI
import jisa.gui.Plot
import jisa.gui.Series
import jisa.gui.Series.Dash.DOTTED
import jisa.maths.matrices.RealMatrix
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP1_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP2_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.SD_CURRENT
import org.oefet.fetch.measurement.FPPMeasurement.Companion.SD_VOLTAGE

class FPPPlot(data: ResultTable) : FetChPlot("FPP Conductivity", "Drain Current [A]", "Voltage [V]") {

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