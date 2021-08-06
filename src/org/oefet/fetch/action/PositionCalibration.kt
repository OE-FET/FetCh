package org.oefet.fetch.action

import jisa.devices.interfaces.ProbeStation
import jisa.gui.Field
import jisa.gui.Fields
import jisa.results.ResultTable

class PositionCalibration : FetChAction("3-point Position Calibration") {

    private val grossLift    by userInput("Setup", "Gross Distance [m]", 3e-3)
    private val fineLift    by userInput("Setup", "Fine Distance [m]", 3e-4)

    private val pControl by requiredInstrument("Position Control", ProbeStation::class)

    val fast = 3000.0
    val middle = 500.0
    val slow = 10.0

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

        pControl.lockDistance = fineLift
        pControl.setGrossUpDistance(grossLift)


        val calibration = Fields("Calibration")

        val leftContinFast  = calibration.addCheckBox("Left (fast)")
        val leftContinMiddle  = calibration.addCheckBox("Left (medium)")
        val leftContinSlow  = calibration.addCheckBox("Left (slow)")
        calibration.addSeparator()
        val rightContinFast  = calibration.addCheckBox("Right (fast)")
        val rightContinMiddle  = calibration.addCheckBox("Right (medium)")
        val rightContinSlow  = calibration.addCheckBox("Right (slow)")
        calibration.addSeparator()
        val forwardContinFast  = calibration.addCheckBox("Forward (fast)")
        val forwardContinMiddle  = calibration.addCheckBox("Forward (medium)")
        val forwardContinSlow  = calibration.addCheckBox("Forward (slow)")
        calibration.addSeparator()
        val backwardContinFast  = calibration.addCheckBox("Backward (fast)")
        val backContinMiddle  = calibration.addCheckBox("Backward (medium)")
        val backContinSlow  = calibration.addCheckBox("Backward (slow)")

        val Load  = calibration.addDialogButton("Load Position"){
            pControl.goLoadPosition()
        }

        val Center  = calibration.addDialogButton("Center Position"){
            pControl.goProbingZoneCentre()
        }

        calibration.addSeparator()
        val FineUp  = calibration.addDialogButton("Fine Up"){
            pControl.setLocked(true)
        }
        val FineDown  = calibration.addDialogButton("Fine Down"){
            pControl.setLocked(false)
        }
        val GrossUp  = calibration.addDialogButton("Gross Up"){
            pControl.setGrossUp(true)
        }
        val GrossDown  = calibration.addDialogButton("Gross Down"){
            pControl.setGrossUp(false)
        }


        val LightOn  = calibration.addDialogButton("Lamp On"){
            pControl.setLightOn(true)
        }

        val LightOff  = calibration.addDialogButton("Lamp Off"){
            pControl.setLightOn(false)
        }


        continControl(leftContinFast,"X", -1,fast)
        continControl(leftContinMiddle,"X", -1,middle)
        continControl(leftContinSlow,"X", -1,slow)
        continControl(rightContinFast,"X", 1,fast)
        continControl(rightContinMiddle,"X", 1,middle)
        continControl(rightContinSlow,"X", 1,slow)
        continControl(forwardContinFast,"Y", -1,fast)
        continControl(forwardContinMiddle,"Y", -1,middle)
        continControl(forwardContinSlow,"Y", -1,slow)
        continControl(backwardContinFast,"Y", 1,fast)
        continControl(backContinMiddle,"Y", 1,middle)
        continControl(backContinSlow,"Y", 1,slow)



        calibration.addToolbarButton("Calibrate as postion 1 (x,y,z), top left") {
            position1X = pControl.xPosition
            position1Y = pControl.yPosition
            measureHeightZ = pControl.zPosition

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
            }
            else pControl.continMovement(axis, 0.0)
        }
    }

}