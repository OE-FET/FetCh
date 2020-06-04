package org.oefet.fetch.gui.inputs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.gui.tabs.Configuration

class TemperatureChange : Grid("Temperature Change", 1), ActionInput {

    val basic = Fields("Temperature Set-Points")

    val temp = basic.addDoubleField("Temperature [K]", 300.0)

    init { basic.addSeparator() }

    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    val subQueue = ActionQueue()

    init {

        setGrowth(true, false)
        addAll(basic)
        setIcon(Icon.SNOWFLAKE)
        basic.linkConfig(Settings.tempSingleBasic)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        if (showAsConfirmation()) {

            basic.writeToConfig()

            val temperature = temp.value

            queue.addAction("Change Temperature to $temperature K") {

                val tc = Configuration.tControl.get() ?: throw Exception("No temperature controller configured")

                tc.targetTemperature = temperature
                tc.useAutoHeater()
                tc.waitForStableTemperature(temperature, stabilityPercentage, stabilityTime)

            }

        }


    }

    val stabilityPercentage: Double
        get() = stabPerc.value

    val stabilityTime: Long
        get() = (stabTime.value * 1000.0).toLong()

}