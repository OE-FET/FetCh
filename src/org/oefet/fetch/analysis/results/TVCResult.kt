package org.oefet.fetch.analysis.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.measurement.TVCMeasumrent

class TVCResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = TVPlot(data)
    override val name       = "Thermal Voltage Calibration (${data.getAttribute("Name")})"
    override val image      = Icon.THERMOMETER.blackImage
    override val label      = "Thermal Voltage Calibration"

    private val possibleParameters = listOf(
        Device::class,
        Repeat::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class
    )

    init {

        if (data.getAttribute("Type") != label) {
            throw Exception("That is not a thermal voltage calibration file")
        }

        val length       = data.getAttribute("Length").removeSuffix("m").toDouble()
        val separation   = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
        val width        = data.getAttribute("Width").removeSuffix("m").toDouble()
        val thickness    = data.getAttribute("Thickness").removeSuffix("m").toDouble()
        val dielectric   = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
        val permittivity = data.getAttribute("Dielectric Permittivity").toDouble()
        val probeNumber  = data.getAttribute("Probe Number").toInt()

        parameters += Length(length, 0.0)
        parameters += FPPSeparation(separation, 0.0)
        parameters += Width(width, 0.0)
        parameters += Thickness(thickness, 0.0)
        parameters += DThickness(dielectric, 0.0)
        parameters += Permittivity(permittivity, 0.0)

        for ((_, value) in data.attributes) {

            parameters += Quantity.parseValue(value) ?: continue

        }

        parameters += extraParams

        for ((heaterVoltage, data) in data.split(TVCMeasumrent.SET_HEATER_VOLTAGE)) {

            val power      = data.getMean(TVCMeasumrent.HEATER_POWER)
            val fit        = data.linearFit(TVCMeasumrent.STRIP_CURRENT, TVCMeasumrent.STRIP_VOLTAGE)

            val resistance = StripResistance(fit.gradient, fit.gradientError, parameters, possibleParameters)

        }

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}