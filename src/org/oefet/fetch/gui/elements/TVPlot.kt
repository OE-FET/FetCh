package org.oefet.fetch.gui.elements

import jisa.experiment.ResultTable
import org.oefet.fetch.measurement.TVCalibration
import org.oefet.fetch.measurement.TVMeasurement
import org.oefet.fetch.measurement.TVMeasurement.Companion.HEATER_POWER
import org.oefet.fetch.measurement.TVMeasurement.Companion.THERMAL_VOLTAGE

class TVPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Heater Power [W]", "Thermal Voltage [V]") {

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setLineVisible(false)
            .watch(data, HEATER_POWER, THERMAL_VOLTAGE)
            .split(TVMeasurement.SET_GATE, "SG = %s V")
            .setMarkerVisible(true)
            .setLineVisible(true)
            .polyFit(1)

    }

}

class TVCPlot(data: ResultTable) : FetChPlot("Thermal Voltage Calibration", "Heater Power [W]", "Resistance [Ohm]") {

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        createSeries()
            .setLineVisible(false)
            .watch(data, TVCalibration.STRIP_CURRENT, TVCalibration.STRIP_VOLTAGE)
            .split(TVCalibration.HEATER_POWER, "P = %.02e W")

    }

}