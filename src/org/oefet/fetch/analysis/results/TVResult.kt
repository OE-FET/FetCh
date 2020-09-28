package org.oefet.fetch.analysis.results

import jisa.enums.Icon
import jisa.experiment.ResultTable
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.measurement.TVMeasurement
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

class TVResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot = TVPlot(data)
    override val name = "Thermal Voltage Measurement (${data.getAttribute("Name")})"
    override val image = Icon.THERMOMETER.blackImage
    override val label = "Thermal Voltage"
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
            throw Exception("That is not a thermal voltage measurement file")
        }

        val length = data.getAttribute("Length").removeSuffix("m").toDouble()
        val separation = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
        val width = data.getAttribute("Width").removeSuffix("m").toDouble()
        val thickness = data.getAttribute("Thickness").removeSuffix("m").toDouble()
        val dielectric = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
        val permittivity = data.getAttribute("Dielectric Permittivity").toDouble()

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

        for ((gate, data) in data.split(TVMeasurement.SET_GATE)) {

            val params = ArrayList(parameters)
            params += Gate(gate, 0.0)

            val fit = data.linearFit(TVMeasurement.HEATER_POWER, TVMeasurement.THERMAL_VOLTAGE)

            if (fit != null) quantities += SeebeckPower(fit.gradient, fit.gradientError, params, possibleParameters)

        }

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {

        val extras = LinkedList<Quantity>()

        for (seebeckPower in quantities.filter { it is SeebeckPower }) {

            val coefficient = quantities.find { it is ThermalCalibration && it.isCompatibleWith(seebeckPower) } ?: continue
            val seebeck     = seebeckPower.value * coefficient.value
            val error       = sqrt((seebeckPower.error / seebeckPower.value).pow(2) + (coefficient.error / coefficient.value).pow(2)) * seebeck

            extras += SeebeckCoefficient(seebeck, error, seebeckPower.parameters)

        }

        return extras

    }

}