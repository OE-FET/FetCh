package org.oefet.fetch.gui

import jisa.gui.Doc

object Splash : Doc("FetCh") {

    init {

        setIcon(Splash::class.java.getResource("images/fEt.png"))

        addImage(Splash::class.java.getResource("images/fEt.png"))
            .setAlignment(Align.CENTRE)
        addHeading("FetCh: FET Characterisation")
            .setAlignment(Align.CENTRE)
        addText("William Wood 2020")
            .setAlignment(Align.CENTRE)
        addText("Loading, please wait...")
            .setAlignment(Align.CENTRE)
        addText(" ".repeat(120))

    }

}