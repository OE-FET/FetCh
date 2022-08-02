package org.oefet.fetch.action

import jisa.devices.interfaces.ProbeStation
import jisa.results.ResultTable
import org.oefet.fetch.gui.images.Images

class PositionChange : FetChAction("Change Position", Images.getImage("calibration.png")) {

    val pControl      by requiredInstrument("Position Controller", ProbeStation::class)
    val xposition     by userInput("Position", "x Position [m]", 1e-3)
    val yposition     by userInput("Position", "y Position [m]", 1e-3)
    val zposition     by userInput("Position", "z Position [m]", 1e-3)

    val safetyMargin = 6e-3


    override fun run(results: ResultTable) {

        pControl.isLocked  = false
        pControl.zPosition = 0.0
        pControl.setXYPosition(xposition, yposition)
        if(zposition > safetyMargin) {
            throw Exception("Larger than safety margin")
        }
        else{
            pControl.zPosition = zposition
        }

    }

    override fun onFinish() {
        
    }

    override fun getLabel(): String {
        return "$xposition, $yposition, $zposition"
    }

}