package fetter.gui

import jisa.enums.Icon
import jisa.gui.Configurator
import jisa.gui.Grid

/**
 * Instrument configuration tab
 */
class Configuration(mainWindow : MainWindow) : Grid("Configuration", 2) {

    val sourceDrain = Configurator.SMU("Source-Drain Channel", "sdSMU", mainWindow.config, mainWindow.connections)
    val sourceGate  = Configurator.SMU("Source-Gate Channel", "sgSMU", mainWindow.config, mainWindow.connections)
    val fourPP1     = Configurator.VMeter("Four Point Probe Channel 1", "fpp1", mainWindow.config, mainWindow.connections)
    val fourPP2     = Configurator.VMeter("Four Point Probe Channel 2", "fpp2", mainWindow.config, mainWindow.connections)
    val tControl    = Configurator.TC("Temperature Control", "tc", mainWindow.config, mainWindow.connections)
    val tMeter      = Configurator.TMeter("Temperature Sensor", "tm", mainWindow.config, mainWindow.connections)

    init {

        setGrowth(true, false)
        setIcon(Icon.COGS)

        addAll(
            sourceDrain,
            sourceGate,
            fourPP1,
            fourPP2,
            tControl,
            tMeter
        )
    }

}