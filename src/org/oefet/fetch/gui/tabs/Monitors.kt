package org.oefet.fetch.gui.tabs

import jisa.control.Connection
import jisa.devices.camera.Camera
import jisa.devices.spectrometer.Spectrometer
import jisa.enums.Icon
import jisa.gui.Grid
import jisa.gui.ImageDisplay
import jisa.gui.Plot
import jisa.gui.Series
import org.oefet.fetch.Settings

object Monitors : Grid("Monitors", 3) {

    init {

        numColumns = Settings.dashboard.intValue("columns").getOrDefault(if (Settings.wide) 3 else 1)

        setIcon(Icon.WAVE)
        setGrowth(false, false)

        setUp()

        Connection.addListener(::setUp)

    }

    fun setUp() {

        clear()


        for ((i, connection) in Connection.getAllConnections().withIndex()) {

            if (!connection.isConnected) continue
            val inst = connection.instrument
            val name = "${connection.name} (${inst?.javaClass?.simpleName ?: "NULL"})"

            when (inst) {

                is Camera<*>    -> {

                    val disp = ImageDisplay(name)
                    inst.addFrameListener(disp::drawFrame)
                    disp.addToolbarButton("Start", inst::startAcquisition)
                    disp.addToolbarButton("Stop", inst::stopAcquisition)
                    add(disp)

                }

                is Spectrometer -> {

                    val plot = Plot(name, "Wavelength [m]", "Counts")
                    val series = plot.createSeries().setMarkerVisible(false).setLineWidth(1.0).setColour(Series.defaultColours[i % Series.defaultColours.size])
                    inst.addSpectrumListener(series::plotSpectrum)
                    plot.addToolbarButton("Start", inst::startAcquisition)
                    plot.addToolbarButton("Stop", inst::stopAcquisition)
                    plot.isLegendVisible = false
                    add(plot)

                }

            }

        }

    }

}