package org.oefet.fetch.gui.inputs

import jisa.devices.TC
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class TemperatureChange : Grid("Temperature Change", 1), ActionInput {

    val basic  = Fields("Temperature Set-Points")
    val config = Configurator<TC>("Temperature Controller", TC::class.java)

    val temp = basic.addDoubleField("Temperature [K]", 300.0)

    init { basic.addSeparator() }

    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    val subQueue = ActionQueue()

    init {

        setGrowth(true, false)
        addAll(basic, config)
        setIcon(Icon.SNOWFLAKE)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        config.linkToConfig(Settings.tempSingleConfig)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.tempSingleBasic)
            config.writeToConfig(Settings.tempSingleConfig)

            val temperature = temp.value

            queue.addAction("Change Temperature to $temperature K") {

                val tc = config.configuration.get() ?: throw Exception("No temperature controller configured")

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