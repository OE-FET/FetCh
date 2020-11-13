package org.oefet.fetch.gui.tabs

import jisa.devices.*
import jisa.enums.Icon
import jisa.gui.Configurator
import jisa.gui.Grid
import jisa.gui.Section
import org.oefet.fetch.Settings
import org.oefet.fetch.measurement.FControl
import org.oefet.fetch.measurement.Instruments

/**
 * Instrument configuration tab
 */
object Configuration : Grid("Configuration", 1) {

    private var loaded: Instruments? = null

    val ground      = Configurator.SMU("Ground Channel (for SPA)", Settings.groundConfig, Connections)
    val sourceDrain = Configurator.SMU("Source-Drain Channel", Settings.sourceDrainConfig, Connections)
    val sourceGate  = Configurator.SMU("Source-Gate Channel", Settings.sourceGateConfig, Connections)
    val heater      = Configurator.SMU("Heater", Settings.heaterConfig, Connections)
    val thermal     = Configurator.VMeter("Thermal Voltage Meter", Settings.tvMeterConfig, Connections)
    val fourPP1     = Configurator.VMeter("Four Point Probe Channel 1", Settings.fourPP1Config, Connections)
    val fourPP2     = Configurator.VMeter("Four Point Probe Channel 2", Settings.fourPP2Config, Connections)
    val tControl    = Configurator.TC("Temperature Control", Settings.tControlConfig, Connections)
    val tMeter      = Configurator.TMeter("Temperature Sensor", Settings.tMeterConfig, Connections)

    init {

        setGrowth(true, false)
        setIcon(Icon.COGS)

        // Add all configurators, grouped in their respective sections, all collapsed to start with
        addAll(
            Section("Standard SMU Channels", Grid(3, ground, sourceDrain, sourceGate)).apply { isExpanded = false },
            Section("Temperature", Grid(2, tControl, tMeter)).apply { isExpanded = false }
        )
    }

}