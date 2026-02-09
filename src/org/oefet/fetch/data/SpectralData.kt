package org.oefet.fetch.data

import jisa.enums.Icon
import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.TakeSpectra
import org.oefet.fetch.quant.Result

class SpectralData(data: ResultTable) : FetChData("Spectral Data", "Spectra", data, Icon.WAVE.blackImage) {

    val NUMBER = data.findColumn(TakeSpectra.NUMBER)
    val WAVELENGTH = data.findColumn(TakeSpectra.WAVELENGTH)
    val COUNTS = data.findColumn(TakeSpectra.COUNTS)

    override fun processData(data: ResultTable): List<Result> {

//        var spectrum: Spectrum? = null
//
//        var count = 0
//        for ((n, wlData) in data.findGroups(NUMBER)) {
//
//            spectrum = spectrum?.add(Spectrum(wlData[WAVELENGTH], wlData[COUNTS])) ?: Spectrum(wlData[WAVELENGTH], wlData[COUNTS])
//            count++
//
//        }
//
//        return spectrum?.map { p ->  Result("Mean Count", "<cnt>", Type.COUNT, p.counts / count, 0.0, parameters + DoubleQuantity("Wavelength", "λ", Type.WAVELENGTH, p.wavelength, 0.0))} ?: emptyList()

        return emptyList()

    }

    override fun getDisplay(): Element {

        val plot = FetChPlot("Measured Spectra", "Wavelength [m]", "Counts")
        val series = plot.createSeries().setMarkerVisible(false).setLineWidth(1.0).watch(data, WAVELENGTH, COUNTS)
            .split(NUMBER)

//        val avSeries = plot.createSeries()
//            .setName("Mean")
//            .setMarkerVisible(false)
//            .setLineWidth(2.0)
//            .setColour(Colour.BLACK)
//            .addPoints(
//                results.map { it.findDoubleParameter("Wavelength", 0.0).value },
//                results.map { it.value }
//            )


        return plot

    }

    override fun generateHybrids(results: List<Result>): List<Result> = emptyList()


}