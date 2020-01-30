package fetter.gui

import fetter.measurement.Instruments
import jisa.enums.Icon
import jisa.gui.Configurator
import jisa.gui.Grid
import jisa.gui.Section

/**
 * Instrument configuration tab
 */
object Configuration : Grid("Configuration", 1) {

    val ground      = Configurator.SMU("Ground Channel (for SPA)", "gndu", Settings, Connections)
    val sourceDrain = Configurator.SMU("Source-Drain Channel", "sdSMU", Settings, Connections)
    val sourceGate  = Configurator.SMU("Source-Gate Channel", "sgSMU", Settings, Connections)
    val fourPP1     = Configurator.VMeter("Four Point Probe Channel 1", "fpp1", Settings, Connections)
    val fourPP2     = Configurator.VMeter("Four Point Probe Channel 2", "fpp2", Settings, Connections)
    val tControl    = Configurator.TC("Temperature Control", "tc", Settings, Connections)
    val tMeter      = Configurator.TMeter("Temperature Sensor", "tm", Settings, Connections)

    init {

        setGrowth(true, false)
        setIcon(Icon.COGS)

        addAll(
            Section("SPA Grounding", Grid(1, ground)).apply { isExpanded = false },
            Section("SMU Channels", Grid(2, sourceDrain, sourceGate)).apply { isExpanded = false },
            Section("Voltage Probes", Grid(2, fourPP1, fourPP2)).apply { isExpanded = false },
            Section("Temperature", Grid(2, tControl, tMeter)).apply { isExpanded = false }
        )
    }

    fun getInstruments(): Instruments {

        return Instruments(
            sourceDrain.get(),
            sourceGate.get(),
            ground.get(),
            fourPP1.get(),
            fourPP2.get(),
            tControl.get(),
            tMeter.get()
        )

    }

}