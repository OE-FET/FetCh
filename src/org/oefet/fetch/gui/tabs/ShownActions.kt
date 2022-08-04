package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.gui.Field
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Actions
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.Sweeps

object ShownActions : Grid("Actions", 1) {

    val config       = Fields("Configuration")
    val measurements = Fields("Enabled Measurements")
    val actions      = Fields("Enabled Actions")
    val sweeps       = Fields("Enabled Sweeps")


    init {

        val type = config.addChoice("Display Type", Settings.actionDisplay.intValue("type").getOrDefault(0), "Dropdown Menu", "List")

        type.setOnChange {
            Settings.actionDisplay.intValue("type").set(type.value)
            Measure.queueList.updateTypes()
            Measure.bigQueue.updateTypes()
        }


        val row  = Grid(3,actions, measurements, sweeps)

        setGrowth(true, false)
        setIcon(Icon.COGS)

        addAll(config, row)

        for (type in Measurements.types) {

            val check = measurements.addCheckBox(type.name, !Settings.hidden.booleanValue(type.name).getOrDefault(false))

            check.setOnChange {
                Settings.hidden.booleanValue(check.text).set(!check.value)
                Measure.queueList.updateTypes()
                Measure.bigQueue.updateTypes()
            }

        }

        for (type in Actions.types) {

            val check = actions.addCheckBox(type.name, !Settings.hidden.booleanValue(type.name).getOrDefault(false))

            check.setOnChange {
                Settings.hidden.booleanValue(check.text).set(!check.value)
                Measure.queueList.updateTypes()
                Measure.bigQueue.updateTypes()
            }

        }

        for (type in Sweeps.types) {

            val check = sweeps.addCheckBox(type.name, !Settings.hidden.booleanValue(type.name).getOrDefault(false))

            check.setOnChange {
                Settings.hidden.booleanValue(check.text).set(!check.value)
                Measure.queueList.updateTypes()
                Measure.bigQueue.updateTypes()
            }

        }

        addToolbarButton("Enable All") {
            sweeps.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(true) }
            measurements.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(true) }
            actions.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(true) }
        }

        addToolbarButton("Disable All") {
            sweeps.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(false) }
            measurements.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(false) }
            actions.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(false) }
        }

        addToolbarButton("Toggle All") {
            sweeps.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(!it.value) }
            measurements.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(!it.value) }
            actions.filter { it.value is Boolean }.forEach { (it as Field<Boolean>).set(!it.value) }
        }

    }

}