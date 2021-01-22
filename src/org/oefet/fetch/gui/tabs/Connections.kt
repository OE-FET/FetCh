package org.oefet.fetch.gui.tabs

import jisa.devices.interfaces.EMController
import jisa.devices.interfaces.LevelMeter
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.Icon
import jisa.gui.ConnectorGrid
import org.oefet.fetch.Settings

object Connections : ConnectorGrid("Connections", 3) {


    init {
        numColumns = 3
        setIcon(Icon.CONNECTION)
        linkToConfig(Settings.connections)
    }

}