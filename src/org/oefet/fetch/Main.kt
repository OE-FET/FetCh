package org.oefet.fetch

import org.oefet.fetch.gui.MainWindow
import jisa.gui.Doc

class Main;

fun main() {

    val doc = Doc("FetCh")
    doc.addImage(Main::class.java.getResource("gui/fEt.png"))
        .setAlignment(Doc.Align.CENTRE)
    doc.addHeading("FetCh: FET Characterisation")
        .setAlignment(Doc.Align.CENTRE)
    doc.addText("William Wood 2020")
        .setAlignment(Doc.Align.CENTRE)
    doc.addText("Loading, please wait...")
        .setAlignment(Doc.Align.CENTRE)
    doc.addText("                                                                                                                     ")

    doc.show()
    MainWindow.show()
    doc.close()
}