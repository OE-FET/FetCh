package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val connections         = subBlock("connections")
    val measureBasic        = subBlock("measureBasic")
    val inputs              = subBlock("inputs")
    val positionSweepCounts = subBlock("positionSweepCounts")
    val dashboard           = subBlock("dashboard")

}