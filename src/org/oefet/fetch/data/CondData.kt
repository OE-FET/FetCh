package org.oefet.fetch.data

import jisa.enums.Icon
import jisa.gui.Element
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type.*
import kotlin.math.absoluteValue

class CondData(data: ResultTable) : FetChData("Conductivity Data", "FPP Conductivity", data, Icon.VOLTMETER.blackImage) {

    val SD_VOLTAGE     = data.findColumn(Conductivity.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Conductivity.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Conductivity.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Conductivity.SG_CURRENT)
    val FPP1_VOLTAGE   = data.findColumn(Conductivity.FPP1_VOLTAGE)
    val FPP2_VOLTAGE   = data.findColumn(Conductivity.FPP2_VOLTAGE)
    val FPP_VOLTAGE    = data.findColumn(Conductivity.FPP_VOLTAGE)
    val TEMPERATURE    = data.findColumn(Conductivity.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Conductivity.GROUND_CURRENT)

    override fun processData(data: ResultTable): List<Result> {

        val length     = findParameter("Length", DoubleQuantity::class)!!
        val width      = findParameter("Width", DoubleQuantity::class)!!
        val thickness  = findParameter("Thickness", DoubleQuantity::class)!!
        val separation = findParameter("FPP Separation", DoubleQuantity::class)!!

        val usedFPP = data.getAttribute("Used FPP")?.toBoolean() ?: true
        val size    = if (usedFPP) separation else length
        val fit     = Fitting.linearFit(data[FPP_VOLTAGE], data[SD_CURRENT])
        val grad    = DoubleQuantity("Gradient", UNKNOWN, fit.gradient.absoluteValue, fit.gradientError.absoluteValue)

        val conductance  = grad.toResult("Conductance", "G", CONDUCTANCE, parameters)
        val resistance   = conductance.pow(-1.0).toResult("Resistance", "R", RESISTANCE, parameters)
        val conductivity = ((size / (width * thickness) / 100.0) * grad).toResult("Conductivity", "σ", CONDUCTIVITY, parameters)
        val resistivity  = conductivity.pow(-1.0).toResult("Resistivity", "ρ", RESISTIVITY, parameters)

        return listOf(conductance, resistance, conductivity, resistivity)

    }

    override fun getDisplay(): Element {
        return FPPPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> {
        return emptyList()
    }

}