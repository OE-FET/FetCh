package org.oefet.fetch.sweep


import javafx.scene.control.CheckBox
import jisa.devices.interfaces.Camera
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.*
import java.util.*


class PositionSweep : FetChSweep<PositionSweep.Position>("Position Sweep", "P") {
    val countX    by input("Sample Setup", "Number of Devices in x Direction", 6)
    val countY    by input("Sample Setup", "Number of Devices in y Direction", 8)
    val fineLift    by input("Sample Setup", "Fine Lift [m]", 0.02)
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

    val fast = 100.0
    val middle = 10.0
    val slow = 1.0


    override fun getExtraTabs(): List<Element> {
        val feed = CameraFeed("Camera", camera)
        feed.start()

        val calibration = Fields("Calibration")

        val leftContinFast  = calibration.addCheckBox("Left (fast)")
        val leftContinMiddle  = calibration.addCheckBox("Left (fast)")
        val leftContinSlow  = calibration.addCheckBox("Left (fast)")
        calibration.addSeparator();

        continControl(leftContinFast,"X", -1,fast)
        continControl(leftContinMiddle,"X", -1,middle)
        continControl(leftContinSlow,"X", -1,slow)


        calibration.addToolbarButton("Calibrate as postion 1 (x,y,z), top left") {
            position1X = pControl.xPosition
            position1Y = pControl.yPosition
            measureHeightZ = pControl.zPosition
            println(position1X)
        }

        calibration.addToolbarButton("Calibrate as postion 2 (x,y), top right") {
            position2X = pControl.xPosition
            position2Y = pControl.yPosition
        }

        calibration.addToolbarButton("Calibrate as postion 3 (x,y), bottom right") {
            position3X = pControl.xPosition
            position3Y = pControl.yPosition
        }

        return listOf(feed,calibration)
    }

    protected fun continControl(box: Field<Boolean>, axis: String, direction : Int,speed : Double){
        box.setOnChange {
            if (box.get()) {
                pControl.continMovement(axis, speed * direction)
            } else pControl.continMovement(axis, 0.0)
        }
    }

    override fun getValues(): List<Position> {
        val list = ArrayList<Position>()
        val directionHorizontalX = position2X - position1X
        val directionHorizontalY = position2Y - position1Y
        val directionVerticalX = position3X - position2X
        val directionVerticalY = position3Y - position2Y

        print(directionHorizontalX)

        for (j in 0 until countY) {
            for (i in 0 until countX) {

                list += Position(
                    position1X + i * directionHorizontalX / (countX-1) + j * directionVerticalX / (countY-1),
                    position1Y + i * directionHorizontalY / (countX-1) + j * directionVerticalY / (countY-1),
                    measureHeightZ - fineLift
                )

            }

        }

        return list
    }

    override fun generateForValue(value: Position, actions: List<Action<*>>): List<Action<*>> {
        val list = ArrayList<Action<*>>()
        val grossLift: Double = measureHeightZ - fineLift

        pControl.zFineLift = fineLift

        list += SimpleAction("Change Position to ${value.x}, ${value.y} m") {
                pControl.isLocked = false
                pControl.zPosition = 0.0
                pControl.setXYPosition(value.x,value.y)
                pControl.zPosition = grossLift
                pControl.isLocked = true
        }
        list += actions


        return list



    }
    override fun formatValue(value: Position): String = "(${value.x},${value.y} )"

    class Position(val x: Double, val y: Double, val z: Double)

}