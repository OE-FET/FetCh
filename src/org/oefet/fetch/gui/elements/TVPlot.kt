package org.oefet.fetch.gui.elements

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.gui.Colour
import org.oefet.fetch.measurement.TVCalibration
import org.oefet.fetch.measurement.TVMeasurement

class TVPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Heater Power [W]", "Thermal Voltage [V]") {

    val SET_GATE              = data.findColumn(TVMeasurement.SET_GATE)
    val TEMPERATURE           = data.findColumn(TVMeasurement.TEMPERATURE)
    val HEATER_POWER          = data.findColumn(TVMeasurement.HEATER_POWER)
    val THERMAL_VOLTAGE       = data.findColumn(TVMeasurement.THERMAL_VOLTAGE)
    val THERMAL_VOLTAGE_ERROR = data.findColumn(TVMeasurement.THERMAL_VOLTAGE_ERROR)

    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED

        if (THERMAL_VOLTAGE_ERROR != -1) {

            createSeries()
                .setLineVisible(false)
                .watch(data, HEATER_POWER, THERMAL_VOLTAGE, THERMAL_VOLTAGE_ERROR)
                .split(SET_GATE, "SG = %s V")
                .setMarkerVisible(true)
                .setLineVisible(true)
                .polyFit(1)

        } else {

            createSeries()
                .setLineVisible(false)
                .watch(data, HEATER_POWER, THERMAL_VOLTAGE)
                .split(SET_GATE, "SG = %s V")
                .setMarkerVisible(true)
                .setLineVisible(true)
                .polyFit(1)

        }

    }

}

class TVCPlot(data: ResultTable) : FetChPlot("Thermal Voltage Calibration", "Heater Power [W]", "Spot Strip Resistance [Ohm]") {

    val SET_STRIP_CURRENT   = data.findColumn(TVCalibration.SET_STRIP_CURRENT)
    val GROUND_CURRENT      = data.findColumn(TVCalibration.GROUND_CURRENT)
    val HEATER_POWER        = data.findColumn(TVCalibration.HEATER_POWER)
    val STRIP_VOLTAGE       = data.findColumn(TVCalibration.STRIP_VOLTAGE)
    val STRIP_VOLTAGE_ERROR = data.findColumn(TVCalibration.STRIP_VOLTAGE_ERROR)
    val STRIP_CURRENT       = data.findColumn(TVCalibration.STRIP_CURRENT)
    val TEMPERATURE         = data.findColumn(TVCalibration.TEMPERATURE)

    init {

        isMouseEnabled  = true
        pointOrdering   = Sort.ORDER_ADDED
        isLegendVisible = false

        if (STRIP_VOLTAGE_ERROR != -1) {

            createSeries()
                .setLineVisible(true)
                .watch(data, { it[HEATER_POWER] }, { it[STRIP_VOLTAGE] / it[STRIP_CURRENT] }, { it[STRIP_VOLTAGE_ERROR] / it[STRIP_CURRENT] })
                .filter { it[SET_STRIP_CURRENT] > 0.0 }
                .setColour(Colour.CORNFLOWERBLUE)
                .polyFit(1)

        } else {

            createSeries()
                .setLineVisible(true)
                .watch(data, { it[HEATER_POWER] }, { it[STRIP_VOLTAGE] / it[STRIP_CURRENT] })
                .filter { it[SET_STRIP_CURRENT] > 0.0 }
                .setColour(Colour.CORNFLOWERBLUE)
                .polyFit(1)

        }

    }

}

class TVCResultPlot(data: ResultTable) : FetChPlot("Thermal Voltage Calibration", "Heater Power [W]", "Four-Wire Resistance [Ohm]") {

    val SET_HEATER_VOLTAGE  = data.findColumn(TVCalibration.SET_HEATER_VOLTAGE)
    val SET_STRIP_CURRENT   = data.findColumn(TVCalibration.SET_STRIP_CURRENT)
    val GROUND_CURRENT      = data.findColumn(TVCalibration.GROUND_CURRENT)
    val HEATER_VOLTAGE      = data.findColumn(TVCalibration.HEATER_VOLTAGE)
    val HEATER_CURRENT      = data.findColumn(TVCalibration.HEATER_CURRENT)
    val HEATER_POWER        = data.findColumn(TVCalibration.HEATER_POWER)
    val STRIP_VOLTAGE       = data.findColumn(TVCalibration.STRIP_VOLTAGE)
    val STRIP_VOLTAGE_ERROR = data.findColumn(TVCalibration.STRIP_VOLTAGE_ERROR)
    val STRIP_CURRENT       = data.findColumn(TVCalibration.STRIP_CURRENT)
    val TEMPERATURE         = data.findColumn(TVCalibration.TEMPERATURE)

    init {

        isMouseEnabled  = true
        pointOrdering   = Sort.ORDER_ADDED
        isLegendVisible = false

        val fitted = ResultList("Heater Power", "Resistance", "Error")

        for ((_, splitData) in data.split(SET_HEATER_VOLTAGE)) {

            val fit = splitData.linearFit(STRIP_CURRENT, STRIP_VOLTAGE)
            fitted.addData(splitData.getMean(HEATER_POWER), fit.gradient, fit.gradientError)

        }

        createSeries()
            .setLineVisible(true)
            .watch(fitted, 0, 1, 2)
            .setColour(Colour.CORNFLOWERBLUE)
            .polyFit(1)

    }

}