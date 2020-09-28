package org.oefet.fetch.measurement

import jisa.devices.*
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.Connections

object Instruments {

    val gdSMU: SMU?          get() = Configuration.ground.get()
    val sdSMU: SMU?          get() = Configuration.sourceDrain.get()
    val sgSMU: SMU?          get() = Configuration.sourceGate.get()
    val htSMU: SMU?          get() = Configuration.heater.get()
    val thermalVolt: VMeter? get() = Configuration.thermal.get()
    val fpp1: VMeter?        get() = Configuration.fourPP1.get()
    val fpp2: VMeter?        get() = Configuration.fourPP2.get()
    val tControl: TC?        get() = Configuration.tControl.get()
    val tMeter: TMeter?      get() = Configuration.tMeter.get()
    val lockIn: DPLockIn?    get() = Connections.lockIn.get()
    val dcPower: DCPower?    get() = Connections.dcPower.get()
    val preAmp: VPreAmp?     get() = Connections.preAmp.get()

}