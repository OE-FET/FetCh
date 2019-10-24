import jisa.enums.Icon
import jisa.gui.Fields
import jisa.gui.Grid

class Temperature(mainWindow: MainWindow) : Grid("Temperature", 1) {

    val basic    = Fields("Temperature Set-Points")
    val enabled  = basic.addCheckBox("Enabled", false)
    val bSep1    = basic.addSeparator()
    val minT     = basic.addDoubleField("Start [K]", 300.0)
    val maxT     = basic.addDoubleField("Stop [K]", 50.0)
    val numT     = basic.addIntegerField("No. Steps", 6)
    val bsep2    = basic.addSeparator()
    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    init {

        setGrowth(true, false)
        add(basic)
        setIcon(Icon.SNOWFLAKE)
        basic.loadFromConfig("temp-basic", mainWindow.config)
        enabled.setOnChange(this::updateEnabled)
        updateEnabled()

    }

    fun updateEnabled() {

        basic.setFieldsDisabled(!enabled.get())
        enabled.setDisabled(false)

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        if (!flag) updateEnabled()
    }

}