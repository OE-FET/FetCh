package org.oefet.fetch.gui.tabs

import jisa.devices.EMController
import jisa.devices.LevelMeter
import jisa.devices.TMeter
import jisa.devices.VMeter
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