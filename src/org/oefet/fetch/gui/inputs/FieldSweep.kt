package org.oefet.fetch.gui.inputs

import jisa.devices.interfaces.EMController
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.gui.Tabs
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
    val rangeB      = basic.addDoubleRange("Field [T]", 0.0, 1.0, 11)
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
                    val emc = config.configuration.get() ?: throw Exception("No EM Controller Configured")
                    emc.field = B
                }


                queue.addQueue(subQueue) {
                    it.setVariable(name, "$B T")
                    if (it is ActionQueue.MeasureAction) it.setAttribute(name, "$B T")
                    it
                }

            }

        }

    }


}