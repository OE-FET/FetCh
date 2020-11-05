package org.oefet.fetch.analysis.results

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.fits.Fitting
import org.oefet.fetch.analysis.quantities.*
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVMeasurement
import kotlin.math.pow
import kotlin.math.sqrt

class TVResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot = TVPlot(data)
    override val name = "Thermal Voltage Measurement (${data.getAttribute("Name")})"
    override val image = Images.getImage("fire.png")
    override val label = "Thermal Voltage"

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
            params    += Gate(gate, 0.0)

            val fit = data.linearFit(TVMeasurement.HEATER_POWER, TVMeasurement.THERMAL_VOLTAGE)

            if (fit != null) quantities += SeebeckPower(fit.gradient, fit.gradientError, params, possibleParameters)

        }

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> {

        val calibrationLeft = quantities.filter {
            it is LeftStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.parameters.find { p -> p is Drain }?.value ?: 1.0 == 0.0
        }

        val calibrationRight = quantities.filter {
            it is RightStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.parameters.find { p -> p is Drain }?.value ?: 1.0 == 0.0
        }

        if (calibrationLeft.isEmpty() || calibrationRight.isEmpty()) {
            return emptyList()
        }

        val left = ResultList("Resistance", "Temperature")
        val right = ResultList("Resistance", "Temperature")

        for (resistance in calibrationLeft) {

            val temperature = resistance.parameters.find { it is Temperature } ?: continue
            left.addData(resistance.value, temperature.value)

        }

        for (resistance in calibrationRight) {

            val temperature = resistance.parameters.find { it is Temperature } ?: continue
            right.addData(resistance.value, temperature.value)

        }

        val leftFit  = left.linearFit(0, 1)
        val rightFit = right.linearFit(0, 1)

        val temperature = parameters.find { it is Temperature }?.value

        val powerLeft = quantities.filter {
            it is LeftStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.parameters.find { p -> p is Temperature }?.value == temperature
        }

        val powerRight = quantities.filter {
            it is RightStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.parameters.find { p -> p is Temperature }?.value == temperature
        }

        val dataLeft  = ResultList("Power", "Temperature", "Error")
        val dataRight = ResultList("Power", "Temperature", "Error")

        for (resistance in powerLeft) {

            val power        = resistance.parameters.find { it is Power } ?: continue
            val temp         = leftFit.gradient * resistance.value + leftFit.intercept
            val intermediate = leftFit.gradient * resistance.value * sqrt((leftFit.gradientError/leftFit.gradient).pow(2) + (resistance.error/resistance.value).pow(2))
            val tempError    = sqrt(intermediate.pow(2) + leftFit.interceptError.pow(2))

            dataLeft.addData(power.value, temp, tempError)

        }

        for (resistance in powerRight) {

            val power        = resistance.parameters.find { it is Power } ?: continue
            val temp         = rightFit.gradient * resistance.value + rightFit.intercept
            val intermediate = rightFit.gradient * resistance.value * sqrt((rightFit.gradientError/rightFit.gradient).pow(2) + (resistance.error/resistance.value).pow(2))
            val tempError    = sqrt(intermediate.pow(2) + rightFit.interceptError.pow(2))

            dataRight.addData(power.value, temp, tempError)

        }

        val leftPowerFit  = dataLeft.linearFit(0, 1).function
        val rightPowerFit = dataRight.linearFit(0, 1).function

        val extras = ArrayList<Quantity>()

        for ((gate, data) in data.split(TVMeasurement.SET_GATE)) {

            val params = ArrayList(parameters)
            params    += Gate(gate, 0.0)
            val dT     = rightPowerFit.value(data.getColumns(TVMeasurement.HEATER_POWER)) - leftPowerFit.value(data.getColumns(TVMeasurement.HEATER_POWER))
            val fit    = Fitting.linearFit(dT, data.getColumns(TVMeasurement.THERMAL_VOLTAGE))

            if (fit != null) extras += SeebeckCoefficient(fit.gradient, fit.gradientError, params, possibleParameters)

        }

        return extras

    }

}