package org.oefet.fetch.gui.inputs

import jisa.devices.interfaces.EMController
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.gui.Tabs
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images
import java.lang.Exception

class FieldChange : Grid("Field Change", 1), ActionInput {

    val basic       = Fields("Field Set-Point")
    val config      = Configurator<EMController>("Field Controller", EMController::class.java)
    val parameters  = Grid("Parameters", 1, basic)
    val instruments = Grid("Instruments", 1, config)

    val field = basic.addDoubleField("Field [T]", 1.0)

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

            val bValue = field.value

            queue.addAction("Change Field to $bValue T") {

                val emc = config.configuration.get() ?: throw Exception("No EM Controller Configured")
                emc.field = bValue

            }

        }

    }


}