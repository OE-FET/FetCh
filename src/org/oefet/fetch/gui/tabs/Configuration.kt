package org.oefet.fetch.gui.tabs

import org.oefet.fetch.measurement.Instruments
import jisa.enums.Icon
import jisa.gui.Configurator
import jisa.gui.Grid
import jisa.gui.Section
import org.oefet.fetch.Settings

/**
 * Instrument configuration tab
 */
object Configuration : Grid("Configuration", 1) {

    val ground      = Configurator.SMU("Ground Channel (for SPA)",
        Settings.groundConfig,
        Connections
    )
    val sourceDrain = Configurator.SMU("Source-Drain Channel",
        Settings.sourceDrainConfig,
        Connections
    )
    val sourceGate  = Configurator.SMU("Source-Gate Channel",
        Settings.sourceGateConfig,
        Connections
    )
    val fourPP1     = Configurator.VMeter("Four Point Probe Channel 1",
        Settings.fourPP1Config,
        Connections
    )
    val fourPP2     = Configurator.VMeter("Four Point Probe Channel 2",
        Settings.fourPP2Config,
        Connections
    )
    val tControl    = Configurator.TC("Temperature Control",
        Settings.tControlConfig,
        Connections
    )
    val tMeter      = Configurator.TMeter("Temperature Sensor",
        Settings.tMeterConfig,
        Connections
    )

    init {

        setGrowth(true, false)
        setIcon(Icon.COGS)

        addAll(
            Section("SPA Grounding", Grid(1, ground)).apply { isExpanded = false },
            Section("SMU Channels", Grid(2,
                sourceDrain,
                sourceGate
            )).apply { isExpanded = false },
            Section("Voltage Probes", Grid(2,
                fourPP1,
                fourPP2
            )).apply { isExpanded = false },
            Section("Temperature", Grid(2,
                tControl,
                tMeter
            )).apply { isExpanded = false }
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