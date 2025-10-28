package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVMeasurement
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.StringQuantity
import org.oefet.fetch.quant.Type
import kotlin.math.pow

class TVData(data: ResultTable) : FetChData("Thermal Voltage Measurement", "Thermal Voltage", data, Images.getImage("fire.png")) {

    val SET_GATE        = data.findColumn(TVMeasurement.SET_GATE)
    val TEMPERATURE     = data.findColumn(TVMeasurement.TEMPERATURE)
    val HEATER_POWER    = data.findColumn(TVMeasurement.HEATER_POWER)
    val THERMAL_VOLTAGE = data.findColumn(TVMeasurement.THERMAL_VOLTAGE)

    override fun processData(data: ResultTable): List<Result> {

        val list = mutableListOf<Result>()

        for ((gate, data) in data.split(SET_GATE)) {

            val params = parameters + DoubleQuantity("Gate Voltage", Type.VOLTAGE, gate)
            val fit    = Fitting.linearFit(data, HEATER_POWER, THERMAL_VOLTAGE) ?: continue

            list += Result("Seebeck Power Coefficient", "Sp", Type.SEEBECK_POWER_COEFFICIENT, fit.gradient, fit.gradientError, params)

        }

        return list

    }

    override fun getDisplay(): Element {
        return TVPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> {

        val list = mutableListOf<Result>()

        for (seebeckPower in results.filter { it.type == Type.SEEBECK_POWER_COEFFICIENT }) {

            val resistances = results.filter { it.name == "T-Probe Resistance" && it.findParameter("Name", StringQuantity::class)?.value == seebeckPower.findParameter("Name", StringQuantity::class)?.value }
            val probes      = resistances.groupBy { it.findDoubleParameter("Probe Number", -1.0).value }.filter { it.key >= 0}

            val baseGradients = probes.mapValues { (pn, data) ->

                val base = data.filter { it.findDoubleParameter("Heater Power", -1.0).value == 0.0 }

                Fitting.linearFit(base.map { it.value }, base.map { it.findDoubleParameter("Temperature", 0.0).value })

            }

            val temperatures = probes.mapValues { (pn, data) ->

                val fit       = baseGradients[pn]!!
                val gradient  = DoubleQuantity("Gradient", fit.gradient, fit.gradientError)
                val intercept = DoubleQuantity("Intercept", fit.intercept, fit.interceptError)

                data.filter { it.findDoubleParameter("Temperature", -1.0).value == seebeckPower.findDoubleParameter("Temperature", -2.0).value }.associate {

                    it.findDoubleParameter("Heater Power", Double.NaN).value to (it * gradient + intercept)
                }

            }

            val diffs     = temperatures[0.0]!!.entries.zip(temperatures[1.0]!!.values).associate { (E, T2) -> E.key to E.value - T2 }
            val diffFit   = Fitting.linearFitWeighted(diffs.values.map { it.value }, diffs.keys, diffs.values.map { it.error.pow(-2) }) ?: Fitting.linearFit(diffs.values.map { it.value }, diffs.keys) ?: continue
            val gradient  = DoubleQuantity("Gradient", diffFit.gradient, diffFit.gradientError)
            val intercept = DoubleQuantity("Intercept", diffFit.intercept, diffFit.interceptError)

            val seebeck   = (seebeckPower * gradient + intercept).toResult("Seebeck Coefficient", "S", Type.SEEBECK_COEFFICIENT, seebeckPower.parameters)

            list += seebeck

        }

        return list

    }

}