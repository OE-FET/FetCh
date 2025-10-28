package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVCPlot
import org.oefet.fetch.gui.elements.TVCResultPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVCalibration
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type


class TVCalibrationData(data: ResultTable) : FetChData("Thermal Voltage Calibration", "Thermal Voltage Calibration", data, Images.getImage("calibration.png")) {

    val SET_HEATER_VOLTAGE = data.findColumn(TVCalibration.SET_HEATER_VOLTAGE)
    val GROUND_CURRENT     = data.findColumn(TVCalibration.GROUND_CURRENT)
    val HEATER_POWER       = data.findColumn(TVCalibration.HEATER_POWER)
    val STRIP_VOLTAGE      = data.findColumn(TVCalibration.STRIP_VOLTAGE)
    val STRIP_CURRENT      = data.findColumn(TVCalibration.STRIP_CURRENT)
    val TEMPERATURE        = data.findColumn(TVCalibration.TEMPERATURE)

    override fun processData(data: ResultTable): List<Result> {

        val list        = mutableListOf<Result>()
        val probeNumber = data.getAttribute("Probe Number").toDouble()

        for ((heaterVoltage, data) in data.split(SET_HEATER_VOLTAGE)) {

            val power      = data.mean(HEATER_POWER)
            val fit        = Fitting.linearFit(data, STRIP_CURRENT, STRIP_VOLTAGE)
            val parameters = parameters + DoubleQuantity("Probe Number", Type.INDEX, probeNumber) + DoubleQuantity("Heater Power", Type.POWER, power) + DoubleQuantity("Base Temperature", Type.TEMPERATURE, data.mean(TEMPERATURE))

            list += Result("T-Probe Resistance", "R_T", Type.RESISTANCE, fit.gradient, fit.gradientError, parameters)

        }

        return list

    }

    override fun getDisplay(): Element {
        return TVCResultPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> = emptyList()

}