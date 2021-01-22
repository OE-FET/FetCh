package org.oefet.fetch.gui.inputs

import jisa.devices.interfaces.TC
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.gui.Tabs
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class TemperatureChange : Tabs("Temperature Change"), ActionInput {

    val basic       = Fields("Temperature Set-Points")
    val config      = Configurator<TC>("Temperature Controller", TC::class.java)
    val parameters  = Grid("Parameters", 1, basic)
    val instruments = Grid("Instruments", 1, config)

    val temp = basic.addDoubleField("Temperature [K]", 300.0)

    init { basic.addSeparator() }

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

        config.loadFromConfig(Settings.tempSingleConfig)
        basic.loadFromConfig(Settings.tempSingleBasic)

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