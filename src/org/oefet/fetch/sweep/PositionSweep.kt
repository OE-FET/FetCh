package org.oefet.fetch.sweep

import jisa.devices.interfaces.Camera
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.CheckGrid
import jisa.gui.Fields
import org.oefet.fetch.action.PositionCalibration


class PositionSweep : FetChSweep<PositionSweep.Position>("Position Sweep", "P") {

    val fineLift by userInput("Sample Setup", "Fine Lift [m]", 0.02)
    val useCalibration by userInput("Sample Setup", "Use values from 3-point calibration", true)
    //val returnToStart    by input("Sample Setup", "Return to start at end?", true)


    var position1XInput     by userInput("Start Position (top left)", "X [m]", 0.0)
    var position1YInput     by userInput("Start Position (top left)", "Y [m]", 0.0)
    var measureHeightZInput by userInput("Start Position (top left)", "Z [m]", 0.0)

    var position2XInput by userInput("Position 2 (top right)", "X [m]", 0.0)
    var position2YInput by userInput("Position 2 (top right)", "Y [m]", 0.0)

    var position3XInput by userInput("Position 3 (bottom right)", "X [m]", 0.0)
    var position3YInput by userInput("Position 3 (bottom right)", "Y [m]", 0.0)

    val pControl by requiredInstrument("Position Control", ProbeStation::class)
    val camera   by optionalInstrument("Camera", Camera::class)

    // Custom input fields
    val counts      = Fields("Counts")
    val countXParam = counts.addIntegerField("No. X", 8)
    val countYParam = counts.addIntegerField("No. Y", 6)
    val checkGrid   = CheckGrid("Active Devices", countXParam.value, countYParam.value)

    init {
        countXParam.setOnChange { checkGrid.setSize(countXParam.value, countYParam.value) }
        countYParam.setOnChange { checkGrid.setSize(countXParam.value, countYParam.value) }
    }

    val countX  by customInput(counts, countXParam)
    val countY  by customInput(counts, countYParam)
    val checked by customInput(checkGrid)

    override fun getValues(): List<Position> {

        val position1X: Double
        val position1Y: Double
        val measureHeightZ: Double
        val position2X: Double
        val position2Y: Double
        val position3X: Double
        val position3Y: Double

        if (useCalibration) {
            position1X = PositionCalibration.position1X
            position1Y = PositionCalibration.position1Y
            measureHeightZ = PositionCalibration.measureHeightZ
            position2X = PositionCalibration.position2X
            position2Y = PositionCalibration.position2Y
            position3X = PositionCalibration.position3X
            position3Y = PositionCalibration.position3Y
        }
        else{
            position1X = position1XInput
            position1Y = position1YInput
            measureHeightZ = measureHeightZInput
            position2X = position2XInput
            position2Y = position2YInput
            position3X = position3XInput
            position3Y = position3YInput
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
                        measureHeightZ,
                        i,
                        j
                    )

                    print(j * countX + i)
                    print("\t")
                    print(position1X + i * directionHorizontalX / (countX - 1) + j * directionVerticalX / (countY - 1))
                    print("\t")
                    println(position1Y + i * directionHorizontalY / (countX - 1) + j * directionVerticalY / (countY - 1))

                }

            }

        }

        return list
    }

    override fun generateForValue(value: Position, actions: List<Action<*>>): List<Action<*>> {
        val list = ArrayList<Action<*>>()
        val grossLift: Double = value.z - fineLift

        list += SimpleAction("Change Position to (${value.nx}, ${value.ny})") {
            pControl.isLocked = false
            pControl.lockDistance = fineLift
            pControl.zPosition = 0.0
            pControl.setXYPosition(value.x, value.y)
            pControl.zPosition = grossLift
            pControl.isLocked = true
        }
        list += actions


        return list


    }

    override fun formatValue(value: Position): String = "(${value.nx}, ${value.ny})"

    class Position(val x: Double, val y: Double, val z: Double, val nx: Int, val ny: Int)

}