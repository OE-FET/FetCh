package org.oefet.fetch.gui.inputs

import jisa.control.SRunnable
import jisa.devices.interfaces.EMController
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class FieldChange : Grid("Field Change", 1), ActionInput {

    val basic = Fields("Field Set-Point")
    val config = Configurator<EMController>("Field Controller", EMController::class.java)
    val parameters = Grid("Parameters", 1, basic)
    val instruments = Grid("Instruments", 1, config)

    val field = basic.addDoubleField("Field [T]", 1.0)
    var bValue = 0.0
    var action: ActionQueue.Action? = null

    init {

        parameters.setGrowth(true, false)
        instruments.setGrowth(true, false)

        addAll(parameters, instruments)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        config.loadFromConfig(Settings.fieldSingleConfig)
        basic.loadFromConfig(Settings.fieldSingleBasic)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.fieldSingleBasic)
            config.writeToConfig(Settings.fieldSingleConfig)

            bValue = field.value

            action = queue.addAction(InputAction("Change Field to $bValue T", this, SRunnable {

                val emc = config.configuration.get() ?: throw Exception("No EM Controller Configured")
                emc.field = bValue

            }))

        }

    }

    override fun edit() {

        showAsAlert()

        basic.writeToConfig(Settings.fieldSingleBasic)
        config.writeToConfig(Settings.fieldSingleConfig)

        bValue = field.value
        action?.name = "Change Field to $bValue T"


    }

}