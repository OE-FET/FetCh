import jisa.enums.Icon
import jisa.gui.Configurator
import jisa.gui.Grid

class Configuration(mainWindow : MainWindow) : Grid("Configuration", 2) {

    public val sourceDrain = Configurator.SMU("Source-Drain Channel", "sdSMU", mainWindow.config, mainWindow.connections)
    public val sourceGate  = Configurator.SMU("Source-Gate Channel", "sgSMU", mainWindow.config, mainWindow.connections)
    public val fourPP1  = Configurator.SMU("Four Point Probe Channel 1", "fpp1", mainWindow.config, mainWindow.connections)
    public val fourPP2  = Configurator.SMU("Four Point Probe Channel 2", "fpp2", mainWindow.config, mainWindow.connections)
    public val tControl  = Configurator.TC("Temperature Control", "tc", mainWindow.config, mainWindow.connections)
    public val tMeter  = Configurator.TMeter("Temperature Sensor", "tm", mainWindow.config, mainWindow.connections)

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