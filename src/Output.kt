import jisa.enums.Icon
import jisa.gui.Fields
import jisa.gui.Grid

class Output(mainWindow: MainWindow) : Grid("Output Curve", 1) {

    val basic   = Fields("Basic Parameters")
    val enabled = basic.addCheckBox("Enabled", false)
    val bSep    = basic.addSeparator()
    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDV      = sourceDrain.addDoubleField("Start [V]", 0.0)
    val maxSDV      = sourceDrain.addDoubleField("Stop [V]", 60.0)
    val numSDV      = sourceDrain.addIntegerField("No. Steps", 7)
    val sdSep       = sourceDrain.addSeparator()
    val symSDV      = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val sourceGate  = Fields("Source-Gate Parameters")
    val minSGV      = sourceGate.addDoubleField("Start [V]", 0.0)
    val maxSGV      = sourceGate.addDoubleField("Stop [V]", 60.0)
    val numSGV      = sourceGate.addIntegerField("No. Steps", 61)
    val sgSep       = sourceGate.addSeparator()
    val symSGV      = sourceGate.addCheckBox("Sweep Both Ways", true)

    init {

        addAll(basic, Grid(2, sourceDrain, sourceGate))
        setGrowth(true, false)
        setIcon(Icon.TRANSISTOR)

        basic.loadFromConfig("output-basic", mainWindow.config)
        sourceDrain.loadFromConfig("output-sd", mainWindow.config)
        sourceGate.loadFromConfig("output-sg", mainWindow.config)

        enabled.setOnChange(this::updateEnabled)
        updateEnabled()

    }

    fun updateEnabled() {

        val disabled = !enabled.get()

        intTime.isDisabled = disabled
        delTime.isDisabled = disabled

        sourceDrain.setFieldsDisabled(disabled)
        sourceGate.setFieldsDisabled(disabled)

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        sourceDrain.setFieldsDisabled(flag)
        sourceGate.setFieldsDisabled(flag)
        if (!flag) updateEnabled()
    }

}