package org.oefet.fetch.gui

import jisa.gui.Pages
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.gui.tabs.*

object MainWindow : Pages("FetCh - FET Characterisation Suite") {


    init {

        addSeparator("Measure")

        addAll(Measure, Dashboard)

        addSeparator("Analyse")

        addAll(FileLoad, Analysis)

        addSeparator("Configure")

        addAll(Connections)

        isMaximised = true
        setExitOnClose(true)
        setIcon(Images.getURL("fEt.png"))

    }

}