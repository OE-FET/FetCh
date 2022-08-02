package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.gui.Field
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Actions
import org.oefet.fetch.Measurements
import org.oefet.fetch.Settings
import org.oefet.fetch.Sweeps

object ShownActions : Grid("Actions", 3) {

    val measurements = Fields("Hidden Measurements")
    val actions      = Fields("Hidden Actions")
    val sweeps       = Fields("Hidden Sweeps")


    init {

        addAll(actions, measurements, sweeps)
        setGrowth(true, false)
        setIcon(Icon.COGS)

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