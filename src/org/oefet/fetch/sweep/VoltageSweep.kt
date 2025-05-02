package org.oefet.fetch.sweep

import javafx.scene.image.Image
import jisa.devices.source.VSource
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.GUI
import jisa.maths.Range

class VoltageSweep : FetChSweep<Double>("Voltage Sweep", "V", Image(GUI::class.java.getResource("images/smu.png").toString())) {

    private val voltages by userInput("Voltages", "Voltage [V]", Range.linear(0, 60))
    private val off      by userInput("Voltages", "Turn Off Afterwards?", true)
    private val vSource  by requiredInstrument("Voltage Source", VSource::class)

    override fun getValues(): List<Double> {

        if (off) {
            val list = voltages.list().toMutableList()
            list += Double.NaN
            return list
        } else {
            return voltages.list()
        }

    }

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        if (value.isNaN()) {

            val turnOff = SimpleAction("Switch Off") {
                vSource.turnOff()
            }

            return listOf(turnOff)

        } else {

            val changeV = SimpleAction("Change Voltage ($value V)") {
                vSource.voltage = value
                vSource.turnOn()
            }

            val generated = ArrayList<Action<*>>()

            generated += changeV
            generated += actions

            return generated

        }

    }

    override fun formatValue(value: Double): String {
        return if (value.isNaN()) "Turn Off" else "$value V"
    }

}