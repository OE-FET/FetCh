package org.oefet.fetch.action

import jisa.devices.relay.ADRelay
import jisa.enums.Icon
import jisa.gui.CheckGrid
import jisa.gui.Doc
import jisa.gui.Element
import jisa.gui.Form
import jisa.results.ResultTable

class MultiRelaySwitch : FetChAction("Multi Relay Switch", Icon.DEVICE.blackImage) {

    private val relay by requiredInstrument("Arduino Relay", ADRelay::class)

    private val setup = Form("Relay Setup")
    private val nChannels = setup.addIntegerField("No. Channels", 8)
    private val statesGrid = CheckGrid("Relay States (Checked = On)", nChannels.value, 1)

    private val channelCount by customInput(setup, nChannels)
    private val states       by customInput(statesGrid)

    init {
        statesGrid.setAll(false)

        nChannels.addChangeListener { _ ->
            val oldCount = statesGrid.columnCount
            statesGrid.setSize(nChannels.value, 1)

            for (channel in oldCount until statesGrid.columnCount) {
                statesGrid.setChecked(channel, 0, false)
            }
        }
    }

    private val element = Doc("Switch").apply {
        addText("Switching Relays...").setAlignment(Doc.Align.CENTRE)
    }

    override fun createDisplay(data: ResultTable): Element = element

    override fun checkForErrors(): List<String> {
        val errors = ArrayList<String>()

        if (channelCount < 1) {
            errors.add("No. Channels must be at least 1")
        }

        try {
            if (channelCount > relay.switches.size) {
                errors.add("No. Channels is $channelCount but the selected relay only has ${relay.switches.size}")
            }
        } catch (_: Exception) {
            // The required-instrument check reports missing relay configuration.
        }

        return errors
    }

    override fun run(results: ResultTable) {
        val switches = relay.switches

        for ((index, switch) in switches.take(channelCount).withIndex()) {
            switch.isOn = states.getOrNull(index)?.getOrNull(0) ?: false
        }
    }

    override fun getLabel(): String {
        return "Set Relay States"
    }

}
