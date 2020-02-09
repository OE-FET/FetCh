package org.oefet.fetch.gui

import jisa.enums.Icon
import jisa.gui.Colour
import jisa.gui.Doc
import jisa.gui.Grid

object Welcome : Grid("Welcome", 1) {

    val doc = Doc("Welcome")

    init {

        setIcon(Icon.LIGHTBULB)

        add(doc)

        doc.addImage(Welcome::class.java.getResource("fEt.png"))
            .setAlignment(Doc.Align.CENTRE)
        doc.addHeading("FetCh: FET Characterisation")
            .setAlignment(Doc.Align.CENTRE)
        doc.addText("William Wood 2020")
            .setAlignment(Doc.Align.CENTRE)
        doc.addText("Welcome to the OE-FET transistor characterisation suite, FetCh.");

        doc.addSubHeading("Instrumentation Status")

        val sdStatus = doc.addValue("Source-Drain Channel", "Not Configured")
        val sgStatus = doc.addValue("Source-Gate Channel", "Not Configured")

    }

}