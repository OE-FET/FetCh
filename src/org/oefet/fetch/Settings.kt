package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val instruments       = subBlock("instruments")
    val measureBasic      = subBlock("measureBasic")
    val repeatBasic       = subBlock("repeatBasic")
    val outputBasic       = subBlock("outputBasic")
    val outputSD          = subBlock("outputSD")
    val outputSG          = subBlock("outputSG")
    val transferBasic     = subBlock("transferBasic")
    val transferSD        = subBlock("transferSD")
    val transferSG        = subBlock("transferSG")
    val holdBasic         = subBlock("holdBasic")
    val holdSD            = subBlock("holdSD")
    val holdSG            = subBlock("holdSG")
    val tempBasic         = subBlock("tempBasic")
    val groundConfig      = subBlock("groundConfig")
    val sourceDrainConfig = subBlock("sourceDrainConfig")
    val sourceGateConfig  = subBlock("sourceGateConfig")
    val fourPP1Config     = subBlock("fourPP1Config")
    val fourPP2Config     = subBlock("fourPP2Config")
    val tControlConfig    = subBlock("tControlConfig")
    val tMeterConfig      = subBlock("tMeterConfig")

}