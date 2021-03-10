package org.oefet.fetch.results

import jisa.experiment.ResultTable
import org.oefet.fetch.quantities.*
import org.oefet.fetch.gui.elements.TVCResultPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVCalibration.Companion.HEATER_POWER
import org.oefet.fetch.measurement.TVCalibration.Companion.SET_HEATER_VOLTAGE
import org.oefet.fetch.measurement.TVCalibration.Companion.STRIP_CURRENT
import org.oefet.fetch.measurement.TVCalibration.Companion.STRIP_VOLTAGE
import org.oefet.fetch.measurement.TVCalibration.Companion.TEMPERATURE

class TVCResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = TVCResultPlot(data)
    override val name       = "Thermal Voltage Calibration (${data.getAttribute("Name")})"
    override val image      = Images.getImage("calibration.png")
    override val label      = "Thermal Voltage Calibration"

    override var length:       Double = 0.0
    override var separation:   Double = 0.0
    override var width:        Double = 0.0
    override var thickness:    Double = 0.0
    override var dielectric:   Double = 0.0
    override var permittivity: Double = 0.0
    override var temperature:  Double = Double.NaN
    override var repeat:       Double = 0.0
    override var stress:       Double = 0.0
    override var field:        Double = 0.0

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

        parseParameters(data, extraParams, data.getMean(TEMPERATURE))

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

            quantities += resistance

        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}