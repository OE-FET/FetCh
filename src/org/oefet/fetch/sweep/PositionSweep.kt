package org.oefet.fetch.sweep

import jisa.devices.interfaces.Camera
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.*
import org.oefet.fetch.action.PositionCalibration
import java.util.*


class PositionSweep : FetChSweep<PositionSweep.Position>("Position Sweep", "P") {
    val countX    by input("Sample Setup", "Number of Devices in x Direction", 6)
    val countY    by input("Sample Setup", "Number of Devices in y Direction", 8)
    val fineLift    by input("Sample Setup", "Fine Lift [m]", 0.02)
    val useCalibration    by input("Sample Setup", "Use values from 3-point calibration", true)
    //val returnToStart    by input("Sample Setup", "Return to start at end?", true)



    var position1X    by input("Start Position (top left)", "x start Position [m]", 0.0)
    var position1Y    by input("Start Position (top left)", "y start position [m]", 0.0)
    var measureHeightZ    by input("Start Position (top left)", "z Measurement height [m]", 0.0)

    var position2X    by input("Position 2 (top right)", "x Position 2 [m]", 0.0)
    var position2Y    by input("Position 2 (top right)", "y Position 2 [m]", 0.0)

    var position3X    by input("Position 3 (bottom right)", "x Position 3 [m]", 0.0)
    var position3Y    by input("Position 3 (bottom right)", "y Position 3 [m]", 0.0)



    val pControl by requiredConfig("Position Control", ProbeStation::class)
    val camera   by optionalConfig("Camera", Camera::class)


    var checkGrid = CheckGrid("Select active devices",countX,countY)


    override fun getExtraTabs(): List<Element> {
        val feed = CameraFeed("Camera", camera)
        checkGrid = CheckGrid("Select active devices",countX,countY)
        feed.start()
        return listOf(feed,checkGrid)
    }



    override fun getValues(): List<Position> {

        if(useCalibration){
            position1X = PositionCalibration.position1X
            position1Y = PositionCalibration.position1Y
            measureHeightZ = PositionCalibration.measureHeightZ
            position2X = PositionCalibration.position2X
            position2Y = PositionCalibration.position2Y
            position3X = PositionCalibration.position3X
            position3Y = PositionCalibration.position3Y
        }



        val list = ArrayList<Position>()
        val directionHorizontalX = position2X - position1X
        val directionHorizontalY = position2Y - position1Y
        val directionVerticalX = position3X - position2X
        val directionVerticalY = position3Y - position2Y


        for (j in 0 until countY) {
            for (i in 0 until countX) {
            if(checkGrid.isChecked(i,j)) {
                    list += Position(
                        position1X + i * directionHorizontalX / (countX - 1) + j * directionVerticalX / (countY - 1),
                        position1Y + i * directionHorizontalY / (countX - 1) + j * directionVerticalY / (countY - 1),
                        measureHeightZ - fineLift,
                        j * countX + i
                    )
                    println(j * countX + i)
                }
            }

        }

        return list
    }

    override fun generateForValue(value: Position, actions: List<Action<*>>): List<Action<*>> {
        val list = ArrayList<Action<*>>()
        val grossLift: Double = measureHeightZ - fineLift

        pControl.lockDistance = fineLift

        list += SimpleAction("Change Position to ${value.x}, ${value.y} m, index ${value.index}") {
                pControl.isLocked = false
                pControl.zPosition = 0.0
                pControl.setXYPosition(value.x,value.y)
                pControl.zPosition = grossLift
                pControl.isLocked = true
        }
        list += actions


        return list



    }
    override fun formatValue(value: Position): String = "(${value.x} m ,${value.y} m, index ${value.index} )"

    class Position(val x: Double, val y: Double, val z: Double, val index: Int)

}