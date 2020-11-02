package org.oefet.fetch.gui.tabs

import jisa.gui.Grid
import jisa.gui.Plot
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.measurement.FetChMeasurement

object Logs : Grid("Logs", 3) {

    private val plots = ArrayList<Plot>()

    fun loadMeasurement(measurement: FetChMeasurement) {

        clear()

        val log = measurement.log

        for (i in 1..log.names.size) {

            val name = log.getTitle(i)
            val plot = FetChPlot(name, "Time [ms]", name)

            plot.createSeries()
                .watch(log, 0, i)
                .setMarkerVisible(false)
                .setLineVisible(true)

            plots += plot
            add(plot)

        }

    }

}