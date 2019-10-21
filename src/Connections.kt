import jisa.control.ConfigStore
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.enums.Icon
import jisa.gui.Connector
import jisa.gui.ConnectorGrid

class Connections(mainWindow: MainWindow) : ConnectorGrid("Connections", mainWindow.config) {

    public val smu1 = addSMU("SMU 1")
    public val smu2 = addSMU("SMU 2")
    public val smu3 = addSMU("SMU 3")
    public val smu4 = addSMU("SMU 4")
    public val tcon = addTC("Temperature Controller")
    public val tsen = addInstrument("Thermometer", TMeter::class.java)

    init {
        setNumColumns(3)
        setIcon(Icon.CONNECTION)
        connectAll()
    }

}