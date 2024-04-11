package org.oefet.fetch.sweep

import javafx.scene.image.Image
import jisa.devices.interfaces.ISource
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.GUI
import jisa.maths.Range

class CurrentSweep : FetChSweep<Double>("Current Sweep", "I", Image(GUI::class.java.getResource("images/smu.png").toString())) {

    private val currents by userInput("Currents", "Current [A]", Range.linear(0, 10e-6, 11))
    private val off      by userInput("Currents", "Turn Off Afterwards?", true)
    private val iSource  by requiredInstrument("Current Source", ISource::class)

    override fun getValues(): List<Double> {

        if (off) {
            val list = currents.list().toMutableList()
            list += Double.NaN
            return list
        } else {
            return currents.list()
        }

    }

    override fun generateForValue(value: Double, actions: List<Action<*>>): List<Action<*>> {

        if (value.isNaN()) {

            val turnOff = SimpleAction("Switch Off") {
                iSource.turnOff()
            }

            return listOf(turnOff)

        } else {

            val changeI = SimpleAction("Change Current ($value A)") {
                iSource.current = value
                iSource.turnOn()
            }

            val generated = ArrayList<Action<*>>()

            generated += changeI
            generated += actions

            return generated;

        }

    }

    override fun formatValue(value: Double): String {
        return if (value.isNaN()) "Turn Off" else "$value A"
    }

}