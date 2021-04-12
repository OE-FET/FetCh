package org.oefet.fetch.results

import jisa.experiment.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.*
import org.oefet.fetch.gui.elements.TVCResultPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVCalibration

class TVCResult(data: ResultTable, extraParams: List<Quantity> = emptyList()) :
    FetChResult(
        "Thermal Voltage Calibration",
        "Thermal Voltage Calibration",
        Images.getImage("calibration.png"),
        data,
        extraParams
    ) {

    val SET_HEATER_VOLTAGE  = data.findColumn(TVCalibration.SET_HEATER_VOLTAGE)
    val GROUND_CURRENT      = data.findColumn(TVCalibration.GROUND_CURRENT)
    val HEATER_POWER        = data.findColumn(TVCalibration.HEATER_POWER)
    val STRIP_VOLTAGE       = data.findColumn(TVCalibration.STRIP_VOLTAGE)
    val STRIP_CURRENT       = data.findColumn(TVCalibration.STRIP_CURRENT)
    val TEMPERATURE         = data.findColumn(TVCalibration.TEMPERATURE)

    private val possibleParameters = listOf(
        Device::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class
    )

    init {

        val probeNumber = data.getAttribute("Probe Number").toInt()

        for ((heaterVoltage, data) in data.split(SET_HEATER_VOLTAGE)) {

            val power              = data.getMean(HEATER_POWER)
            val fit                = data.linearFit(STRIP_CURRENT, STRIP_VOLTAGE)
            val parameters         = parameters.toMutableList()
            val possibleParameters = possibleParameters.toMutableList()
            parameters            += Drain(heaterVoltage, 0.0)
            parameters            += Power(power, 0.0)

            val resistance = if (probeNumber == 0) {
                LeftStripResistance(fit.gradient, fit.gradientError, parameters, possibleParameters)
            } else {
                RightStripResistance(fit.gradient, fit.gradientError, parameters, possibleParameters)
            }

            addQuantity(resistance)

        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

    override fun getPlot(): FetChPlot {
        return TVCResultPlot(data)
    }

}