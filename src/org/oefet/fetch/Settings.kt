package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val holdSDConf        = subBlock("holdSDConf")
    val holdSGConf        = subBlock("holdSGConf")
    val connections       = subBlock("connections")
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

}