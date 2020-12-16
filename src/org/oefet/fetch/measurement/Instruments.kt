package org.oefet.fetch.measurement

import jisa.devices.*
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.Connections

object Instruments {

    // These should be reconfigured everytime they are called
    val gdSMU: SMU?      get() = Configuration.ground.get()
    val sdSMU: SMU?      get() = Configuration.sourceDrain.get()
    val sgSMU: SMU?      get() = Configuration.sourceGate.get()
    val htSMU: SMU?      get() = Configuration.heater.get()
    val tvMeter: VMeter? get() = Configuration.thermal.get()
    val fpp1: VMeter?    get() = Configuration.fourPP1.get()
    val fpp2: VMeter?    get() = Configuration.fourPP2.get()
    val magnet: EMController? get() = Connections.magnet.get();

    // These should be cached for an entire run
    var tControl: TC?     = null
    var tMeter: TMeter?   = null
    var lockIn: DPLockIn? = null
    var dcPower: DCPower? = null
    var preAmp: VPreAmp?  = null

    fun loadInstruments() {

        tControl = Configuration.tControl.get()
        tMeter   = Configuration.tMeter.get()
        lockIn   = Connections.lockIn.get()
        dcPower  = Connections.dcPower.get()
        preAmp   = Connections.preAmp.get()

    }

}