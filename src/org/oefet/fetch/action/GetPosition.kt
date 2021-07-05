package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class GetPosition : FetChAction("Get Position") {

    val pControl      by requiredConfig("Position Controller", ProbeStation::class)


    override fun run(results: ResultTable) {
        println("${pControl.xPosition}, ${pControl.yPosition}, ${pControl.zPosition}")
    }

    override fun onFinish() {
        
    }

    override fun getLabel(): String {
        return "${pControl.xPosition}, ${pControl.yPosition}, ${pControl.zPosition}"
    }

}