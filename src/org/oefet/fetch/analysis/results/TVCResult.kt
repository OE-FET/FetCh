package org.oefet.fetch.analysis.results

import jisa.enums.Icon
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.gui.elements.TVCPlot
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.measurement.TVCalibration
import org.oefet.fetch.measurement.TVCalibration.Companion.HEATER_POWER
import org.oefet.fetch.measurement.TVCalibration.Companion.SET_HEATER_VOLTAGE
import org.oefet.fetch.measurement.TVCalibration.Companion.STRIP_CURRENT
import org.oefet.fetch.measurement.TVCalibration.Companion.STRIP_VOLTAGE

class TVCResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = TVCPlot(data)
    override val name       = "Thermal Voltage Calibration (${data.getAttribute("Name")})"
    override val image      = Icon.THERMOMETER.blackImage
    override val label      = "Thermal Voltage Calibration"

    override var length:       Double = 0.0
    override var separation:   Double = 0.0
    override var width:        Double = 0.0
    override var thickness:    Double = 0.0
    override var dielectric:   Double = 0.0
    override var permittivity: Double = 0.0

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

        parseParameters(data, extraParams)

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

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }

}