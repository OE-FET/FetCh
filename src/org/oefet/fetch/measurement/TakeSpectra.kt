package org.oefet.fetch.measurement

import jisa.devices.spectrometer.Spectrometer
import jisa.gui.Element
import jisa.results.ResultTable
import org.oefet.fetch.data.SpectralData
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.results.FetChResult
import org.oefet.fetch.results.SpectraResult

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

            for (point in spectrum) {

                results.mapRow(
                    NUMBER     to i,
                    TIMESTAMP  to spectrum.timestamp,
                    WAVELENGTH to point.wavelength,
                    COUNTS     to point.counts
                )

            }

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