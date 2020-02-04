package fetter.gui

import jisa.gui.Tabs

object MainWindow : Tabs("FetCh - FET Characterisation Suite") {

    init {
        addAll(Welcome, Connections, Configuration, Temperature, Output, Transfer, Measure)
        setMaximised(true)
        setExitOnClose(true)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

}