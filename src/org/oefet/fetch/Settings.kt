package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val holdSDConf        = subBlock("holdSDConf")
    val holdSGConf        = subBlock("holdSGConf")
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
    val tempConfig        = subBlock("tempConfig")
    val tempSingleBasic   = subBlock("tempSingleBasic")
    val tempSingleConfig  = subBlock("tempSingleConfig")
    val groundConfig      = subBlock("groundConfig")
    val sourceDrainConfig = subBlock("sourceDrainConfig")
    val heaterConfig      = subBlock("heaterConfig")
    val sourceGateConfig  = subBlock("sourceGateConfig")
    val tvMeterConfig     = subBlock("tvMeterConfig")
    val fourPP1Config     = subBlock("fourPP1Config")
    val fourPP2Config     = subBlock("fourPP2Config")
    val tControlConfig    = subBlock("tControlConfig")
    val tMeterConfig      = subBlock("tMeterConfig")

}