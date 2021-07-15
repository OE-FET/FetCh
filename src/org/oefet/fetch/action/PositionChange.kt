package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation

import jisa.results.ResultTable
import jisa.results.DoubleColumn
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class PositionChange : FetChAction("Change Position") {

    val pControl      by requiredConfig("Position Controller", ProbeStation::class)
    val xposition     by input("Position", "x Position [m]", 1e-3)
    val yposition     by input("Position", "y Position [m]", 1e-3)
    val zposition     by input("Position", "z Position [m]", 1e-3)


    override fun run(results: ResultTable) {

        pControl.isLocked  = false
        pControl.zPosition = 0.0
        pControl.setXYPosition(xposition, yposition)
        pControl.zPosition = zposition

    }

    override fun onFinish() {
        
    }

    override fun getLabel(): String {
        return "$xposition, $yposition, $zposition"
    }

}