import jisa.control.ConfigStore
import jisa.gui.Tabs

class MainWindow : Tabs("FETTER - FET Characterisation Suite") {

    val config        = ConfigStore("FETTER")
    val connections   = Connections(this)
    val configuration = Configuration(this)
    val output        = Output(this)
    val transfer      = Transfer(this)

    init {
        addAll(connections, configuration, output, transfer)
        setMaximised(true)
        setExitOnClose(true)
    }

}

fun main() {

    val mainWindow = MainWindow()
    mainWindow.show()

}