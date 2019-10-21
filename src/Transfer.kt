import jisa.enums.Icon
import jisa.gui.Fields
import jisa.gui.Grid

class Transfer(mainWindow: MainWindow) : Grid("Transfer Curve", 1) {

    val basic = Fields("Basic Parameters")
    val sourceDrain = Fields("Source-Drain Parameters")
    val sourceGate = Fields("Source-Gate Parameters")

    val enabled = basic.addCheckBox("Enabled", false)
    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val minSDV  = sourceDrain.addDoubleField("Start [V]", 0.0)
    val maxSDV  = sourceDrain.addDoubleField("Stop [V]", 60.0)
    val numSDV  = sourceDrain.addIntegerField("No. Steps", 61)

    init { sourceDrain.addSeparator() }

    val symSDV  = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val minSGV  = sourceGate.addDoubleField("Start [V]", 0.0)
    val maxSGV  = sourceGate.addDoubleField("Stop [V]", 60.0)
    val numSGV  = sourceGate.addIntegerField("No. Steps", 7)

    init { sourceGate.addSeparator() }

    val symSGV  = sourceGate.addCheckBox("Sweep Both Ways", true)


    init {

        addAll(basic, Grid(2, sourceDrain, sourceGate))
        setGrowth(true, false)
        setIcon(Icon.RHEOSTAT)

        basic.loadFromConfig("transfer-basic", mainWindow.config)
        sourceDrain.loadFromConfig("transfer-sd", mainWindow.config)
        sourceGate.loadFromConfig("transfer-sg", mainWindow.config)

        enabled.setOnChange(this::updateEnabled)
        updateEnabled()

    }

    fun updateEnabled() {

        val disabled = !enabled.get()

        intTime.setDisabled(disabled)
        delTime.setDisabled(disabled)

        sourceDrain.setFieldsDisabled(disabled)
        sourceGate.setFieldsDisabled(disabled)

    }

}