package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Field
import jisa.gui.Fields
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class PositionCalibration : FetChAction("3-point Position Calibration") {

    val pControl by requiredConfig("Position Control", ProbeStation::class)

    val fast = 100.0
    val middle = 10.0
    val slow = 1.0

    companion object {
        var position1X = 0.0
        var position1Y = 0.0
        var measureHeightZ = 0.0

        var position2X  = 0.0
        var position2Y  = 0.0

        var position3X  = 0.0
        var position3Y = 0.0
    }

    override fun run(results: ResultTable) {

        val calibration = Fields("Calibration")

        val leftContinFast  = calibration.addCheckBox("Left (fast)")
        val leftContinMiddle  = calibration.addCheckBox("Left (medium)")
        val leftContinSlow  = calibration.addCheckBox("Left (slow)")

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
        calibration.show()
    }

    protected fun continControl(box: Field<Boolean>, axis: String, direction : Int, speed : Double){
        box.setOnChange {
            if (box.get()) {
                pControl.continMovement(axis, speed * direction)
            } else pControl.continMovement(axis, 0.0)
        }
    }

}