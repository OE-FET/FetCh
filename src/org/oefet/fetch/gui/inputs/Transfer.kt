package org.oefet.fetch.gui.inputs

import org.oefet.fetch.measurement.TransferMeasurement
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.FileLoad
import org.oefet.fetch.gui.tabs.Measure
import java.lang.Exception

object Transfer : Grid("Transfer Curve", 1) {

    val basic = Fields("Basic Parameters")
    val name = basic.addTextField("Measurement Name", "")

    init {
        basic.addSeparator()
    }

    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDV = sourceDrain.addDoubleField("Start [V]", -5.0)
    val maxSDV = sourceDrain.addDoubleField("Stop [V]", 50.0)
    val numSDV = sourceDrain.addIntegerField("No. Steps", 2)

    val sourceGate = Fields("Source-Gate Parameters")
    val minSGV = sourceGate.addDoubleField("Start [V]", -10.0)
    val maxSGV = sourceGate.addDoubleField("Stop [V]", 50.0)
    val numSGV = sourceGate.addIntegerField("No. Steps", 61)

    init {
        sourceGate.addSeparator()
    }

    val symSGV = sourceGate.addCheckBox("Sweep Both Ways", true)

    init {

        addAll(
            basic, Grid(2,
                sourceDrain,
                sourceGate
            ))
        setGrowth(true, false)
        setIcon(Icon.TRANSISTOR)

        basic.linkConfig(Settings.transferBasic)
        sourceDrain.linkConfig(Settings.transferSD)
        sourceGate.linkConfig(Settings.transferSG)

    }

    fun updateEnabled() {

        val disabled = false

        intTime.isDisabled = disabled
        delTime.isDisabled = disabled

        sourceDrain.setFieldsDisabled(disabled)
        sourceGate.setFieldsDisabled(disabled)

    }

    fun ask(queue: ActionQueue) : Boolean {

        val count = queue.getMeasurementCount(TransferMeasurement::class.java)
        name.set("Transfer${if (count > 0) count.toString() else ""}")

        if (showAsConfirmation()) {

            val measurement = getMeasurement()
            val name        = name.get()
            val action      = queue.addMeasurement(name, measurement)
            val base        = Measure.baseFile

            action.resultsPath = "$base-%s-$name.csv"
            action.setAttribute("Type", "Transfer")

            action.setBefore {
                (it.measurement as TransferMeasurement).loadInstruments(Configuration.getInstruments())
                Measure.display(it)
            }

            action.setAfter { try { FileLoad.addData(it.data) } catch (e: Exception) {e.printStackTrace()} }

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

    fun getMeasurement(): TransferMeasurement {

        val measurement = TransferMeasurement()

        measurement.configureSD(
            minSDV.get(),
            maxSDV.get(),
            numSDV.get(),
            false
        ).configureSG(
            minSGV.get(),
            maxSGV.get(),
            numSGV.get(),
            symSGV.get()
        ).configureTimes(
            intTime.get(),
            delTime.get()
        )

        return measurement

    }

    val isEnabled: Boolean
        get() = true


}