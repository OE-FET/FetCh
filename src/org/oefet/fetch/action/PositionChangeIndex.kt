package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class PositionChangeIndex : FetChAction("Change Position to Index") {

    val pControl    by requiredConfig("Position Controller", ProbeStation::class)
    val countX      by input("Sample Setup", "Number of Devices in x Direction", 6)
    val countY      by input("Sample Setup", "Number of Devices in y Direction", 8)
    val i           by input("Position", "x Position (index)", 0)
    val j           by input("Position", "y Position (index)", 0)
    val fineLift    by input("Sample Setup", "Fine Lift [m]", 0.02)



    override fun run(results: ResultTable) {
        val position1X = PositionCalibration.position1X
        val position1Y = PositionCalibration.position1Y
        val measureHeightZ = PositionCalibration.measureHeightZ
        val position2X = PositionCalibration.position2X
        val position2Y = PositionCalibration.position2Y
        val position3X = PositionCalibration.position3X
        val position3Y = PositionCalibration.position3Y

        val directionHorizontalX = position2X - position1X
        val directionHorizontalY = position2Y - position1Y
        val directionVerticalX = position3X - position2X
        val directionVerticalY = position3Y - position2Y
        val grossLift = measureHeightZ - fineLift

        pControl.isLocked  = false
        pControl.zPosition = 0.0
        pControl.setXYPosition(
            position1X + i * directionHorizontalX / (countX - 1) + j * directionVerticalX / (countY - 1),
            position1Y + i * directionHorizontalY / (countX - 1) + j * directionVerticalY / (countY - 1),
        )
        pControl.zPosition = grossLift
        pControl.isLocked = true






    }


}