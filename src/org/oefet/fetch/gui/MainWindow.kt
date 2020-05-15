package org.oefet.fetch.gui

import jisa.gui.Pages
import jisa.gui.Tabs
import org.oefet.fetch.gui.tabs.*

object MainWindow : Pages("FetCh - FET Characterisation Suite") {


    init {

        addSeparator("Measure")

        addAll(Measure)

        addSeparator("Analyse")

        addAll(FileLoad, Analysis)

        addSeparator("Configure")

        addAll(Connections, Configuration)

        setMaximised(true)
        setExitOnClose(true)
        setIcon(MainWindow::class.java.getResource("fEt.png"))
    }

}