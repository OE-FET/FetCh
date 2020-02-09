package org.oefet.fetch.gui

import jisa.gui.Doc

object TempChangeNotice : Doc("Temperature Stabilising") {

    init {

        addHeading("Temperature Change in Progress")
            .setAlignment(Doc.Align.CENTRE)
        addText("Waiting for the temperature controller to report a stable temperature within range of the current set-point before continuing...")
            .setAlignment(Doc.Align.CENTRE)

    }

}