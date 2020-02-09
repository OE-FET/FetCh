package org.oefet.fetch.gui

import jisa.gui.Tabs

object MainWindow : Tabs("FetCh - FET Characterisation Suite") {

    init {
        addAll(Measure, Results, Connections, Configuration)
        setMaximised(true)
        setExitOnClose(true)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

}