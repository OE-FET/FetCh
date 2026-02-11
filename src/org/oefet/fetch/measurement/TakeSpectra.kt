package org.oefet.fetch.measurement

import jisa.devices.spectrometer.Spectrometer
import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.data.SpectralData
import org.oefet.fetch.gui.elements.FetChPlot

class TakeSpectra : FetChMeasurement("Take Spectra", "Spectra") {

    val count by userInput("Basic", "Count", 1)
    val delay by userTimeInput("Basic", "Delay", 0)

    val channel by requiredInstrument("Spectrometer Channel", Spectrometer::class)

    companion object : Columns() {

        val NUMBER     = integerColumn("Spectrum Number")
        val TIMESTAMP  = longColumn("Timestamp")
        val WAVELENGTH = decimalColumn("Wavelength", "m")
        val COUNTS     = decimalColumn("Counts")

    }

    override fun run(results: ResultTable) {

        for (i in 0 until count) {

            sleep(delay)

            val spectrum = channel.getSpectrum()

            results.mapRows(
                NUMBER     to List(spectrum.size()) { i },
                TIMESTAMP  to List(spectrum.size()) { spectrum.timestamp },
                WAVELENGTH to spectrum.listWavelengths(),
                COUNTS     to spectrum.listCounts()
            )

            checkPoint()

        }

    }

    override fun onFinish() {

    }

    override fun createDisplay(data: ResultTable): Element {

        val plot = FetChPlot("Measured Spectra", "Wavelength [m]", "Counts")
        val series = plot.createSeries().setMarkerVisible(false).setLineWidth(1.0).watch(data, WAVELENGTH, COUNTS).split(NUMBER)

        return plot

    }

    override fun processResults(data: ResultTable): SpectralData {
        return SpectralData(data)
    }

}