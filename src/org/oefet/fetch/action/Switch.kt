package org.oefet.fetch.action

import jisa.devices.interfaces.Switch
import jisa.enums.Icon
import jisa.gui.Doc
import jisa.gui.Element
import jisa.results.ResultTable

class Switch : FetChAction("Relay Switch", Icon.DEVICE.blackImage) {

    // Config (parameter and instrument)
    val on     by userInput ("Basic", "On", false)
    val switch by requiredInstrument("Switch", Switch::class)

    // GUI element to show when switching
    val element = Doc("Switch").apply { addText("Switching Relay...").setAlignment(Doc.Align.CENTRE) }

    override fun createDisplay(data: ResultTable): Element = element

    override fun run(results: ResultTable) {
        switch.isOn = on
    }

    override fun onFinish() { /* Nothing to do */ }

    override fun getLabel(): String {
        return if (on) "On" else "Off"
    }

}