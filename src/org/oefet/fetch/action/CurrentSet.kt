package org.oefet.fetch.action

import javafx.scene.image.Image
import jisa.devices.source.ISource
import jisa.gui.GUI
import jisa.results.ResultTable

class CurrentSet : FetChAction("Set Current", Image(GUI::class.java.getResource("images/smu.png").toString())) {

    private val current by userInput("Current [A]", 10e-6)
    private val off     by userInput("Turn Off?", false)
    private val iSource by requiredInstrument("Current Source", ISource::class)

    override fun run(results: ResultTable?) {

        if (off) {
            iSource.turnOff()
        } else {
            iSource.turnOn()
            iSource.current = current
        }

    }

    override fun getLabel(): String {
        return "$current A"
    }


}