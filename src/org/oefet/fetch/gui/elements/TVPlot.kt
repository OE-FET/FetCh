package org.oefet.fetch.gui.elements

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.Colour
import org.oefet.fetch.measurement.TVCalibration
import org.oefet.fetch.measurement.TVMeasurement
import org.oefet.fetch.measurement.TVMeasurement.Companion.HEATER_POWER
import org.oefet.fetch.measurement.TVMeasurement.Companion.THERMAL_VOLTAGE
import org.oefet.fetch.measurement.TVMeasurement.Companion.THERMAL_VOLTAGE_ERROR

class TVPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Heater Power [W]", "Thermal Voltage [V]") {

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        if (data.getName(THERMAL_VOLTAGE_ERROR) == "Thermal Voltage Error") {

            createSeries()
                .setLineVisible(false)
                .watch(data, HEATER_POWER, THERMAL_VOLTAGE, THERMAL_VOLTAGE_ERROR)
                .split(TVMeasurement.SET_GATE, "SG = %s V")
                .setMarkerVisible(true)
                .setLineVisible(true)
                .polyFit(1)

        } else {

            createSeries()
                .setLineVisible(false)
                .watch(data, HEATER_POWER, THERMAL_VOLTAGE)
                .split(TVMeasurement.SET_GATE, "SG = %s V")
                .setMarkerVisible(true)
                .setLineVisible(true)
                .polyFit(1)

        }

    }

}

class TVCPlot(data: ResultTable) : FetChPlot("Thermal Voltage Calibration", "Heater Power [W]", "Spot Strip Resistance [Ohm]") {

    init {

        isMouseEnabled  = true
        pointOrdering   = Sort.ORDER_ADDED
        isLegendVisible = false

        createSeries()
            .setLineVisible(true)
            .watch(data, { it[TVCalibration.HEATER_POWER] }, { it[TVCalibration.STRIP_VOLTAGE] / it[TVCalibration.STRIP_CURRENT] })
            .filter { it[TVCalibration.SET_STRIP_CURRENT] > 0.0 }
            .setColour(Colour.CORNFLOWERBLUE)
            .polyFit(1)

    }

}

class TVCResultPlot(data: ResultTable) : FetChPlot("Thermal Voltage Calibration", "Heater Power [W]", "Four-Wire Resistance [Ohm]") {

    init {

        isMouseEnabled  = true
        pointOrdering   = Sort.ORDER_ADDED
        isLegendVisible = false

        val fitted = ResultList("Heater Power", "Resistance", "Error")

        for ((_, data) in data.split(TVCalibration.SET_HEATER_VOLTAGE)) {

            val fit = data.linearFit(TVCalibration.STRIP_CURRENT, TVCalibration.STRIP_VOLTAGE)
            fitted.addData(data.getMean(TVCalibration.HEATER_POWER), fit.gradient, fit.gradientError)

        }

        createSeries()
            .setLineVisible(true)
            .watch(fitted, 0, 1, 2)
            .setColour(Colour.CORNFLOWERBLUE)
            .polyFit(1)

    }

}