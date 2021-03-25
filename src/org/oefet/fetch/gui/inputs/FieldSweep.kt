package org.oefet.fetch.gui.inputs

import jisa.devices.interfaces.EMController
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.*
import jisa.gui.ActionQueueDisplay.ActionRunnable
import jisa.maths.Range
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.gui.images.Images
import java.lang.Exception

class FieldSweep : Tabs("Field Sweep"), SweepInput {

    val basic       = Fields("Field Set-Point")
    val config      = Configurator<EMController>("Field Controller", EMController::class.java)
    val parameters  = Grid("Parameters", 2, basic)
    val instruments = Grid("Instruments", 1, config)
    val name        = basic.addTextField("Variable Name", "B").apply { isDisabled = true }
    val rangeB      = basic.addDoubleRange("Field [T]", Range.linear(0.0, 1.0, 11), 0.0, 1.0, 11, 0.1, 2)
    val subQueue    = ActionQueue()

    init {

        parameters.setGrowth(true, false)
        instruments.setGrowth(true, false)

        addAll(parameters, instruments)
        setIcon(Images.getURL("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        parameters.clear()
        parameters.addAll(basic, FetChQueue("Interval Actions", subQueue))

        config.loadFromConfig(Settings.fieldSweepConfig)
        basic.loadFromConfig(Settings.fieldSweepBasic)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.fieldSweepBasic)
            config.writeToConfig(Settings.fieldSweepConfig)

            val name  = name.value
            val range = rangeB.value

            for (B in range) {

                queue.addAction("Change Field to $B T") {
                    val emc = config.configuration.instrument ?: throw Exception("No EM Controller Configured")
                    emc.field = B
                }

                queue.addAlteredQueue(subQueue) { it.setAttribute(name, "$B T") }

            }

        }

    }


}