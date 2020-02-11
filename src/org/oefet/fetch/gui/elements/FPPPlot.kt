package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import jisa.gui.GUI
import jisa.gui.Plot
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP1_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP2_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.FPP_VOLTAGE
import org.oefet.fetch.measurement.FPPMeasurement.Companion.SD_CURRENT
import org.oefet.fetch.measurement.FPPMeasurement.Companion.SD_VOLTAGE

class FPPPlot(data: ResultTable) : Plot("FPP Conductivity", "Drain Current [A]", "Voltage [V]") {

    init {

        useMouseCommands(true)

        createSeries()
            .setName("FPP Difference")
            .showMarkers(false)
            .watch(data, SD_CURRENT, FPP_VOLTAGE)

        createSeries()
            .setName("SD Voltage")
            .showMarkers(false)
            .watch(data, SD_CURRENT, SD_VOLTAGE)

        createSeries()
            .setName("Probe 1")
            .showMarkers(false)
            .watch(data, SD_CURRENT, FPP1_VOLTAGE)

        createSeries()
            .setName("Probe 2")
            .showMarkers(false)
            .watch(data, SD_CURRENT, FPP2_VOLTAGE)

        addSaveButton("Save")

        addToolbarButton("Conductivity") {

            val length    = Measure.fppLength.get()
            val width     = Measure.width.get()
            val thickness = Measure.cThick.get()

            val fit = data.linearFit(FPP_VOLTAGE, SD_CURRENT)
            val cond = fit.gradient * (length / (width * thickness)) * 1e4

            GUI.infoAlert("Conductivity = %e S/cm".format(cond))

        }


    }

}