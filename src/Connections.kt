import jisa.control.ConfigStore
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.enums.Icon
import jisa.gui.Connector
import jisa.gui.ConnectorGrid

class Connections(mainWindow: MainWindow) : ConnectorGrid("Connections", mainWindow.config) {

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