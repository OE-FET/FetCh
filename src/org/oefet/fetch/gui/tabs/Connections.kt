package org.oefet.fetch.gui.tabs

import jisa.devices.EMController
import jisa.devices.LevelMeter
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.enums.Icon
import jisa.gui.ConnectorGrid
import org.oefet.fetch.Settings

object Connections : ConnectorGrid("Connections", Settings.instruments) {

    val smu1    = addSMU("SMU 1")
    val smu2    = addSMU("SMU 2")
    val vMeter1 = addInstrument("Voltmeter 1", VMeter::class.java)
    val vMeter2 = addInstrument("Voltmeter 2", VMeter::class.java)
    val tCon    = addTC("Temperature Controller")
    val tSen    = addInstrument("Thermometer", TMeter::class.java)
    val lockIn  = addDPLockIn("Lock-In Amplifier")
    val dcPower = addDCPower("Motor Power Supply")
    val preAmp  = addVPreAmp("Voltage Pre-Amplifier")
    val magnet  = addInstrument("Electromagnet Controller", EMController::class.java)
    val hMeter  = addInstrument("Helium Level Meter", LevelMeter::class.java)

    init {
        numColumns = 2
        setIcon(Icon.CONNECTION)
        connectAll()
    }

}