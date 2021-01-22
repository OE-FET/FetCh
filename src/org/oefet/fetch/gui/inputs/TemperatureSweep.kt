package org.oefet.fetch.gui.inputs

import jisa.devices.interfaces.TC
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.gui.Tabs
import jisa.maths.Range
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class TemperatureSweep : Tabs("Temperature Sweep"), SweepInput {

    val params      = Grid("Parameters", 2)
    val instruments = Grid("Instruments", 2)
    val basic       = Fields("Temperature Set-Points")
    val config      = Configurator<TC>("Temperature Controller", TC::class.java)
    val name        = basic.addTextField("Variable Name", "T").apply { isDisabled = true }

    init { basic.addSeparator() }

    val minT = basic.addDoubleField("Start [K]", 300.0)
    val maxT = basic.addDoubleField("Stop [K]", 50.0)
    val numT = basic.addIntegerField("No. Steps", 6)

    init { basic.addSeparator() }

    val stabPerc  = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime  = basic.addDoubleField("Stability Time [s]", 600.0)

    val subQueue  = ActionQueue()
    val names     = ArrayList<String>()

    init {

        instruments.addAll(config)
        params.setGrowth(true, false)
        instruments.setGrowth(true, false)
        addAll(params, instruments)
        setIcon(Icon.SNOWFLAKE)
        setIcon(Images.getURL("fEt.png"))

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
    }

    override fun ask(queue: ActionQueue) {

        params.clear()
        params.addAll(basic, FetChQueue("Interval Actions", subQueue))

        basic.loadFromConfig(Settings.tempBasic)
        config.loadFromConfig(Settings.tempConfig)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.tempBasic)
            config.writeToConfig(Settings.tempConfig)

            val name = name.get()

            for (T in Range.linear(minT.get(), maxT.get(), numT.get())) {

                queue.addAction("Change Temperature to $T K") {

                    val tc = config.configuration.get() ?: throw Exception("No temperature controller configured")

                    tc.targetTemperature = T
                    tc.useAutoHeater()
                    tc.waitForStableTemperature(T, stabilityPercentage, stabilityTime)

                }

                for (action in subQueue) {

                    val copy = action.copy()
                    copy.setVariable(name, "$T K")
                    if (copy is ActionQueue.MeasureAction) copy.setAttribute(name, "$T K")

                    queue.addAction(copy)

                }

            }

        }

    }

    override fun showAsConfirmation(): Boolean {
        subQueue.clear()
        names.clear()
        return super.showAsConfirmation()
    }

    val values: Range<Double>
        get() = Range.linear(minT.get(), maxT.get(), numT.get())

    val stabilityPercentage: Double
        get() = stabPerc.get()

    val stabilityTime: Long
        get() = (stabTime.get() * 1000.0).toLong()

}