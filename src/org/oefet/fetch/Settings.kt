package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val instruments       = subBlock("instruments")
    val measureBasic      = subBlock("measureBasic")
    val repeatBasic       = subBlock("repeatBasic")
    val timeBasic         = subBlock("timeBasic")
    val inputs            = subBlock("inputs")
    val holdBasic         = subBlock("holdBasic")
    val holdSD            = subBlock("holdSD")
    val holdSG            = subBlock("holdSG")
    val stressBasic       = subBlock("stressBasic")
    val stressInterval    = subBlock("stressInterval")
    val tempBasic         = subBlock("tempBasic")
    val tempSingleBasic   = subBlock("tempSingleBasic")
    val groundConfig      = subBlock("groundConfig")
    val sourceDrainConfig = subBlock("sourceDrainConfig")
    val sourceGateConfig  = subBlock("sourceGateConfig")
    val fourPP1Config     = subBlock("fourPP1Config")
    val fourPP2Config     = subBlock("fourPP2Config")
    val tControlConfig    = subBlock("tControlConfig")
    val tMeterConfig      = subBlock("tMeterConfig")

}