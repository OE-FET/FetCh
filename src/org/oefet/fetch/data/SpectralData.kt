package org.oefet.fetch.data

import jisa.enums.Icon
import jisa.gui.Colour
import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.TakeSpectra
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.Result
import org.oefet.fetch.quant.Type
import kotlin.math.pow
import kotlin.math.sqrt

class SpectralData(data: ResultTable) : FetChData("Spectral Data", "Spectra", data, Icon.WAVE.blackImage) {

    val NUMBER     = data.findColumn(TakeSpectra.NUMBER)
    val WAVELENGTH = data.findColumn(TakeSpectra.WAVELENGTH)
    val COUNTS     = data.findColumn(TakeSpectra.COUNTS)

    override fun processData(data: ResultTable): List<Result> {

        val list = mutableListOf<Result>()

        for ((wl, wlData) in data.split(WAVELENGTH)) {

            val average = wlData.mean(COUNTS)

            list += Result("Average Count", "mean(C)", Type.COUNT, average, sqrt(wlData.mean { it[COUNTS].pow(2) } - average.pow(2)), parameters + DoubleQuantity("Wavelength", "λ", Type.WAVELENGTH, wl, 0.0))

        }

        return list

    }

    override fun getDisplay(): Element {

        val plot   = FetChPlot("Measured Spectra", "Wavelength [m]", "Counts")
        val series = plot.createSeries().setMarkerVisible(false).setLineWidth(1.0).watch(data, WAVELENGTH, COUNTS).split(NUMBER)

        val avSeries = plot.createSeries()
            .setName("Mean")
            .setMarkerVisible(false)
            .setLineWidth(2.0)
            .setColour(Colour.BLACK)
            .addPoints(
                results.map { it.findDoubleParameter("Wavelength", 0.0).value },
                results.map { it.value }
            )


        return plot

    }

    override fun generateHybrids(results: List<Result>): List<Result> = emptyList()


}