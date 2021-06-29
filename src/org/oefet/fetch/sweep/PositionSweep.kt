package org.oefet.fetch.sweep


import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation
//todo:remove
import jisa.devices.interfaces.TC
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.gui.Series
import jisa.maths.Range
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.gui.tabs.Measure
import java.util.*

class PositionSweep : FetChSweep<Int>("Position Sweep", "P") {
    //TODO:change class
    val interval      by input("Sample Setup", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val xdistance      by input("Sample Setup", "x distance between devices [TBD]", 0.5)
    val ydistance      by input("Sample Setup", "y distance between devices [TBD]", 0.5)

    //var xstart      by input("Position Control", "x start position [TBD]", 0.5)
    //val ystart      by input("Position Control", "y start position [TBD]", 0.5)

    val positionControl by requiredConfig("Position Control", ProbeStation::class)
    var xstart: Double = 0.0
    var ystart: Double = 0.0

    override fun getValues(): List<Int> {
        //return Range.step(0, +47, 1).array().toList()
        return (0..47).toList()
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {
        val list = LinkedList<Action<*>>()
        list += MeasurementAction(SweepPoint(value,xstart,ystart,xdistance,ydistance,interval,positionControl))
        list += actions
        return list

    }
    override fun formatValue(value: Int): String = "$value"


    class SweepPoint(val position: Int, var xstart: Double,var ystart: Double,val xdistance: Double,val ydistance: Double, val interval: Long, val qControl: ProbeStation?) : FetChAction("Change Position") {
        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {
            println("start plot")
            val plot = FetChPlot("Change position to index (${position%8}, ${position/8}) ", "Time [s]", "Index")

            plot.createSeries()
                .watch(data, { it[0] }, { position.toDouble() })
                .setMarkerVisible(false)
                .setLineWidth(1.0)
                .setLineDash(Series.Dash.DASHED)
                .setColour(Colour.GREY)

            plot.createSeries()
                .watch(data, 0, 1)
                .setMarkerVisible(false)
                .setColour(Colour.PURPLE)

            plot.isLegendVisible = false

            return plot

        }

        override fun run(results: ResultTable) {
            println("run")
            if (qControl == null) {
                throw Exception("Station is not configured.")
            }

            if(position==0){
                xstart = qControl.xposition
                ystart = qControl.yposition

            }

            task = RTask(interval) { t -> results.addData(t.secFromStart, position.toDouble())}
            //also with y-position? or everthing in one?
            task?.start()

            qControl.ChuckFineUp()
            qControl.ChuckGrossUp()
            sleep(100)

            qControl.xposition = xstart + (position % 8) * xdistance
            qControl.yposition = ystart + ( position / 8 ) * ydistance
            sleep(100)

            qControl.ChuckGrossDown()
            qControl.ChuckFineDown()



        }

        override fun onFinish() {
            println("onFinish")
            task?.stop()
        }

        override fun getColumns(): Array<Col> {
            println("getColumns")
            return arrayOf(
                Col("Time", "s"),
                Col("Index", " ")
            )

        }

        override fun getLabel(): String {
            return "${position%8}, ${position/8}"
        }

    }


}