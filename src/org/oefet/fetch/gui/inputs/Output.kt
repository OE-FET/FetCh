package org.oefet.fetch.gui.inputs

import org.oefet.fetch.measurement.OutputMeasurement
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.Measure

object Output : Grid("Output Curve", 1) {

    val basic = Fields("Basic Parameters")
    val name = basic.addTextField("Measurement Name", "")

    init {
        basic.addSeparator()
    }

    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDV = sourceDrain.addDoubleField("Start [V]", 0.0)
    val maxSDV = sourceDrain.addDoubleField("Stop [V]", 60.0)
    val numSDV = sourceDrain.addIntegerField("No. Steps", 61)

    init {
        sourceDrain.addSeparator()
    }

    val symSDV = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val sourceGate = Fields("Source-Gate Parameters")
    val minSGV = sourceGate.addDoubleField("Start [V]", 0.0)
    val maxSGV = sourceGate.addDoubleField("Stop [V]", 60.0)
    val numSGV = sourceGate.addIntegerField("No. Steps", 7)


    init {

        addAll(
            basic, Grid(2,
                sourceDrain,
                sourceGate
            ))
        setGrowth(true, false)
        setIcon(Icon.RHEOSTAT)

        basic.linkConfig(Settings.outputBasic)
        sourceDrain.linkConfig(Settings.outputSD)
        sourceGate.linkConfig(Settings.outputSG)

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        sourceDrain.setFieldsDisabled(flag)
        sourceGate.setFieldsDisabled(flag)
    }

    fun ask(queue: ActionQueue): Boolean {

        val count = queue.getMeasurementCount(OutputMeasurement::class.java)
        name.set("Output${if (count > 0) count.toString() else ""}")

        if (showAsConfirmation()) {

            val measurement = getMeasurement()
            val name        = name.get()
            val action      = queue.addMeasurement(name, measurement)
            val base        = Measure.baseFile

            action.resultsPath = "$base-%s-$name.csv"
            action.setAttribute("Type", "Output")

            action.setBefore {
                (it.measurement as OutputMeasurement).loadInstruments(Configuration.getInstruments())
                Measure.display(it)
            }

            return true

        } else {
            return false
        }

    }

    fun getMeasurement(): OutputMeasurement {

        val measurement = OutputMeasurement()

        measurement.configureSD(
            minSDV.get(),
            maxSDV.get(),
            numSDV.get(),
            symSDV.get()
        ).configureSG(
            minSGV.get(),
            maxSGV.get(),
            numSGV.get(),
            false
        ).configureTimes(
            intTime.get(),
            delTime.get()
        )

        return measurement

    }

}