package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val connections   = subBlock("connections")
    val measureBasic  = subBlock("measureBasic")
    val inputs        = subBlock("inputs")
    val dashboard     = subBlock("dashboard")
    val hidden        = subBlock("hidden")
    val actionDisplay = subBlock("actionDisplay")
    val logged        = subBlock("logged")

    val wide = actionDisplay.intValue("size").getOrDefault(0) == 0

}