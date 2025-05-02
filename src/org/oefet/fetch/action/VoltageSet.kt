package org.oefet.fetch.action

import javafx.scene.image.Image
import jisa.devices.source.VSource
import jisa.gui.GUI
import jisa.results.ResultTable

class VoltageSet : FetChAction("Set Voltage", Image(GUI::class.java.getResource("images/smu.png").toString())) {

    private val voltage by userInput("Voltage [V]", 10.0)
    private val off     by userInput("Turn Off?", false)
    private val vSource by requiredInstrument("Voltage Source", VSource::class)

    override fun run(results: ResultTable?) {

        if (off) {
            vSource.turnOff()
        } else {
            vSource.turnOn()
            vSource.voltage = voltage
        }

    }

    override fun getLabel(): String {
        return "$voltage V"
    }


}