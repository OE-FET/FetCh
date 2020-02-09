package org.oefet.fetch.gui

import org.oefet.fetch.measurement.OutputMeasurement
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.gui.Fields
import jisa.gui.Grid

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

        addAll(basic, Grid(2, sourceDrain, sourceGate))
        setGrowth(true, false)
        setIcon(Icon.RHEOSTAT)

        basic.loadFromConfig("op-basic", Settings)
        sourceDrain.loadFromConfig("op-sd", Settings)
        sourceGate.loadFromConfig("op-sg", Settings)


    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        sourceDrain.setFieldsDisabled(flag)
        sourceGate.setFieldsDisabled(flag)
    }

    fun askForMeasurement(queue: ActionQueue): Boolean {

        name.set("Output${queue.getMeasurementCount(OutputMeasurement::class.java) + 1}")

        return if (showAndWait()) {
            val action = queue.addMeasurement(name.get(), getMeasurement())
            action.setResultsPath("${Measure.baseFile}-${name.get()}.csv")
            action.setBefore { Measure.showMeasurement(action); Results.addMeasurement(action); }
            true
        } else {
            false
        }

    }

    fun getMeasurement(): Measurement {

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