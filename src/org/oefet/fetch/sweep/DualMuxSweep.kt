package org.oefet.fetch.sweep

import jisa.devices.mux.Multiplexer
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.CheckGrid
import jisa.gui.Form
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYPoint

class DualMuxSweep : FetChSweep<XYPoint>("Dual Multiplexer Sweep", "XY", Type.INDEX, Icon.DEVICE.blackImage) {

    val A by requiredInstrument("Multiplexer Channel A", Multiplexer::class)
    val B by requiredInstrument("Multiplexer Channel B", Multiplexer::class)

    // Custom input fields
    val counts    = Form("Counts")
    val nA        = counts.addIntegerField("No. Routes A", 24)
    val nB        = counts.addIntegerField("No. Routes B", 24)
    val checkGrid = CheckGrid("Active Devices", nA.value, nB.value)

    init {
        nA.addChangeListener { _ -> checkGrid.setSize(nA.value, nB.value) }
        nB.addChangeListener { _ -> checkGrid.setSize(nA.value, nB.value) }
    }

    val countA  by customInput(counts, nA)
    val countB  by customInput(counts, nB)
    val checked by customInput(checkGrid)

    override fun getValues(): List<XYPoint> {

        val list = mutableListOf<XYPoint>()

        for (i in 0 until countA) {

            for (j in 0 until countB) {

                if (checked[i][j]) {
                    list += XYPoint(i.toDouble(), j.toDouble())
                }

            }

        }

        return list.toList()

    }

    override fun formatValue(value: XYPoint): String {
        return "%d, %d".format(value.x.toInt(), value.y.toInt())
    }

    override fun generateForValue(value: XYPoint, actions: List<Action<*>>): List<Action<*>> {

        val switch = SimpleAction("Change MUX Route to (%d, %d)".format(value.x.toInt(), value.y.toInt())) {
            A.route = value.x.toInt()
            B.route = value.y.toInt()
        }

        return listOf(switch) + actions

    }

}