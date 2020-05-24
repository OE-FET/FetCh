package org.oefet.fetch.gui.inputs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.maths.Range
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.Measure

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
        setIcon(MainWindow::class.java.getResource("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        if (showAsConfirmation()) {

            basic.writeToConfig()

            val T = temp.get()

            queue.addAction("Change Temperature to $T K") {

                val tc = Configuration.tControl.get() ?: throw Exception("No temperature controller configured")

                tc.targetTemperature = T
                tc.useAutoHeater()
                tc.waitForStableTemperature(T, stabilityPercentage, stabilityTime)

            }

        }


    }

    val stabilityPercentage: Double
        get() = stabPerc.get()

    val stabilityTime: Long
        get() = (stabTime.get() * 1000.0).toLong()

}