package fetter.gui

import jisa.control.RTask
import jisa.devices.TMeter
import jisa.gui.Colour
import jisa.gui.Plot
import jisa.gui.Series

object TempChangePlot : Plot("Temperature", "Time [s]", "Temperature [K]") {

    private var task: RTask? = null

    private val tempSeries = createSeries()
        .setName("Temperature")
        .showMarkers(false)
        .setColour(Colour.BLUE)

    private val targSeries = createSeries()
        .setName("Set-Point")
        .showMarkers(false)
        .setColour(Colour.CORNFLOWERBLUE)
        .setLineDash(Series.Dash.DOTTED)


    fun start(T: Double, tm: TMeter) {

        tempSeries.clear()
        targSeries.clear()

        task = RTask(1000) { task ->
            tempSeries.addPoint(task.secFromStart, tm.temperature)
            targSeries.addPoint(task.secFromStart, T)
        }

        task?.start()

    }

    fun stop() {
        task?.stop()
    }

}