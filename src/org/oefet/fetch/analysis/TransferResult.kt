package org.oefet.fetch.analysis

import javafx.scene.image.Image
import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.gui.Series
import jisa.maths.interpolation.Interpolation
import jisa.maths.functions.Function
import org.oefet.fetch.*
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.elements.TransferPlot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class TransferResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = TransferPlot(data).apply { legendRows = data.getUniqueValues(SET_SD).size }
    override val name       = "Transfer Measurement (${data.getAttribute("Name")})"
    override val image      = Image(MainWindow.javaClass.getResourceAsStream("transfer.png"))

    private val possibleParameters = listOf(
        Temperature::class,
        Repeat::class,
        Time::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class,
        Gate::class,
        Drain::class
    )

    init {

        if (data.getAttribute("Type") != "Transfer") {
            throw Exception("That is not a transfer curve file")
        }

        val length        = data.getAttribute("Length").removeSuffix("m").toDouble()
        val separation    = data.getAttribute("FPP Separation").removeSuffix("m").toDouble()
        val width         = data.getAttribute("Width").removeSuffix("m").toDouble()
        val thickness     = data.getAttribute("Thickness").removeSuffix("m").toDouble()
        val dielectric    = data.getAttribute("Dielectric Thickness").removeSuffix("m").toDouble()
        val permittivity  = data.getAttribute("Dielectric Permittivity").toDouble()

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

        val capacitance = permittivity * EPSILON / dielectric

        try {

            var maxLinMobility = 0.0
            var maxSatMobility = 0.0

            for ((drain, data) in data.split(SET_SD)) {

                val fb = data.splitTwoWaySweep { it[SET_SG] }

                val function: Function
                val gradFwd: Function?
                val gradBwd: Function?

                val vGFwd  = fb.forward.getColumns(SET_SG)
                val iDFwd  = fb.forward.getColumns(SD_CURRENT)
                val vGBwd  = fb.backward.getColumns(SET_SG)
                val iDBwd  = fb.backward.getColumns(SD_CURRENT)
                val linear = abs(drain) < data.getMax { abs(it[SET_SG]) }

                if (linear) {

                    gradFwd = if (fb.forward.numRows > 1) {

                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> abs(x) }).derivative()

                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {

                        Interpolation.interpolate1D(vGBwd, iDBwd.map { x -> abs(x) }).derivative()

                    } else null

                    function = Function { 1e4 * abs((length / (capacitance * width)) * (it / drain)) }

                } else {

                    gradFwd = if (fb.forward.numRows > 1) {
                        Interpolation.interpolate1D(vGFwd, iDFwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    gradBwd = if (fb.backward.numRows > 1) {
                        Interpolation.interpolate1D(vGBwd, iDBwd.map { x -> sqrt(abs(x)) }).derivative()
                    } else null

                    function = Function { 1e4 * 2.0 * it.pow(2) * length / (width * capacitance) }

                }

                for (gate in data.getUniqueValues(SET_SG).sorted()) {

                    val params = ArrayList(parameters)
                    params    += Gate(gate, 0.0)
                    params    += Drain(drain, 0.0)

                    if (gradFwd != null) quantities += if (linear) {
                        maxLinMobility = max(maxLinMobility, function.value(gradFwd.value(gate)))
                        FwdLinMobility(function.value(gradFwd.value(gate)), 0.0, params, possibleParameters)
                    } else {
                        maxSatMobility = max(maxSatMobility, function.value(gradFwd.value(gate)))
                        FwdSatMobility(function.value(gradFwd.value(gate)), 0.0, params, possibleParameters)
                    }

                    if (gradBwd != null) quantities += if (linear) {
                        maxLinMobility = max(maxLinMobility, function.value(gradBwd.value(gate)))
                        BwdLinMobility(function.value(gradBwd.value(gate)), 0.0, params, possibleParameters)
                    } else {
                        maxSatMobility = max(maxSatMobility, function.value(gradBwd.value(gate)))
                        BwdSatMobility(function.value(gradBwd.value(gate)), 0.0, params, possibleParameters)
                    }

                }

            }

            if (maxLinMobility > 0) quantities += MaxLinMobility(maxLinMobility, 0.0, parameters, possibleParameters)
            if (maxSatMobility > 0) quantities += MaxSatMobility(maxSatMobility, 0.0, parameters, possibleParameters)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun calculateHybrids(quantities: List<Quantity>): List<Quantity> = emptyList()

}