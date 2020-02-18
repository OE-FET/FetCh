package org.oefet.fetch.gui.inputs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.SyncMeasurement

object Sync : Grid("Synced Curve", 1) {

    val basic = Fields("Basic Parameters")
    val name = basic.addTextField("Measurement Name", "")

    init { basic.addSeparator() }

    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDV      = sourceDrain.addDoubleField("Start [V]", 0.0)
    val maxSDV      = sourceDrain.addDoubleField("Stop [V]", 50.0)
    val numSDV      = sourceDrain.addIntegerField("No. Steps", 51)

    init { sourceDrain.addSeparator() }

    val symSGV = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val sourceGate = Fields("Source-Gate Parameters")
    val offset     = sourceGate.addDoubleField("Offset [V]", 0.0)

    init {

        addAll(basic, Grid(2, sourceDrain, sourceGate))
        setGrowth(true, false)
        setIcon(Icon.WAVE)

        basic.linkConfig(Settings.syncBasic)
        sourceDrain.linkConfig(Settings.syncSD)
        sourceGate.linkConfig(Settings.syncSG)

    }

    fun updateEnabled() {

        val disabled = false

        intTime.isDisabled = disabled
        delTime.isDisabled = disabled

        sourceDrain.setFieldsDisabled(disabled)
        sourceGate.setFieldsDisabled(disabled)

    }

    fun ask(queue: ActionQueue) : Boolean {

        val count = queue.getMeasurementCount(SyncMeasurement::class.java)
        name.set("Sync${if (count > 0) count.toString() else ""}")

        if (showAndWait()) {

            val measurement = getMeasurement()
            val name        = name.get()
            val action      = queue.addMeasurement(name, measurement)
            val base        = Measure.baseFile

            action.resultsPath = "$base-%s-$name.csv"
            action.setAttribute("type", "synced")

            action.setBefore {
                (it.measurement as SyncMeasurement).loadInstruments(Configuration.getInstruments())
                Measure.display(it)
            }

            return true

        } else {
            return false
        }

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        sourceDrain.setFieldsDisabled(flag)
        sourceGate.setFieldsDisabled(flag)
        if (!flag) updateEnabled()
    }

    fun getMeasurement(): SyncMeasurement {

        val measurement = SyncMeasurement()

        measurement.configureVoltages(
            minSDV.get(),
            maxSDV.get(),
            numSDV.get(),
            symSGV.get()
        ).configureOffset(
            offset.get()
        ).configureTimes(
            intTime.get(),
            delTime.get()
        )

        return measurement

    }

    val isEnabled: Boolean
        get() = true


}