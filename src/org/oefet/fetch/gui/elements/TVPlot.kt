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
import org.oefet.fetch.measurement.TVMeasurement

class TVPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Measurement No.", "Thermal Voltage [V]") {

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setLineVisible(false)
            .watch(data, TVMeasurement.MEAS_NO, TVMeasurement.THERMAL_VOLTAGE)
            .split(TVMeasurement.SET_GATE, "SG = %s V")

    }

}