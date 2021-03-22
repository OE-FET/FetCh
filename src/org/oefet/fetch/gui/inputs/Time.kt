package org.oefet.fetch.gui.inputs

import jisa.Util
import jisa.control.SRunnable
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class Time : Grid("Wait", 1), ActionInput {

    val fields  = Fields("Time")
    val hours   = fields.addIntegerField("Hours", 0)
    val minutes = fields.addIntegerField("Minutes", 0)
    val seconds = fields.addIntegerField("Seconds", 0)
    val millis  = fields.addIntegerField("Milliseconds", 0)

    var waitTime: Long = 0
    var action: ActionQueue.Action? = null

    init {
        add(fields)
        setIcon(Icon.DEVICE)
        setIcon(Images.getURL("fEt.png"))
    }

    override fun edit() {

        showAsAlert()

        fields.writeToConfig(Settings.timeBasic)

        waitTime = (millis.value + (1000 * seconds.value) + (1000 * 60 * minutes.value) + (1000 * 60 * 60 * hours.value)).toLong()
        val text = Util.msToString(waitTime)

        action?.name = "Wait for $text"


    }

    override fun ask(queue: ActionQueue) {

        fields.loadFromConfig(Settings.timeBasic)

        if (showAsConfirmation()) {

            fields.writeToConfig(Settings.timeBasic)

            waitTime = (millis.value + (1000 * seconds.value) + (1000 * 60 * minutes.value) + (1000 * 60 * 60 * hours.value)).toLong()
            val text = Util.msToString(waitTime)

            action = queue.addAction(InputAction("Wait for $text", this, SRunnable {
                Thread.sleep(waitTime)
            }))

        }

    }

}