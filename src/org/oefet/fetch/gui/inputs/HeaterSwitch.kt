package org.oefet.fetch.gui.inputs

import jisa.control.SRunnable
import jisa.devices.interfaces.EMController
import jisa.devices.power.IPS120
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images
import java.lang.Exception

class HeaterSwitch : Grid("Heater Switch", 1), ActionInput {

    val basic       = Fields("Heater Switch")
    val config      = Configurator<EMController>("Field Controller", EMController::class.java)
    val parameters  = Grid("Parameters", 1, basic)
    val instruments = Grid("Instruments", 1, config)

    val switch      = basic.addCheckBox("Heater On?",false)
    var on          = false

    var action: ActionQueue.Action? = null

    init {

        parameters.setGrowth(true, false)
        instruments.setGrowth(true, false)

        addAll(parameters, instruments)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        config.loadFromConfig(Settings.fieldSingleConfig)

        if (showAsConfirmation()) {

            config.writeToConfig(Settings.fieldSingleConfig)
            on  = switch.value

            action = queue.addAction(InputAction("Turn Heater ${if (on) "On" else "Off"}", this, SRunnable {

                val emc = config.configuration.get() ?: throw Exception("No IPS120 Configured")

                if (emc is IPS120) {
                    emc.setHeater(on)
                } else {
                    throw Exception("Configured EM Controller is not an IPS120")
                }

            }))

        }
    }

    override fun edit() {

        showAsAlert()

        config.writeToConfig(Settings.fieldSingleConfig)
        on  = switch.value
        action?.name = "Turn Heater ${if (on) "On" else "Off"}"


    }

}