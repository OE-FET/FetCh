package org.oefet.fetch.gui.inputs

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Configuration
import org.oefet.fetch.gui.tabs.FileLoad
import org.oefet.fetch.gui.tabs.Measure
import org.oefet.fetch.measurement.FPPMeasurement

object FPP : Grid("FPP Conductivity", 1) {

    val basic = Fields("Basic Parameters")
    val name = basic.addTextField("Measurement Name", "")

    init {
        basic.addSeparator()
    }

    val intTime = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDI = sourceDrain.addDoubleField("Start [A]", 0.0)
    val maxSDI = sourceDrain.addDoubleField("Stop [A]", 10e-6)
    val numSDI = sourceDrain.addIntegerField("No. Steps", 11)

    init { sourceDrain.addSeparator() }

    val symSDI = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val sourceGate = Fields("Source-Gate Parameters")
    val hold       = sourceGate.addCheckBox("Use Gate", false)
    val gate       = sourceGate.addDoubleField("Gate Voltage [V]", 0.0)

    init {
        setIcon(Icon.VOLTMETER)
        addAll(basic, Grid(2, sourceDrain, sourceGate))
        basic.linkConfig(Settings.fppBasic)
        sourceDrain.linkConfig(Settings.fppSD)
        sourceGate.linkConfig(Settings.fppSG)

        gate.isDisabled = !hold.get()
        hold.setOnChange { gate.isDisabled = !hold.get() }

    }

    fun ask(queue: ActionQueue) {

        val count = queue.getMeasurementCount(FPPMeasurement::class.java)
        name.set("FPPCond${if (count > 0) count.toString() else ""}")

        if (showAsConfirmation()) {

            val measurement = FPPMeasurement()
                .configureCurrent(minSDI.get(), maxSDI.get(), numSDI.get(), symSDI.get())
                .configureGate(hold.get(), gate.get())
                .configureTiming(intTime.get(), delTime.get())

            val name        = name.get()
            val action      = queue.addMeasurement(name, measurement)
            val base        = Measure.baseFile

            action.resultsPath = "$base-%s-$name.csv"
            action.setAttribute("Type", "FPP Conductivity")

            action.setBefore {
                (it.measurement as FPPMeasurement).loadInstruments(Configuration.getInstruments())
                Measure.display(it)
            }

        }

    }

}