package fetter.gui

import jisa.control.ConfigStore
import jisa.gui.Tabs

class MainWindow : Tabs("FETTER - FET Characterisation Suite") {

    val config        = ConfigStore("FETTER")
    val connections   = Connections(this)
    val configuration = Configuration(this)
    val temperature   = Temperature(this)
    val output        = Output(this)
    val transfer      = Transfer(this)
    val measure       = Measure(this)

    init {
        addAll(connections, configuration, temperature, output, transfer, measure)
        setMaximised(true)
        setExitOnClose(true)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

}