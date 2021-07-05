package org.oefet.fetch.sweep


import jisa.devices.interfaces.ProbeStation
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.experiment.queue.SimpleAction
import java.util.*

class PositionSweep : FetChSweep<PositionSweep.Position>("Position Sweep", "P") {
    val countX    by input("Sample Setup", "Number of Devices in x Direction", 6)
    val countY    by input("Sample Setup", "Number of Devices in y Direction", 8)
    val fineLift    by input("Sample Setup", "Fine Lift [m]", 0.02)
    //val returnToStart    by input("Sample Setup", "Return to start at end?", true)


    val position1X    by input("Start Position (top left)", "x start Position [m]", 0.0)
    val position1Y    by input("Start Position (top left)", "y start position [m]", 0.0)
    val measureHeightZ    by input("Start Position (top left)", "z Measurement height [m]", 0.0)

    val position2X    by input("Position 2 (top right)", "x Position 2 [m]", 0.0)
    val position2Y    by input("Position 2 (top right)", "y Position 2 [m]", 0.0)

    val position3X    by input("Position 3 (bottom right)", "x Position 3 [m]", 0.0)
    val position3Y    by input("Position 3 (bottom right)", "y Position 3 [m]", 0.0)

    val directionHorizontalX = position2X - position1X
    val directionHorizontalY = position2Y - position1Y
    val directionVerticalX = position3X - position2X
    val directionVerticalY = position3Y - position2Y

    val pControl by requiredConfig("Position Control", ProbeStation::class)

    var grossLift: Double = measureHeightZ - fineLift





    override fun getValues(): List<Position> {
        val list = ArrayList<Position>()

        for (i in 0 until countX) {
            for (j in 0 until countY) {

                val x = position1X + i * directionHorizontalX / countX  + j * directionVerticalX / countY
                val y = position1Y + i * directionHorizontalY / countX  + j * directionVerticalY / countY
                list += Position(x, y,measureHeightZ - fineLift)

            }

        }

        return list
    }

    override fun generateForValue(value: Position, actions: List<Action<*>>): List<Action<*>> {
        val list = ArrayList<Action<*>>()

        if(list.isNullOrEmpty()){

            println("Start generateForValue")
            //pControl.zFineLift = fineLift
            //pControl.isLocked = false
            //pControl.zPosition = grossLift
        }

        list += SimpleAction("Change Position to ${value.x}, ${value.y} m") {
                //pControl.isLocked = false
                pControl.setXYPosition(value.x,value.y)
                //pControl.isLocked = true
        }
        list += actions


        return list



    }
    override fun formatValue(value: Position): String = "(${value.x},${value.y} )"

    class Position(val x: Double, val y: Double, val z: Double)

}