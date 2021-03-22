package org.oefet.fetch.gui.inputs

import jisa.control.SRunnable
import jisa.devices.interfaces.TC
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class TemperatureChange : Grid("Temperature Change", 1), ActionInput {

    val basic = Fields("Temperature Set-Points")
    val config = Configurator<TC>("Temperature Controller", TC::class.java)
    val parameters = Grid("Parameters", 1, basic)
    val instruments = Grid("Instruments", 1, config)

    val temp = basic.addDoubleField("Temperature [K]", 300.0)

    init {
        basic.addSeparator()
    }

    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    val subQueue = ActionQueue()

    init {

        parameters.setGrowth(true, false)
        instruments.setGrowth(true, false)

        addAll(parameters, instruments)
        setIcon(Icon.SNOWFLAKE)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        config.loadFromConfig(Settings.tempConfig)
        basic.loadFromConfig(Settings.tempSingleBasic)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.tempSingleBasic)
            config.writeToConfig(Settings.tempConfig)

            temperature         = temp.value
            stabilityPercentage = stabPerc.value
            stabilityTime       = (stabTime.value * 1000.0).toLong()

            action = queue.addAction(InputAction("Change Temperature to $temperature K", this, SRunnable {

                val tc = config.configuration.get() ?: throw Exception("No temperature controller configured")

                tc.targetTemperature = temperature
                tc.useAutoHeater()

                tc.waitForStableTemperature(temperature, stabilityPercentage, stabilityTime)

            }))

        }


    }

    override fun edit() {

        showAsAlert()

        basic.writeToConfig(Settings.tempSingleBasic)
        config.writeToConfig(Settings.tempConfig)

        temperature         = temp.value
        stabilityPercentage = stabPerc.value
        stabilityTime       = (stabTime.value * 1000.0).toLong()
        action?.name        = "Change Temperature to $temperature K"



    }

    var temperature = 0.0
    var stabilityPercentage = 0.0
    var stabilityTime = 600000.toLong()
    var action: ActionQueue.Action? = null

}