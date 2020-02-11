package org.oefet.fetch.gui

import jisa.gui.Tabs
import org.oefet.fetch.gui.tabs.*

object MainWindow : Tabs("FetCh - FET Characterisation Suite") {

    init {
        addAll(
            Measure,
            Results,
            Analysis,
            Connections,
            Configuration
        )
        setMaximised(true)
        setExitOnClose(true)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

}