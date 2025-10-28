package org.oefet.fetch.action

import jisa.enums.Icon
import jisa.gui.Doc
import jisa.gui.Element
import jisa.gui.GUI
import jisa.results.ResultTable

class UserWait : FetChAction("Wait for User", Icon.CLOCK.blackImage) {

    val message by userInput("Basic", "Message", "Press OK to continue...")

    override fun run(results: ResultTable?) {
        GUI.infoAlert(message)
    }

    override fun getLabel(): String {
        return message
    }

    override fun createDisplay(data: ResultTable): Element {
        val doc = Doc("Waiting...")
        doc.addText("Waiting for user...")
        return doc
    }

}