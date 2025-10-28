package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.results.ResultTable
import org.oefet.fetch.measurement.TakeSpectra
import org.oefet.fetch.quantities.Quantity

class SpectraResult(data: ResultTable) : FetChResult("Spectral Data", "Spectra", Icon.WAVE.blackImage, data) {

    val NUMBER     = data.findColumn(TakeSpectra.NUMBER)
    val WAVELENGTH = data.findColumn(TakeSpectra.WAVELENGTH)
    val COUNTS     = data.findColumn(TakeSpectra.COUNTS)

    override fun calculateHybrids(otherQuantities: List<Quantity<*>>): List<Quantity<*>> {
        return emptyList()
    }

//    override fun getPlot(): FetChPlot? {
//
//        val plot = FetChPlot("Measured Spectra", "Wavelength [m]", "Counts")
//        val series = plot.createSeries().setMarkerVisible(false).setLineWidth(1.0).watch(data, WAVELENGTH, COUNTS).split(NUMBER)
//
//        return plot
//
//    }

}