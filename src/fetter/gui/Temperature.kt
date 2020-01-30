package fetter.gui

import jisa.enums.Icon
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.maths.Range

object Temperature : Grid("Temperature", 1) {

    val basic    = Fields("Temperature Set-Points")
    val enabled  = basic.addCheckBox("Enabled", false)

    init { basic.addSeparator() }

    val minT     = basic.addDoubleField("Start [K]", 300.0)
    val maxT     = basic.addDoubleField("Stop [K]", 50.0)
    val numT     = basic.addIntegerField("No. Steps", 6)

    init { basic.addSeparator() }

    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    init {

        setGrowth(true, false)
        add(basic)
        setIcon(Icon.SNOWFLAKE)
        basic.loadFromConfig("temp-basic", Settings)
        enabled.setOnChange(this::updateEnabled)
        updateEnabled()

    }

    fun updateEnabled() {

        basic.setFieldsDisabled(!enabled.get())
        enabled.isDisabled = false

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        if (!flag) updateEnabled()
    }

    val isEnabled: Boolean
        get() = enabled.get()


    val values: Range<Double>
        get() = Range.linear(minT.get(), maxT.get(), numT.get())

    val stabilityPercentage : Double
        get() = stabPerc.get()

    val stabilityTime : Long
        get() = (stabTime.get() * 1000.0).toLong()

}