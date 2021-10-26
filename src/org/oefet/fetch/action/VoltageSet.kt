package org.oefet.fetch.action

import jisa.Util
import jisa.devices.interfaces.VSource
import jisa.results.ResultTable

class VoltageSet : FetChAction("Set Voltage") {

    private val voltage by userInput("Voltage [V]", 10.0)
    private val vSource by requiredInstrument("Voltage Source", VSource::class)

    override fun run(results: ResultTable?) {

        vSource.turnOn()
        vSource.voltage = voltage

        if (voltage == 0.0) vSource.turnOff()

    }

    override fun getLabel(): String {
        return "$voltage V"
    }


}