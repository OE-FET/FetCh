package org.oefet.fetch.data

import jisa.gui.Element
import jisa.maths.fits.Fitting
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVHTPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVHighThroughput
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type
import kotlin.math.abs

class TVHighThroughputData(data: ResultTable) : FetChData("Thermal Voltage High Throughput", "Thermal Voltage High Throughput", data, Images.getImage("fire.png")) {

    val VOLTAGE                = data.findColumn(TVHighThroughput.VOLTAGE)
    val TEMPERATURE_DIFFERENCE = data.findColumn(TVHighThroughput.TEMPERATURE_DIFFERENCE)

    override fun processData(data: ResultTable): List<Result> {

        val fit   = Fitting.linearFit(data.toMatrix(TEMPERATURE_DIFFERENCE), data.toMatrix(VOLTAGE))
        val value = abs(fit.gradient)
        val error = abs(fit.gradientError)

        return listOf(Result("Seebeck Coefficient", "S", Type.SEEBECK_COEFFICIENT, value, error, parameters))

    }

    override fun getDisplay(): Element {
        return TVHTPlot(data)
    }

    override fun generateHybrids(results: List<Result>): List<Result> = emptyList()

}