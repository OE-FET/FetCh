package org.oefet.fetch.sweep

import jisa.devices.interfaces.Camera
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.*
import org.oefet.fetch.action.PositionCalibration


class PositionSweep : FetChSweep<PositionSweep.Position>("Position Sweep", "P") {

    val fineLift by input("Sample Setup", "Fine Lift [m]", 0.02)
    val useCalibration by input("Sample Setup", "Use values from 3-point calibration", true)
    //val returnToStart    by input("Sample Setup", "Return to start at end?", true)


    var position1X by input("Start Position (top left)", "X [m]", 0.0)
    var position1Y by input("Start Position (top left)", "Y [m]", 0.0)
    var measureHeightZ by input("Start Position (top left)", "Z [m]", 0.0)

    var position2X by input("Position 2 (top right)", "X [m]", 0.0)
    var position2Y by input("Position 2 (top right)", "Y [m]", 0.0)

    var position3X by input("Position 3 (bottom right)", "X [m]", 0.0)
    var position3Y by input("Position 3 (bottom right)", "Y [m]", 0.0)


    val pControl by requiredConfig("Position Control", ProbeStation::class)
    val camera by optionalConfig("Camera", Camera::class)


    val counts      = Fields("Counts")
    val countXParam = counts.addIntegerField("No. X", 8)
    val countYParam = counts.addIntegerField("No. Y", 6)
    val checkGrid   = CheckGrid("Active Devices", countXParam.value, countYParam.value)

    val countX get() = countXParam.value
    val countY get() = countYParam.value

    init {
        countXParam.setOnChange { checkGrid.setSize(countXParam.value, countYParam.value) }
        countYParam.setOnChange { checkGrid.setSize(countXParam.value, countYParam.value) }
    }


    override fun getExtraTabs(): List<Element> {
        val feed = CameraFeed("Camera", camera)
        feed.start()
        return listOf(feed)
    }

    override fun getCustomParams(): List<Element> {
        return listOf(Grid(counts, checkGrid))
    }

    override fun getValues(): List<Position> {

        if (useCalibration) {
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
                if (checkGrid.isChecked(i, j)) {
                    list += Position(
                        position1X + i * directionHorizontalX / (countX - 1) + j * directionVerticalX / (countY - 1),
                        position1Y + i * directionHorizontalY / (countX - 1) + j * directionVerticalY / (countY - 1),
                        i,
                        j
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

        list += SimpleAction("Change Position to (${value.nx}, ${value.ny})") {
            pControl.isLocked = false
            pControl.zPosition = 0.0
            pControl.setXYPosition(value.x, value.y)
            pControl.zPosition = grossLift
            pControl.isLocked = true
        }
        list += actions


        return list


    }

    override fun formatValue(value: Position): String = "(${value.nx}, ${value.ny})"

    class Position(val x: Double, val y: Double, val nx: Int, val ny: Int)

}