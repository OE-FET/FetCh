package org.oefet.fetch.gui

import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.enums.Icon
import jisa.gui.ConnectorGrid

object Connections : ConnectorGrid("Connections", Settings.instruments) {

    val smu1    = addSMU("SMU 1")
    val smu2    = addSMU("SMU 2")
    val vMeter1 = addInstrument("Voltmeter 1", VMeter::class.java)
    val vMeter2 = addInstrument("Voltmeter 2", VMeter::class.java)
    val tCon    = addTC("Temperature Controller")
    val tSen    = addInstrument("Thermometer", TMeter::class.java)

    init {
        setNumColumns(2)
        setIcon(Icon.CONNECTION)
        connectAll()
    }

}