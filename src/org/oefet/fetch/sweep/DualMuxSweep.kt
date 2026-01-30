package org.oefet.fetch.sweep

import jisa.devices.mux.Multiplexer
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.maths.Range
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYPoint

class DualMuxSweep : FetChSweep<XYPoint>("Dual Multiplexer Sweep", "XY", Type.INDEX, Icon.DEVICE.blackImage) {

    val A by requiredInstrument("Multiplexer Channel A", Multiplexer::class)
    val B by requiredInstrument("Multiplexer Channel B", Multiplexer::class)

    val type    by userChoice("Basic", "Sweep Type", "Combinatorial", "Pairs")
    val routesA by userInput("MUX Channel A", "Routes", Range.linear(0, 23))
    val routesB by userInput("MUX Channel B", "Routes", Range.linear(0, 23))

    override fun getValues(): List<XYPoint> {

        val list = mutableListOf<XYPoint>()

        if (type == 0) {

            for (a in routesA) {
                for (b in routesB) {
                    list.add(XYPoint(a, b))
                }
            }

        } else {

            list += routesA.zip(routesB) { a, b -> XYPoint(a, b) }

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